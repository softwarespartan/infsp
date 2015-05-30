package com.infsp.utils;

import com.infsp.UtilityClient.UtilityClient;
import com.infsp.UtilityClient.UtilityRequest;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 8/5/11
 * Time: 4:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class infsp_node_stats extends UtilityRequest{

    public infsp_node_stats(String hostname) throws  UnknownHostException, SocketException{
        super(hostname,"nodestats", UtilityClient.port);
    }

    public Set<String> get(){

        Set<String> stats = new HashSet<String>();

        try{
            this.init();

            this.out.write(this.mode+"\n"); out.flush();

            int numStats = Integer.parseInt(in.readLine());

            for (int i = 0; i < numStats; i++){
                stats.add(in.readLine());
            }

        }catch (Exception e){
            System.err.println(e.toString());
        } finally {
            this.close();
            return stats;
        }
    }

    public static void main(String[] args) {

        String hostname = "";

        if (args.length == 0){
            hostname = "localhost";
        } else {
            hostname = args[0];
        }

        double totalFreeSpace  = 0;
        double totalTotalSpace = 0;
        double totalNumFiles   = 0;
        double totalUsed;
        double totalPercentUsed;

        double freeSpace;
        double totalSpace;
        int numFiles;

        double used;
        double percentUsed;

        String node;
        String nodeStats;

        String[] lineParts;

        System.out.println("                   size         free       used       %    #files");

        try{
            Set<String> stats
                    = new infsp_node_stats(hostname).get();

            for (String s : stats){

                lineParts = s.split("\\s+");

                if (lineParts.length != 4){
                    System.out.println(s);
                    continue;
                }

//                for (int i = 0; i < lineParts.length; i++){
//                    System.out.println(lineParts[i]);
//                }

                node       = lineParts[0];
                freeSpace  = Double.parseDouble(lineParts[1]);
                totalSpace = Double.parseDouble(lineParts[2]);
                numFiles   = Integer.parseInt(lineParts[3]);

                totalFreeSpace  += freeSpace;
                totalTotalSpace += totalSpace;
                totalNumFiles   += numFiles;

                used = (totalSpace-freeSpace);
                percentUsed = used/totalSpace;

                nodeStats = String.format("%15s  %8.2f    %8.2f    %6.1f    %6.3f   %-10d",
                        node,totalSpace,freeSpace,used,percentUsed,numFiles);

                System.out.println(nodeStats);
            }

            used = (totalTotalSpace - totalFreeSpace);
            percentUsed = used/totalTotalSpace;

            String totalStats =  String.format("%15s  %8.2fT   %8.2fT   %6.1fG   %6.3f   %-10d",
                    "total",totalTotalSpace/1024.0,totalFreeSpace/1024.0,used,percentUsed,(int)totalNumFiles);

            System.out.println("--------------------------------------------------------------------");
            System.out.println(totalStats);

        }catch (Exception e){
            System.err.println("Error retreiving node stats ");
            System.err.println(e.toString());
            System.exit(2);
        }
    }


}
