package com.infsp.FileClient;

import ipworks.IPWorksException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 7/29/11
 * Time: 10:58 AM
 */

public class FileClient extends Thread{

    private final FileRequestExecutor fileRequestExecutor;

    public static final int port = 10102;

    private final ServerSocket serverSocket;
    private final ExecutorService pool;

    private final int socketAcceptTimeout = 2000;

    static final Logger LOGGER = Logger.getLogger(FileClient.class);

    public FileClient(int poolSize) throws IOException {

        LOGGER.setLevel(Level.ERROR);

        try{

            this.serverSocket = new ServerSocket(FileClient.port);
            this.serverSocket.setSoTimeout(this.socketAcceptTimeout);
        }catch (IOException ioe){
            LOGGER.error("could not start FileClient on port "+ FileClient.port);
            throw ioe;
        }

        // init the thread pool
        pool = Executors.newFixedThreadPool(poolSize);

        this.fileRequestExecutor = new FileRequestExecutor(4);
    }

    public void run() { // run the service

        // let log know we've started
        LOGGER.debug("FileClient entering run loop");

        try {

            // verify pool status
            //LOGGER.debug("pool status: "+pool.isShutdown());

            // loop until the execution pool is closed
            while(!pool.isShutdown()){

                try{

                    // and here we wait for socketAcceptTimeout seconds
                    //LOGGER.debug("waiting for connections ...");

                    // listen for incoming connections
                    final Socket connection = this.serverSocket.accept();

                    String clientAddr = connection.getInetAddress().getHostAddress();

                    // let the log know we've heard the call
                    LOGGER.debug("connection established with "+clientAddr+", parsing xml request");

                    // execute the request via handler
                    pool.execute(new Handler(connection, this.fileRequestExecutor));

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

                    LOGGER.debug("shutting down FileClient socket");

                    this.serverSocket.close();
                }
            }catch (IOException ioe){

            }
        }

        LOGGER.debug("FileClient exiting run loop");
    }

    public void willShutdown(){
        this.pool.shutdown();
        this.fileRequestExecutor.kill();
    }

    public void shutdown(){
    }

    class Handler implements Runnable {

        private final Socket connection;

        private final FileRequestExecutor executor;

        Handler(Socket socket, FileRequestExecutor executor) {
            this.connection = socket;
            this.executor = executor;
        }

        public void run() {

            try{

                // get the connection output stream
                Writer out = new BufferedWriter(
                        new OutputStreamWriter(
                                connection.getOutputStream() ) );


                // fully initialize the network input from new socket
                BufferedReader networkIn = new BufferedReader(
                        new InputStreamReader(
                                connection.getInputStream() ) );

                // create new string buffer for the xml data
                StringBuffer xmlData = new StringBuffer();

                // integer holds a byte's worth of data
                int c;

                // read the input stream until nothing left (i.e returns -1)
                // read the data one byte at a time ...
                while((c = networkIn.read()) > 0){
                    // append any characters (bytes) to the string buffer
                    xmlData.append((char) c);
                }

                LOGGER.error("Finished reading");

                String stats = "Error parsing xml data!";
                try {
                    FileRequestXmlParser parser
                            = new FileRequestXmlParser(this.executor);
                    stats = parser.parse(xmlData.toString().trim());
                    LOGGER.error(stats);
                } catch (IPWorksException e) {
                    LOGGER.error(e.toString());
                    return;
                }

                out.write(stats+"\n");
                out.flush();

                LOGGER.error("That's all folks ...");

            }catch (IOException ioe){
                LOGGER.error("error parsing xml request "+ioe.toString());
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
}
