package com.infsp.FileClient;

import com.sun.tools.internal.ws.processor.model.Response;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 7/29/11
 * Time: 3:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class TellFileClient {

    static final Logger LOGGER = Logger.getLogger(TellFileClient.class);

    public static void submit(String xmlData){

        BasicConfigurator.configure();

        Socket connection = new Socket();
        String response = "";
        try{

            InetAddress iaddr = InetAddress.getByName("localhost");

            SocketAddress saddr = new InetSocketAddress(iaddr,FileClient.port);

            connection.setSoTimeout(4000);
            connection.connect(saddr,10000);

            LOGGER.debug("got connection ");

            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(
                                          connection.getInputStream()));

            BufferedWriter out = new BufferedWriter(
                                    new OutputStreamWriter(
                                          connection.getOutputStream()));


            // tell the server we want to put a file
            out.write(xmlData,0,xmlData.length());
            out.flush();

            LOGGER.debug("finished writing xml ... waiting for response");
            //response = in.readLine();


            //return response;

        }catch (Exception e){
            LOGGER.error(e.toString());
        } finally {
            // klose the file
            try{
                if (connection != null
                        && connection.isBound()
                            && !connection.isClosed()){
                    connection.close();
                }
            }catch (IOException ioe){
                LOGGER.error("Error closing connection: "+ioe.toString());
            }
        }
    }

}
