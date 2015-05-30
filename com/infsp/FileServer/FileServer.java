package com.infsp.FileServer;

import com.infsp.vfs.VirtualFileSystem;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.net.ServerSocket;
import java.net.Socket;

import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import java.io.*;
import java.util.concurrent.Semaphore;

/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 7/18/11
 * Time: 4:07 PM
 */
public class FileServer extends Thread{

    private final ServerSocket serverSocket;
    private final ExecutorService pool;
    private final VirtualFileSystem vfs;

    static final Logger LOGGER = Logger.getLogger(FileServer.class);

    private final int socketAcceptTimeout = 2000;

    public static final int port = 10101;

    public FileServer(int poolSize, VirtualFileSystem fileSystem) throws IOException{

        LOGGER.setLevel(Level.ERROR);

        try{

            // create new server listening socket on port
            this.serverSocket = new ServerSocket(this.port);

            // set the time out to some number so that do not
            // sit and wait forever on accept if the server
            // has been shut down.  Otherwise server will get
            // shutdown message but hang in socket.accept() forever
            this.serverSocket.setSoTimeout(this.socketAcceptTimeout);

            // let the log know were done
            LOGGER.debug("FileServer opened on port "+ port);

        }catch (IOException ioe){

            // let the log know we've got problems
            LOGGER.error("Failed to start file server: "+ioe.toString());

            // don't swallow this!
            throw ioe;
        }

        // init rest of the object
        this.pool         = Executors.newFixedThreadPool(poolSize);
        this.vfs          = fileSystem;
    }

    public void run(){

        // let log know we've started
        LOGGER.debug("FileServer entering run loop");

        try {

            // verify pool status
            LOGGER.debug("pool status: "+pool.isShutdown());

            // loop until the execution pool is closed
            while(!pool.isShutdown()){

                try{

                    // and here we wait for socketAcceptTimeout seconds
                    //LOGGER.debug("waiting for connections ...");

                    // listen for incoming connections
                    final Socket connection = this.serverSocket.accept();

                    String clientAddr = connection.getInetAddress().getHostAddress();

                    // let the log know we've heard the call
                    LOGGER.debug("connection established with "+clientAddr+", executing request");

                    // execute the request via handler
                    pool.execute(new Handler(connection, vfs));

                } catch (SocketTimeoutException ste){
                    // healthy timeout here.
                    // a chance to recheck if the pool is open
                }
            }
        } catch (IOException ex){
            pool.shutdown();
        } catch (RejectedExecutionException ree){
            LOGGER.error("rejecting request execution");
        } finally{
            LOGGER.debug("shutting down pool");
            pool.shutdown();
            try{
                if(this.serverSocket != null){
                    LOGGER.debug("shutting down server socket");
                    this.serverSocket.close();
                }
            }catch (IOException ioe){

            }
        }

        LOGGER.debug("FileServer exiting run loop");
    }

    public void willShutdown(){pool.shutdown();}
    public void shutdown(){}
}

class Handler implements Runnable {

    private final Socket connection;
    private final VirtualFileSystem vfs;

    static final Semaphore mkdirsLock = new Semaphore(1);

    static final Logger LOGGER = Logger.getLogger(Handler.class);

    Handler(Socket socket,VirtualFileSystem vfs) {
        LOGGER.setLevel(Level.ERROR);
        this.connection = socket;
        this.vfs = vfs;
    }

    private void fail(BufferedWriter out,String msg) throws IOException{
        LOGGER.error(msg);
        out.write("fail: "+msg+'\n');
        out.flush();
        if (this.connection != null) this.connection.close();
    }

