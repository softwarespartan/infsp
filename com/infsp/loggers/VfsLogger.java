package com.infsp.loggers;

import com.infsp.vfs.VfsListener;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;

/**
 * Created by IntelliJ IDEA.
 * User: abelbrown
 * Date: 7/14/11
 * Time: 12:54 PM
 */

public class VfsLogger implements VfsListener{

    static final Logger LOGGER = Logger.getLogger(VfsLogger.class);

    public VfsLogger(){
        //BasicConfigurator.configure();
        LOGGER.setLevel(Level.ERROR);
    }

    public String toString(){
        return "VfsLogger";
    }

    public void putFile(String disk, String relativeFilePath,double fileSize) {

        // calculate the size of the file in GB
        double fsize = fileSize/(1024.0*1024.0*1024.0);

        // blab about it
        LOGGER.info("putting file: "+relativeFilePath+" on disk: "+disk+" of size "+String.format("%.2f",fsize));
    }

    public void getFile(String relativeFilePath) {
        LOGGER.info("getting file: "+relativeFilePath);
    }

    public void delFile(String relativeFilePath) {
        LOGGER.info("deleting file: "+relativeFilePath);
    }

    public void xferDidStart(String relativeFilePath) {
        LOGGER.info("starting transfer of file: "+relativeFilePath);
    }

    public void xferDidFinish(String relativeFilePath) {
        LOGGER.info("finished transfer of file: "+relativeFilePath);
    }

    public void willShutdown(){}
    public void shutdown(){}
}
