package com.infsp.FileClient;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 7/28/11
 * Time: 5:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetFileRequest extends FileRequest implements Runnable{

    public final String src;
    public final String dst;

    static final Logger LOGGER = Logger.getLogger(GetFileRequest.class);


    public GetFileRequest(String src, String dst){

        super();

        LOGGER.setLevel(Level.ERROR);

        this.src = src;
        this.dst = dst;
    }

    public void run() {

        if (this.fileMap == null){
            LOGGER.error("no fileMap for getFileReq "+src);
            return;
        }

        // first make sure that the file is in the map
        if(!this.fileMap.containsKey(src)){
            LOGGER.error("file "+src+" does not exist");
            return;
        }

        // get hosts for src file
        Collection<String> hostSet = this.fileMap.get(src);

        if(hostSet.size() == 0){
            LOGGER.error("No hosts for file "+src);
            return;
        }

        // make it easy for ourselves
        Iterator<String> hosts = hostSet.iterator();

        String hostname;
        GetFileXfer xfer;
        boolean wasTransfered = false;

        while(hosts.hasNext()){

            // get a host for the file
            hostname = hosts.next();

            try {
                xfer = new GetFileXfer(hostname,src,dst);
                xfer.run();
            } catch (Exception e){
                LOGGER.error(e.toString()+" GET-XFER failed on host "+hostname+" for file "+src);
                continue;
            }

            if(xfer.status){
                wasTransfered = true;
                LOGGER.info(xfer.statusMsg+" to host "+hostname+ " for file "+src);
                break;
            }
        }

        if (!wasTransfered){
            LOGGER.error("could not get file "+src);
        }
    }

    public String toString(){
        return "GetFileRequest: "+src;
    }
}
