package com.infsp;

import com.hazelcast.config.Config;
import com.hazelcast.config.NetworkConfig;
import com.infsp.FileClient.FileClient;
import com.infsp.FileServer.FileServer;
import com.infsp.UtilityServer.UtilityServer;
import com.infsp.vfs.VfsFileMap;
import com.infsp.vfs.VirtualFileSystem;
import ipworks.IPWorksException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;

import com.hazelcast.core.Hazelcast;
import com.infsp.UtilityClient.UtilityClient;

/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 7/27/11
 * Time: 3:06 PM
 */
public class Infsp {

    // set ports for services
    //public static final int fileServerPort = 10101;
    //public static final int fileClientPort = 10102;
    //public static final int utilityPort    = 10103;

    // number of threads per service
    public static final int numThreads = 12;

    // look for config file in same dir as jar file
    public final String infspConfigXml = "/infSP/infsp.xml";

    // where this node stores data
    public VirtualFileSystem vfs;

    // where this node tells every one
    // about the data it stores
    public VfsFileMap vfm;

    // file services
    public FileServer     fileServer;
    public FileClient     fileClient;

    public UtilityServer  utilityServer;
    public UtilityClient  utilityClient;

    // blab about stuff here
    static final Logger LOGGER = Logger.getLogger(Infsp.class);

    public Infsp(){

        BasicConfigurator.configure();
        LOGGER.setLevel(Level.ALL);

        try{

            this.startFileServer();
            this.startUtilityServer();
            this.startFileMap();
            this.vfs.registerListener(this.vfm);
            this.startFileClient();
            this.startUtilityClient();

        } catch (Exception e){
            this.doShutdown();
        }
    }

    private void startFileMap() throws UnknownHostException{
        // initialize the global file mapping
        try{
            this.vfm = new VfsFileMap();
        } catch (UnknownHostException uhe){
            //LOGGER.debug(uhe.toString());
            LOGGER.error("unable to start FileMap"+uhe.toString());
            throw uhe;
        }
    }

    private void startFileServer() throws IOException,IPWorksException{

        // init new file server connected to our vfs
        try{
            if (this.vfs == null){
                // need vfs before can start file server
                this.startVirtualFileSystem();
            }

            this.fileServer
                    = new FileServer(Infsp.numThreads,
                                     this.vfs);
            this.fileServer.start();
        }catch (IOException ioe){
            //LOGGER.debug(ioe.toString());
            LOGGER.error("unable to start FileServer");
            throw ioe;
        } catch (IPWorksException ipwe){
            throw ipwe;
        }
    }

    private void startVirtualFileSystem() throws IPWorksException{
        // initialize a file system
        try{
            this.vfs = new VirtualFileSystem(this.infspConfigXml);
        }catch (IPWorksException ipwe){
            //LOGGER.debug(ipwe.toString());
            LOGGER.error("unable to start VirtualFileSystem");
            throw ipwe;
        }
    }

    private void startFileClient() throws IOException{

        try{
            this.fileClient = new FileClient(Infsp.numThreads);
        } catch (IOException ioe){
            LOGGER.error("FileClient failed to start: "+ioe.toString());
            throw ioe;
        }

        this.fileClient.start();
    }

    private void startUtilityServer() throws IOException,IPWorksException{
        try{
            if (this.vfs == null){
                this.startVirtualFileSystem();
            }
            this.utilityServer
                    = new UtilityServer(Infsp.numThreads,this.vfs);

            this.utilityServer.start();

        } catch (IOException ioe){
            LOGGER.error("UtilityServer failed to start: "+ioe.toString());
            throw ioe;
        } catch (IPWorksException ipwe){
            LOGGER.error("UtilityServer failed to start: "+ipwe.toString());
            throw ipwe;
        }
    }

    private void startUtilityClient() throws Exception{

        try{
            if(this.vfm == null){
                this.startFileMap();
            }

            this.utilityClient
                    = new UtilityClient(Infsp.numThreads,this.vfm);

            this.utilityClient.start();
        } catch (Exception e){
            LOGGER.error("Utility client failed to start: "+e.toString());
            throw e;

        }

    }

    private void willShutdown(){

        LOGGER.info("willShutdown ...");

        try{

            if (this.vfs != null) {this.vfs.willShutdown();}
            if (this.vfm != null) {this.vfm.willShutdown();}

            if (this.fileServer    != null) {   this.fileServer.willShutdown();}
            if (this.fileClient    != null) {   this.fileClient.willShutdown();}
            if (this.utilityServer != null) {this.utilityServer.willShutdown();}
            if (this.utilityClient != null) {this.utilityClient.willShutdown();}

        } catch (Exception e){
            LOGGER.debug(e.toString());
        }
    }

    private void shutdown(){

        LOGGER.info("shutdown ...");

        try{

            if (this.vfs != null){this.vfs.shutdown();}
            if (this.vfm != null){this.vfm.shutdown();}

            if (this.fileServer    !=null){    this.fileServer.shutdown();}
            if (this.fileClient    !=null){    this.fileClient.shutdown();}
            if (this.utilityServer != null){this.utilityServer.shutdown();}
            if (this.utilityClient != null){this.utilityClient.shutdown();}

        } catch (Exception e){
            LOGGER.error(e.toString());
        }
    }

    public void doShutdown(){
        this.willShutdown();
        this.shutdown();
    }

    public static void main(String[] args){
        Infsp infsp = new Infsp();
        //infsp.doShutdown();
    }
}
