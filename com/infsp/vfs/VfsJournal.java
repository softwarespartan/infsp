package com.infsp.vfs;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import sun.tools.java.ClassNotFound;

import java.io.*;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by IntelliJ IDEA.
 * User: abelbrown
 * Date: 7/14/11
 * Time: 10:46 AM
 */
public class VfsJournal implements VfsListener{

    private ConcurrentHashMap<String,String> journal;

    private String journalPath = null;

    private final String DEFAULT_JOURNAL_PATH = "infsp.local.journal";

    static final Logger LOGGER = Logger.getLogger(VfsJournal.class);

    public VfsJournal(){
        LOGGER.setLevel(Level.ERROR);
        this.journal = new ConcurrentHashMap<String, String>();
    }

    public VfsJournal(String journalPath){

        this();

        if (journalPath != null
                && new File(journalPath).getParentFile().exists()){
            this.journalPath = new File(journalPath).getAbsolutePath();
        }else{
            this.journalPath = this.DEFAULT_JOURNAL_PATH;
        }

        LOGGER.debug("Journal file set to "+ this.journalPath);

        // if it actually exists then load it
        if(new File(journalPath).exists()){
            loadJournal();
        }

        // add runtime hook so that journal gets saved on ctrl-c etc
        Runtime.getRuntime().addShutdownHook(new VfsJournalShutdownHook(this));
    }

    public void putFile(String disk, String relativeFilePath,double fileSize) {

        relativeFilePath = cleanPath(relativeFilePath);

        if (! exists(relativeFilePath)){
            this.journal.put(relativeFilePath,disk);
            LOGGER.debug("putting file :"+ relativeFilePath+" on disk:"+disk);
        }
    }

    public void getFile(String relativeFilePath) {
    }

    public void delFile(String relativeFilePath) {
        if (exists(relativeFilePath)){
            try{
                this.journal.remove(relativeFilePath);
                LOGGER.debug("deleting file: "+relativeFilePath+" from journal ");

            } catch (NullPointerException npe){
                LOGGER.error("DEL: null pointer exception for file "+relativeFilePath);
            }
        }
    }

    public void xferDidStart(String relativeFilePath) {
    }

    public void xferDidFinish(String relativeFilePath) {
    }

    public void willShutdown(){
        if (this.journalPath != null){
            saveJournal();
        }

    }

    public void shutdown(){}

    public String toString(){
        return "VfsJournal";
    }

    public String resolve(String relativeFilePath){

        relativeFilePath = cleanPath(relativeFilePath);

        String fullFilePath = "";

        try{
            if (exists(relativeFilePath)){
                // pull the disk from the journal
                String disk = this.journal.get(relativeFilePath);

                // create full file /some/disk/rel/file/path
                File fullFile = new File(disk,relativeFilePath);

                // super defensive here
                // should never return a path
                // for file that does not exist!
                if (fullFile.exists()){
                    fullFilePath = fullFile.getPath();
                }else{
                    LOGGER.error("can not resolve b/c file "+relativeFilePath + "does not exist on disk");
                }
            }
        } catch (NullPointerException npe){
            LOGGER.error("RESOLVE: NULL pointer for file: "+relativeFilePath);
        }

        return fullFilePath;

    }

    public boolean exists(String relativeFilePath){

        relativeFilePath = cleanPath(relativeFilePath);

        boolean exists = false;

        try{
            exists = this.journal.containsKey(relativeFilePath);
        } catch (NullPointerException npe){
            // not much to do here but blab about it
            LOGGER.error("EXISTS: NULL pointer for file: "+relativeFilePath);
        }

        return exists;
    }

    public String cleanPath(String relativeFilePath){
        return new File(relativeFilePath).getPath();
    }

    public void loadJournal(){

        try{

            //open object file
            FileInputStream fis = new FileInputStream(this.journalPath);


            // wrap with obj stream so can import the object
            ObjectInputStream ois = new ObjectInputStream(fis);

            // blab about it on the log
            LOGGER.info("loading journal from file: "+this.journalPath);

            // read + cast object
            this.journal = (ConcurrentHashMap<String,String>) ois.readObject();

            // that's all
            ois.close();

        } catch (ClassNotFoundException cnfe){
            LOGGER.error("Journal not found in file: "+ this.journalPath);

            // just restart the journal
            this.journal = new ConcurrentHashMap<String, String>();;

        }catch (FileNotFoundException fnfe){
            LOGGER.error("File " + this.journalPath + " not found");
        } catch (IOException ioe){
            LOGGER.error("Could not load journal at " + this.journalPath);
        }
    }

    public void saveJournal(){

        if (this.journalPath == null) return;

        try{
            // open the file for storage
            FileOutputStream fos   = new FileOutputStream(this.journalPath);

            // wrap the file stream so i can export java objects
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            // blab about it on the log
            LOGGER.info("Saving journal to file: "+this.journalPath);

            // export the hashmap journal
            oos.writeObject(this.journal);

            // that a wrap
            oos.close();

        } catch (IOException ioe){
            LOGGER.error("Fatal error occured whist saving journal to "+ this.journalPath);
            LOGGER.error(ioe.toString());
        }

    }

    public int size(){
        return this.journal.size();
    }

    private static class VfsJournalShutdownHook extends Thread {

        private final VfsJournal journal;

        static final Logger LOGGER = Logger.getLogger(VfsJournalShutdownHook.class);

        public VfsJournalShutdownHook(VfsJournal journal){this.journal = journal;}

        public void run() {
            VfsJournal.LOGGER.error("Executing shutdown hook to save journal");
            this.journal.saveJournal();
        }
  }
}
