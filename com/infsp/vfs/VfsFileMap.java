package com.infsp.vfs;

/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 7/25/11
 * Time: 4:03 PM
 */

import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.MultiMap;
import com.hazelcast.core.Hazelcast;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.omg.PortableInterceptor.LOCATION_FORWARD;

import java.io.FileNotFoundException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collection;

public class VfsFileMap implements VfsListener{

    private final String hostname;
    private final MultiMap<String,String> fileMap;
    private volatile boolean isShutdown =  false;

    public static final String mapName = "com.infsp.vfs.FileMap";

    public static final String configPath = "/infSP/hazelcast.xml";

    static final Logger LOGGER = Logger.getLogger(VfsFileMap.class);

    public VfsFileMap()throws UnknownHostException{

        LOGGER.setLevel(Level.ERROR);

        try{
            this.hostname = java.net.InetAddress.getLocalHost().getHostAddress();
            LOGGER.error("VfsFileMap initializing with hostname "+this.hostname);

        }catch (UnknownHostException uhe){

            // let the log know
            LOGGER.error(uhe.toString());

            // do not swallow.  lets controller katch it
            throw uhe;
        }

        // attach to the multimap
        this.fileMap  = Hazelcast.getMultiMap(VfsFileMap.mapName);
    }

    public void putFile(String disk, String relativeFilePath, double fileSize) {

        // lyf cycle
        if (this.isShutdown){
            LOGGER.error("PUT:  host "+this.hostname+" can not put file "+relativeFilePath+" b/c it is shutdown");
            return;
        }

        // defensive - make sure not already mapped to this file
        if(this.fileMap.containsEntry(relativeFilePath,this.hostname)){
            LOGGER.error("PUT: host "+this.hostname+" already mapped to file "+relativeFilePath);
            return;
        }

        // finall ... map self to the file
        this.fileMap.put(relativeFilePath,this.hostname);
    }

    public void getFile(String relativeFilePath) {
    }

    public void delFile(String relativeFilePath) {

        // lyf cycle
        if(this.isShutdown){
            LOGGER.error("DEL:  host "+this.hostname+" can not delete file "+relativeFilePath+" b/c it is shutdown");
            return;
        }

        //defensive - make sure file actually exists
        if(!exists(relativeFilePath)){
            LOGGER.error("DEL: File "+relativeFilePath+" does not exist in FileMap");
            return;
        }

        // make sure you're actually host-mapped to this file
        if(!this.fileMap.containsEntry(relativeFilePath,this.hostname)){
            LOGGER.error("DEL: host "+this.hostname+" is not mapped to file "+relativeFilePath);
            return;
        }

        // delete only your own name from the map
        this.fileMap.remove(relativeFilePath,this.hostname);

        // should remove the entry from the map is now empty
//        if (this.fileMap.get(relativeFilePath).size()==0){
//            this.fileMap.r
//        }
    }

    public void xferDidStart(String relativeFilePath) {
    }

    public void xferDidFinish(String relativeFilePath) {
    }

    public void willShutdown() {
        this.isShutdown = true;
    }

    public void shutdown() {
        Hazelcast.shutdownAll();
    }

    public boolean exists(String relativeFilePath){
        return this.fileMap.containsKey(relativeFilePath);
    }

    public int size(){
        return this.fileMap.size();
    }

    public String toString(){
        return "VfsFileMap("+this.hostname+")";
    }

    public MultiMap<String,String> getFileMap(){
        return this.fileMap;
    }

    public static void main(String[] args){

    }

}
