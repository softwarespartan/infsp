package com.infsp.UtilityClient;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 8/5/11
 * Time: 11:24 AM
 * To change this template use File | Settings | File Templates.
 */
public class ResolveRequest extends UtilityRequest{

    public ResolveRequest(String hostname)
        throws UnknownHostException, SocketException {
        super(hostname,"resolve");
    }

    public String resolve(String filePath)
            throws SocketTimeoutException,IOException {
        try{
            this.init();

            this.out.write(this.mode+"\n");out.flush();
            this.out.write(filePath+"\n"); out.flush();

            return in.readLine();

        } finally{
            this.close();
        }
    }

    public static void main(String[] args)
            throws SocketTimeoutException,IOException{
        ResolveRequest resolveRequest = new ResolveRequest("10.0.1.3");
        System.out.println(resolveRequest.resolve("/infSP/testdata/smallFiles/os107822.sp3"));
    }
}
