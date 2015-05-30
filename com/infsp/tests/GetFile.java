package com.infsp.tests;

import com.infsp.FileServer.FileServer;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.io.*;
import java.net.*;

/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 7/19/11
 * Time: 10:31 AM
 * To change this template use File | Settings | File Templates.
 */
public class GetFile implements Runnable {

    private final String filePath;
    private final String hostname;
    private final int    port;
    private  String dstFilePath;

    private Socket connection;

    static final Logger LOGGER = Logger.getLogger(GetFile.class);

    public GetFile(String hostname, String filePath,String dstFilePath) throws IOException{

        LOGGER.setLevel(Level.ERROR);

        this.filePath = filePath;
        this.hostname = hostname;
        this.port     = FileServer.port;

        //this.dstFilePath = dstFilePath;

        // first check if the dstFilePath is a file
        File dstFile = new File(dstFilePath).getAbsoluteFile();
        File srcFile = new File(filePath);

        this.dstFilePath = dstFile.getPath();

        if (dstFile.isDirectory()){

            // make full file path for destination file
            this.dstFilePath = new File(dstFile,srcFile.getName()).getPath();
        }

    }

    public void run(){

        // standard setup w/client socket
        FileOutputStream fos = null;

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


            // tell the server we want to fet a file
            out.write("get\n"); out.flush();

            // send the file path
            out.write(filePath+"\n"); out.flush();

            // ok with server to stream the file?
            LOGGER.debug("Sent file name awaiting response ...");
            String response = in.readLine();

            if(!response.startsWith("ok")){
                LOGGER.error("FileServer rejected get for file " + filePath);
                return;
            } else {
                LOGGER.debug("FileServer accepted file "+filePath);
            }

            // lets make sure we can create dstFilePath
            // before we ask the server to transfer the file

            try{
                fos = new FileOutputStream(new File(this.dstFilePath));
            } catch (FileNotFoundException fnfe){
                LOGGER.error(fnfe.toString());
                LOGGER.error("could not open file "+this.dstFilePath);
                return;
            }

            // open the file as buffered byte input stream
            BufferedOutputStream  dst = new BufferedOutputStream(fos);

            // get a buffered version of the connection output stream
            BufferedInputStream src = new BufferedInputStream(connection.getInputStream());

            long startTime = System.currentTimeMillis();

            // creat 32Kb buffer
            byte[] buffer = new byte[1024*32];
            int bytesRead;
            int fileSize = 0;

            // loop over the streams and xfer
            while((bytesRead = src.read(buffer,0,buffer.length))>0){
                dst.write(buffer,0,bytesRead);
                fileSize = fileSize + bytesRead;
            }

            // final flush of the file buffer (don't forget!)
            dst.flush();

            // close the file
            dst.close();

            // record the stop time
            long  endTime = System.currentTimeMillis();

            // compute elapsed time
            float seconds = (endTime - startTime) / 1000F;

            double xferSpeed = (fileSize/seconds)/(1024.0*1024.0);

            LOGGER.info(String.format("transfer speed: %5.2f MB/sec",xferSpeed));

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

            if (fos != null){
                try{
                    fos.close();
                } catch (IOException ioee){
                    // ignore
                }
            }
        }

    }

}
