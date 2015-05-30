package com.infsp.UtilityClient;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 8/5/11
 * Time: 9:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class NodeStatsRequest extends UtilityRequest{

    public NodeStatsRequest(String hostname)
            throws UnknownHostException, SocketException{
        super(hostname,"nodestats");
    }

    public String get()
            throws SocketTimeoutException,IOException{
        try{

            // est. connection and init in and out buffers
            this.init();

            // tell the utility server we'd like node statistics
            this.out.write("nodestats"+"\n"); this.out.flush();

            return this.in.readLine();

        } finally {
            this.close();
        }
    }

    public static void main(String[] args)
            throws SocketTimeoutException,IOException{
        NodeStatsRequest nodeStatsRequest = new NodeStatsRequest("localhost");
        System.out.println(nodeStatsRequest.get());
    }


}
