package com.infsp.UtilityClient;

import com.infsp.UtilityServer.UtilityServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;


/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 8/4/11
 * Time: 5:49 PM
 */
public class UtilityRequest {

    public final String mode;
    public final String hostname;
    private final int port;

    public final Socket connection;

    private final int connectTimeLimit = 10000;
    private final int readTimeLimit    = 4000;
    private final SocketAddress socketAddress;

    public BufferedWriter out;
    public BufferedReader in;

    public UtilityRequest(String hostname, String mode) throws UnknownHostException, SocketException{

        this.mode     = mode;
        this.hostname = hostname;

        this.port = UtilityServer.port;

        this.connection = new Socket();

        InetAddress inetAddress
                = InetAddress.getByName(this.hostname);

        this.socketAddress
                = new InetSocketAddress(inetAddress,this.port);

        this.connection.setSoTimeout(this.readTimeLimit);

    }

    public UtilityRequest(String hostname, String mode,int port) throws UnknownHostException, SocketException{

        this.mode     = mode;
        this.hostname = hostname;

        this.port = port;

        this.connection = new Socket();

        InetAddress inetAddress
                = InetAddress.getByName(this.hostname);

        this.socketAddress
                = new InetSocketAddress(inetAddress,this.port);

        this.connection.setSoTimeout(this.readTimeLimit);

    }

    public void init() throws SocketTimeoutException, IOException{

        this.connection.connect(this.socketAddress,this.connectTimeLimit);

        in = new BufferedReader(
                                new InputStreamReader(
                                      connection.getInputStream()));

        out = new BufferedWriter(
                                new OutputStreamWriter(
                                      connection.getOutputStream()));
    }

    public void close(){
        if (this.connection != null
                && this.connection.isBound()
                    && ! this.connection.isClosed()){
            try{
                this.connection.close();
            }catch (IOException ioe){
                // no op
            }
        }
    }

}