    public void run() {

        LOGGER.info("servicing new file request");

        BufferedReader in  = null;
        BufferedWriter out = null;

        try{
            // where we say stuff to the client
            out = new BufferedWriter(
                    new OutputStreamWriter(
                            connection.getOutputStream()));

            // where the client says stuff to us
            in  = new BufferedReader(
                    new InputStreamReader(
                            connection.getInputStream()));

            // get the mode from the client
            String mode = in.readLine();

            if (mode.equalsIgnoreCase("put")){

                // get the file size
                Double fileSize = Double.parseDouble(in.readLine());

                // get the file path
                String filePath = in.readLine();

                // get a disk to write to from vfs
                String disk = this.vfs.diskForFileOfSize(fileSize);
                if (disk.equalsIgnoreCase("")){
                    fail(out,"no disk for file "+filePath);
                    return;
                }

                // verify that we have enough free space on the
                // file system to save the file
                long freeSpace = new File(disk).getFreeSpace();
                if (fileSize > freeSpace){
                    fail(out,"not enough free space on disk for file "+filePath);
                    return;
                }

                // check that file does not already exist
                if(this.vfs.exists(filePath)){
                    this.fail(out,"file "+filePath+" already exists");
                    return;
                }

                // construct the full path as it would be on virtural disk
                File fullFile = new File(disk,filePath);

                try{
                    Handler.mkdirsLock.acquire();
                    // make sure that parent directories exist
                    // if not try to make it, fail if not
                    File parentFile = new File(fullFile.getPath()).getParentFile();
                    if(!parentFile.exists()){
                        LOGGER.debug("Parent file: "+parentFile.getPath()+" does not exist.  will make it");
                        if (!parentFile.mkdirs()){
                            fail(out,"could not make parent dir: "+parentFile.getPath());
                            return;
                        }
                    }

                }catch (InterruptedException ie){
                    // ignore
                } finally {
                    Handler.mkdirsLock.release();
                }

                // finally, let the client know it's ok to start streaming
                out.write("ok\n"); out.flush();

                // let all components know xfer will start
                vfs.notifyAllXferDidStart(filePath);

                // open file of same name on disk
                FileOutputStream dst = new FileOutputStream(fullFile);

                // open buffered streams for efficient transfer
                BufferedOutputStream bdst = new BufferedOutputStream(dst);
                BufferedInputStream  src  = new BufferedInputStream(connection.getInputStream());

                // init sufficiently large buffer for file
                byte[] buffer = new byte[1024*64];
                int bytesRead;

                // fill buffer, transmit, repeat
                while ((bytesRead = src.read(buffer,0,buffer.length)) > 0)
                    bdst.write(buffer,0,bytesRead);

                // transmit any remaining bytes in the buffer
                bdst.flush();

                // close the file stream
                bdst.close();

                // let everyone know xfer finished
                vfs.notifyAllXferDidFinish(filePath);

                // let all the other components know
                vfs.notifyAllPutFile(disk,filePath,fileSize);

            } else if (mode.equalsIgnoreCase("get")){

                // get the file path
                String filePath = in.readLine();

                // resolve the full path of the file
                String fullFilePath;
                if(vfs.exists(filePath)){

                    // vfs has journal entry so resolve it
                    fullFilePath = vfs.resolve(filePath);

                    // if anything (i.e. exception) happends in
                    // the mean time resolve will return empty string
                    if (fullFilePath.equalsIgnoreCase("")){
                        fail(out,"could not resolve disk for file"+filePath);
                        return;
                    }
                } else {
                    fail(out,"file "+filePath+" does not exist in journal");
                    return;
                }

                out.write("ok\n"); out.flush();

                vfs.notifyAllXferDidStart(filePath);

                //open the file for reading
                BufferedInputStream src = new BufferedInputStream(
                                                new FileInputStream(new File(fullFilePath)));

                // wrap the sockout output stream in buffer
                BufferedOutputStream dst = new BufferedOutputStream(connection.getOutputStream());

                byte[] buffer = new byte[1024*64];
                int bytesRead;

                // fill'er up
                while ((bytesRead = src.read(buffer,0,buffer.length)) > 0){
                    dst.write(buffer,0,bytesRead);
                }

                // flush the rest of the file to the client
                dst.flush();

                // close the file
                src.close();

                // let everyone know the xfer finished
                vfs.notifyAllXferDidFinish(filePath);

                // let everyone know the file was "getted"
                vfs.notifyAllGetFile(filePath);

            } else if (mode.equalsIgnoreCase("del")){

                // get the file path
                String filePath = in.readLine();

                // resolve the full path of the file
                String fullFilePath;
                if(vfs.exists(filePath)){

                    // vfs has journal entry so resolve it
                    fullFilePath = vfs.resolve(filePath);

                    // if anything (i.e. exception) happends in
                    // the mean time resolve will return empty string
                    if (fullFilePath.equalsIgnoreCase("")){
                        fail(out,"could not resolve disk for file"+filePath);
                        return;
                    }
                } else {
                    fail(out,"file "+filePath+" does not exist in journal");
                    return;
                }

                //delete the file on the local disk
                File fullFile = new File(fullFilePath);

                // check if exists on local disk
                if (!fullFile.exists()){
                    fail(out,"file "+filePath+" does not exist on local disk");
                    return;
                }

                // let client know we're going to delete
                out.write("ok\n"); out.flush();


                // do it
                fullFile.delete();

                // let all listeners know the file has been deleted
                vfs.notifyAllDelFile(filePath);

            }

        }catch (IOException ioe){
           ioe.printStackTrace();
           LOGGER.error("Fatal error "+ioe.toString());

        } finally {
            // klose the file
            try{
                if (this.connection != null
                        && this.connection.isBound()
                            && !this.connection.isClosed()){
                    this.connection.close();
                }
            }catch (IOException ioe){
                LOGGER.error("Error closing connection: "+ioe.toString());
            }
        }
    }
}

//
//The following method shuts down an ExecutorService in two phases, first by calling shutdown to reject incoming tasks, and then calling shutdownNow, if necessary, to cancel any lingering tasks:
// void shutdownAndAwaitTermination(ExecutorService pool) {
//   pool.shutdown(); // Disable new tasks from being submitted
//   try {
//     // Wait a while for existing tasks to terminate
//     if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
//       pool.shutdownNow(); // Cancel currently executing tasks
//       // Wait a while for tasks to respond to being cancelled
//       if (!pool.awaitTermination(60, TimeUnit.SECONDS))
//           System.err.println("Pool did not terminate");
//     }
//   } catch (InterruptedException ie) {
//     // (Re-)Cancel if current thread also interrupted
//     pool.shutdownNow();
//     // Preserve interrupt status
//     Thread.currentThread().interrupt();
//   }
// }