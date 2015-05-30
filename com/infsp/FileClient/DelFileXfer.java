package com.infsp.FileClient;

/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 7/28/11
 * Time: 6:51 PM
 */

import com.infsp.FileServer.FileServer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.*;

public class DelFileXfer implements Runnable {

    private final String filePath;
    private final String hostname;
    private final int    port;

    private Socket connection;

    static final Logger LOGGER = Logger.getLogger(DelFileXfer.class);

    public boolean status = false;
    public String statusMsg = "";

    public DelFileXfer(String hostname, String filePath){

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
                this.statusMsg = uhe.toString();
                LOGGER.error(this.statusMsg);
                return;
            } catch (SocketTimeoutException ste){
                this.statusMsg = ste.toString();
                LOGGER.error(this.statusMsg);
                return;
            } catch (ConnectException ce){
                this.statusMsg = ce.toString();
                LOGGER.error(this.statusMsg);
                return;
            } catch (IOException ioe){
                this.statusMsg = ioe.toString();
                LOGGER.error(this.statusMsg);
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

            if (response == null){
                this.statusMsg = "No response from "+hostname+" for del file "+filePath;
                LOGGER.error(this.statusMsg);
                return;
            }

            if(!response.startsWith("ok")){
                this.statusMsg = response;
                LOGGER.error(hostname+" rejected del for file " + filePath);
            } else {
                this.status = true;
                LOGGER.debug(hostname+" deleted file "+filePath);
            }

        } catch (IOException ioe){
            this.statusMsg = ioe.toString();
            LOGGER.error(this.statusMsg);
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

