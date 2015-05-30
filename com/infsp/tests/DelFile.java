package com.infsp.tests;

import com.infsp.FileServer.FileServer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.*;

/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 7/19/11
 * Time: 10:31 AM
 * To change this template use File | Settings | File Templates.
 */
public class DelFile implements Runnable {

    private final String filePath;
    private final String hostname;
    private final int    port;

    private Socket connection;

    static final Logger LOGGER = Logger.getLogger(DelFile.class);

    public DelFile(String hostname, String filePath){

        LOGGER.setLevel(Level.ERROR);

        this.filePath = filePath;
        this.hostname = hostname;
        this.port     = FileServer.port;
    }

    public void run(){

        // standard setup w/client socket
        try{

            try{
                LOGGER.debug("attempt socket on port "+port+" to host "+hostname);
                this.connection = new Socket();
                InetAddress iaddr = InetAddress.getByName(this.hostname);
                SocketAddress saddr = new InetSocketAddress(iaddr,this.port);
                this.connection.connect(saddr,10000);
                this.connection.setSoTimeout(4000);

                LOGGER.debug("got connection ");
            } catch(UnknownHostException uhe){
                LOGGER.error(uhe.toString());
                return;
            } catch (SocketTimeoutException ste){
                LOGGER.error(ste.toString());
                return;
            } catch (ConnectException ce){
                LOGGER.error(ce.toString());
                return;
            } catch (IOException ioe){
                ioe.printStackTrace();
                return;
            }

            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(
                                          connection.getInputStream()));

            BufferedWriter out = new BufferedWriter(
                                    new OutputStreamWriter(
                                          connection.getOutputStream()));


            // tell the server we want to put a file
            out.write("del\n"); out.flush();


            // send the file path
            out.write(filePath+"\n");
            out.flush();

            // ok with server to stream the file?
            LOGGER.debug("Sent file to delete,  awaiting response ...");
            String response = in.readLine();

            if(!response.startsWith("ok")){
                LOGGER.error("FileServer rejected del for file " + filePath);
                return;
            } else {
                LOGGER.debug("FileServer deleted file "+filePath);
            }


        } catch (IOException ioe){
            LOGGER.error(ioe.toString());
        } finally {
            if(this.connection.isBound() && !this.connection.isClosed()){
                try{
                    LOGGER.debug("Closing socket");
                    this.connection.close();
                }catch (IOException ioe){
                    ioe.printStackTrace();
                }
            }

            try{
                Thread.sleep(40);
            } catch (InterruptedException ie){
                // no op
            }
        }

    }
}
