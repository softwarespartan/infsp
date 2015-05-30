package com.infsp.utils;

import com.infsp.UtilityClient.UtilityClient;
import com.infsp.UtilityClient.UtilityRequest;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;


/**
 * Created by IntelliJ IDEA.
 * User: abelbrown
 * Date: 8/1/11
 * Time: 5:29 PM
 */
public class infsp_resolve extends UtilityRequest{

    public infsp_resolve(String hostname) throws  UnknownHostException, SocketException{
        super(hostname,"resolve", UtilityClient.port);
    }

    public Set<String> resolve(String filePath){

        Set<String> absPaths = new HashSet<String>();

        try{
            this.init();

            this.out.write(this.mode+"\n"); out.flush();
            this.out.write(filePath+"\n"); out.flush();

            int numResolve = Integer.parseInt(in.readLine());

            for (int i = 0; i < numResolve; i++){
                absPaths.add(in.readLine());
            }

        }catch (Exception e){
            System.err.println(e.toString());
        } finally {
            this.close();
            return absPaths;
        }
    }

    public static void main(String[] args) {

        String hostname = "";
        String filePath = "";

        if (args.length == 0){
            System.err.println("USAGE: infsp_resolve [hostname] filePath");
            System.exit(1);
        }

        if (args.length == 1){
            hostname = "localhost";
            filePath = args[0];
        }

        if (args.length == 2){
            hostname = args[0];
            filePath = args[1];
        }

        try{
            Set<String> absPaths
                    = new infsp_resolve(hostname).resolve(filePath);

            for (String s : absPaths){
                System.out.println(s);
            }

        }catch (Exception e){
            System.err.println("Error resolving file "+filePath);
            System.err.println(e.toString());
            System.exit(2);
        }
    }
}
