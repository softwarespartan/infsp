package com.infsp.FileClient;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 7/28/11
 * Time: 6:51 PM
 */
public class DelFileRequest extends FileRequest implements Runnable{

    private final String filePath;

    static final Logger LOGGER = Logger.getLogger(DelFileRequest.class);
    public DelFileRequest(String filePath){
        super();
        LOGGER.setLevel(Level.ERROR);
        this.filePath = filePath;
    }

    public void run() {

        if (this.fileMap == null){
            LOGGER.error("no fileMap for delFileReq "+filePath);
            return;
        }

        // first make sure that the file is in the map
        if(!this.fileMap.containsKey(filePath)){
            LOGGER.error("file "+filePath+" does not exist");
            return;
        }

        // get hosts for src file
        Collection<String> hostSet = this.fileMap.get(filePath);

        if(hostSet.size() == 0){
            LOGGER.error("No hosts for file "+filePath);
            return;
        }

        // make it easy for ourselves
        Iterator<String> hosts = hostSet.iterator();
        String hostname;
        DelFileXfer xfer;

        while(hosts.hasNext()){
            hostname = hosts.next();
            xfer = new DelFileXfer(hostname,filePath);

            try{
                xfer.run();
            } catch (Exception e){
                LOGGER.error(e.toString());
                continue;
            }

            if (xfer.status){
                LOGGER.info("file "+ filePath+" removed from host "+hostname);
            }
        }

    }
}
