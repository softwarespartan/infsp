package com.infsp.FileClient;

/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 7/28/11
 * Time: 2:22 PM
 * To change this template use File | Settings | File Templates.
 */


import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.Member;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.*;

public class PutFileRequest extends FileRequest implements Runnable{

    private final int nRep;

    private final String filePath;

    static final Logger LOGGER = Logger.getLogger(PutFileRequest.class);

    public PutFileRequest(String filePath,int replicationCount){

        super();

        LOGGER.setLevel(Level.ERROR);

        // the file to xfer
        this.filePath = filePath;

        // the number of replication to attempt
        this.nRep     = replicationCount;
    }

    public void run() {

        if(this.fileMap == null){
            LOGGER.error("no fileMap for putReq: "+filePath);
            return;
        }

        // make sure file does not exist already in map
        if (fileMap.containsKey(filePath)){
            LOGGER.error("File "+filePath+" already exists");
            return;
        }

        // first things first get list of hazelcast members
        Set<Member> clusterMembers = Hazelcast.getCluster().getMembers();

        // make a list from the set
        List<Member> clusterMembersList = new ArrayList<Member>(clusterMembers);

        // shuffel the list
        Collections.shuffle(clusterMembersList);

        // reasign the members
        clusterMembers = new HashSet<Member>(clusterMembersList);

        // get member iterator
        Iterator<Member> nodes = clusterMembersList.iterator();

        Member node;
        String hostname;
        int repCount = 0;

        // file xfer object
        PutFileXfer xfer;

        while(nodes.hasNext() &&  repCount < this.nRep){

            // get next node
            node = nodes.next();

            // super clients are not storage nodes
            if (node.isSuperClient()){
                continue;
            }

            // get the host name of the
            hostname = node.getInetSocketAddress().getHostName();

            try{
                // init new xfer for host
                xfer = new PutFileXfer(hostname,filePath);

                // attempt file transfer to host
                xfer.run();

            }catch (Exception e){
                LOGGER.error(e.toString()+" PUT-XFER on host "+hostname+" for file "+filePath);
                continue;
            }

            // need to ckeck the xfer status
            if (xfer.status){

                // blab about the xfer as info
                LOGGER.info(xfer.statusMsg+" to host "+hostname+" for file "+filePath);

                // update replica count
                repCount = repCount + 1;

            } else {

                // let error log know xfer was no
                LOGGER.error(xfer.statusMsg+" PUT-XFER failed on host "+hostname+" for file "+filePath);
            }
        }

        LOGGER.info(" File "+filePath+" transfered "+repCount+" times");
    }

    public String toString(){
        return "PutFileRequest: "+filePath;
    }
}
