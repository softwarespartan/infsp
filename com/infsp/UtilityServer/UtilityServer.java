package com.infsp.UtilityServer;

import com.infsp.vfs.VirtualFileSystem;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.net.*;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 8/4/11
 * Time: 4:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class UtilityServer extends Thread{

    private final ServerSocket serverSocket;

    private final ExecutorService pool;

    private final VirtualFileSystem vfs;

    static final Logger LOGGER = Logger.getLogger(UtilityServer.class);

    private final int socketAcceptTimeout = 2000;

    public static final int port = 10103;

    public UtilityServer(int poolSize, VirtualFileSystem fileSystem) throws IOException {

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
            LOGGER.debug("UtilityServer opened on port "+ port);

        }catch (IOException ioe){

            // let the log know we've got problems
            LOGGER.error("Failed to start utility server: "+ioe.toString());

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
            LOGGER.debug("pool isShutdown: "+pool.isShutdown());

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
                    LOGGER.debug("shutting down utility server socket");
                    this.serverSocket.close();
                }
            }catch (IOException ioe){

            }
        }

        LOGGER.debug("UtilityServer exiting run loop");
    }

    public void willShutdown(){pool.shutdown();}
    public void shutdown(){}
}

class Handler implements Runnable {

    private final Socket connection;
    private final VirtualFileSystem vfs;

    //static final Semaphore mkdirsLock = new Semaphore(1);

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

        LOGGER.debug("servicing new file request");

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

            LOGGER.debug("mode: "+mode);

            if (mode.equalsIgnoreCase("nodestats")){

                double gb = 1024.0*1024.0*1024.0;

                double freeSpace  = vfs.getFreeSpace()/gb;
                double totalSpace = vfs.getTotalSpace()/gb;
                int numFiles   = vfs.getNumberOfFiles();

                String stats = String.format("%8.2f %8.2f %10d",freeSpace,
                                                                totalSpace,
                                                                numFiles);
                LOGGER.debug(stats);

                out.write(stats+"\n");
                out.flush();
                return;

            } else if (mode.equalsIgnoreCase("resolve")){

                // get the file to resolve
                String filePath = in.readLine();

                boolean fileExists = vfs.exists(filePath);
                String absFilePath = vfs.resolve(filePath);

                // if the abs file path is empty then
                // vfs could not resolve the file path
                boolean couldResolveFilePath = false;
                if (!absFilePath.equalsIgnoreCase("")){
                    couldResolveFilePath = true;
                }

                // no problems all good
                if (couldResolveFilePath){
                    out.write(absFilePath+"\n");
                    out.flush();
                    return;
                }

                // vfs has no such file
                if(!fileExists){
                    out.write("no such file in local journal"+"\n");
                    out.flush();
                    return;
                }

                // vfs has journal entry but no actual file
                if (fileExists && !couldResolveFilePath){
                    out.write("file exists in local journal but no such file on disk"+"\n");
                    out.flush();
                    return;
                }
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
