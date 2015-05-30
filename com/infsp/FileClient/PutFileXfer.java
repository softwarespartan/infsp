package com.infsp.FileClient;

/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 7/28/11
 * Time: 4:10 PM
 */


import com.infsp.FileServer.FileServer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.io.*;
import java.net.*;

public class PutFileXfer implements Runnable {

    private final String filePath;
    private final String hostname;
    private final int    port;

    private Socket connection;

    public boolean status = false;
    public String statusMsg = "";

    static final Logger LOGGER = Logger.getLogger(PutFileXfer.class);

    public PutFileXfer(String hostname, String filePath) throws IOException{

        LOGGER.setLevel(Level.ERROR);

        this.filePath = filePath;
        this.hostname = hostname;
        this.port     = FileServer.port;
    }

    public void run(){

        // standard setup w/client socket
        try{

            File srcFile = new File(this.filePath);

            // make sure the file to put exists locally
            if(!srcFile.exists()){
                this.statusMsg = "file "+filePath+" does not exist";
                LOGGER.error(this.statusMsg);
                return;
            }

            // make sure we can read the local file
            if (!srcFile.canRead()){
                this.statusMsg = "do not have read permissions for file "+this.filePath;
                LOGGER.error(this.statusMsg);
                return;
            }

            // make sure we can actually open it
            FileInputStream fis;
            try{
                fis = new FileInputStream(filePath);
            } catch (FileNotFoundException fnfe){
                LOGGER.error(fnfe.toString());
                this.statusMsg = " could not open file "+filePath;
                LOGGER.error(this.statusMsg);
                return;
            }


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
            out.write("put\n");
            out.flush();

            // compute the file size
            double fileSize = new File(filePath).length();

            // send the file size
            out.write(""+fileSize+"\n");
            out.flush();

            // send the file path
            out.write(filePath+"\n");
            out.flush();

            // ok with server to stream the file?
            LOGGER.debug("Sent file credentials awaiting response ...");
            String response = in.readLine();

            if (response == null){
                this.statusMsg = "No response from "+this.hostname+" for file "+this.filePath;
                LOGGER.error(this.statusMsg);
                return;
            }

            if(!response.startsWith("ok")){
                this.statusMsg = "FileServer rejected put for file " + filePath;
                LOGGER.error(this.statusMsg);
                return;
            } else {
                LOGGER.debug("FileServer accepted file "+filePath);
            }


            // open the file as buffered byte input stream
            BufferedInputStream  src = new BufferedInputStream(fis);

            // get a buffered version of the connection output stream
            BufferedOutputStream dst = new BufferedOutputStream(connection.getOutputStream());

            // creat 32Kb buffer
            byte[] buffer = new byte[1024*32];
            int bytesRead;

            // record the start time for logging
            long startTime = System.currentTimeMillis();

            // loop over the streams and xfer
            while((bytesRead = src.read(buffer,0,buffer.length))>0)
                dst.write(buffer,0,bytesRead);

            // final flush of the file buffer (don't forget!)
            dst.flush();

            // close the file
            src.close();

            // record the stop time
            long  endTime = System.currentTimeMillis();

            // compute elapsed time
            float seconds = (endTime - startTime) / 1000F;

            double xferSpeed = (fileSize/seconds)/(1024.0*1024.0);

            this.statusMsg = String.format("transfer speed: %5.2f MB/sec",xferSpeed);
            LOGGER.info(this.statusMsg);

            // the xfer was successful
            this.status = true;

        } catch (IOException ioe){
            this.statusMsg = ioe.toString();
            LOGGER.error(this.statusMsg);
        } finally {
            if(this.connection != null &&
                    this.connection.isBound()
                        && !this.connection.isClosed()){
                try{
                    LOGGER.debug("Closing socket");
                    this.connection.close();
                }catch (IOException ioe){
                    ioe.printStackTrace();
                }
            }

            try{
                Thread.sleep(4);
            } catch (InterruptedException ie){
                // no op
            }

        }

    }
}

