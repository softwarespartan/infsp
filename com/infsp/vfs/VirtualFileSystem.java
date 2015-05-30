package com.infsp.vfs;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import com.infsp.loggers.VfsLogger;
import com.infsp.parsers.VfsXmlParser;
import ipworks.IPWorksException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import sun.tools.asm.TryData;

/**
 * Created by IntelliJ IDEA.
 * User: abelbrown
 * Date: 7/14/11
 * Time: 10:23 AM
 */
public class VirtualFileSystem {

    public VfsJournal journal;
    private final Set<String> diskSet = new HashSet<String>();
    private final int maxXfer = 4;
    private Set<VfsListener> listeners;

    static final Logger LOGGER = Logger.getLogger(VirtualFileSystem.class);

    public VirtualFileSystem(){

        LOGGER.setLevel(Level.INFO);

        this.listeners = new HashSet<VfsListener>();
        this.registerListener(new VfsLogger());

    }

    public VirtualFileSystem(String xmlConfigFile)throws IPWorksException{

        this();

        VfsXmlParser xmlParser;

        // parse the config file
        try {
             xmlParser = new VfsXmlParser(xmlConfigFile);

            // add the parsed disks
            for (String disk : xmlParser.diskSet){
                this.addDisk(disk);
            }

            // create journal
            this.journal = new VfsJournal(xmlParser.journalPath);

            // register it for notifications
            this.registerListener(this.journal);

            // finally, see if need to build the journal
            if (! new File(xmlParser.journalPath).exists()){
                this.buildJournal();
            }

        } catch (IPWorksException e){
            // don't swallow this exception
            throw e;
        }

    }

    public void addDisk(String path){

        File disk = new File(path);
        if (disk.exists()
                && disk.isAbsolute()
                    && disk.isDirectory()
                        && disk.canRead()
                            && disk.canWrite())
        {
          LOGGER.info("Adding disk: " + disk.getPath());
          this.diskSet.add(disk.getPath());
        } else {
            LOGGER.error("Could not add disk: "+disk.getPath());
        }
    }

    public void registerListener(VfsListener listener){
        LOGGER.debug("Adding listener: "+ listener.toString());
        this.listeners.add(listener);
    }

    public void buildJournal(){

        // try to build the journal
        new JournalBuilder().buildJournal(this.diskSet,this.listeners);

    }

    public void shutdown(){
        for (VfsListener listener : this.listeners){
            listener.shutdown();
        }
    }

    public void willShutdown(){
        for(VfsListener listener : this.listeners){
            listener.willShutdown();
        }
    }

    public boolean exists(String relativeFilePath){
        return this.journal.exists(relativeFilePath);
    }

    public String resolve(String relativeFilePath){
        return this.journal.resolve(relativeFilePath);
    }

    public String diskForFileOfSize(double fileSize){

        if (this.diskSet.size()==0){
            return "";
        }

        // file size in bytes
        String[] diskArray = this.diskSet.toArray(new String[0]);

        // just choose random disk for now
        return diskArray[new Random().nextInt(this.diskSet.size())];
    }

    public void notifyAllPutFile(String disk,String relativeFilePath, double fileSize){
        for(VfsListener listener : this.listeners){
            listener.putFile(disk,relativeFilePath,fileSize);
        }
    }

    public void notifyAllGetFile(String relativeFilePath){
        for(VfsListener listener : this.listeners){
            listener.getFile(relativeFilePath);
        }
    }

    public void notifyAllDelFile(String relativeFilePath){
        for (VfsListener listener : this.listeners){
            listener.delFile(relativeFilePath);
        }
    }

    public void notifyAllXferDidStart(String relativeFilePath){
        for(VfsListener listener : this.listeners){
            listener.xferDidStart(relativeFilePath);
        }
    }

    public void notifyAllXferDidFinish(String relativeFilePath){
        for(VfsListener listener : this.listeners){
            listener.xferDidFinish(relativeFilePath);
        }
    }

    public double getFreeSpace(){

        double freeSpace = 0.0;

        for (String disk : this.diskSet){
            freeSpace += new File(disk).getFreeSpace();
        }

        return freeSpace;
    }

    public double getTotalSpace(){

        double totalSpace = 0.0;

        for (String disk : this.diskSet){
            totalSpace += new File(disk).getTotalSpace();
        }

        return totalSpace;
    }

    public int getNumberOfFiles(){
        return this.journal.size();
    }


    public static void main(String[] args)throws IPWorksException{

        VirtualFileSystem vfs
                = new VirtualFileSystem("/infSP/infsp.xml");

        String file = "TRS/websites/XSLT_en.png";
        System.out.println("TESTING: "+file);
        System.out.println("exists: "+ vfs.exists(file));
        if(vfs.exists(file)){
            System.out.println("resolves to: "+ vfs.resolve(file));
        }

        file = "workspace/Python_GPS_Tools/src/gpsdate.py";
        System.out.println("TESTING: "+file);
        System.out.println("exists: "+ vfs.exists(file));
        if(vfs.exists(file)){
            System.out.println("resolves to: " +vfs.resolve(file));
        }

        for (int i=0;i<10;i++){
            System.out.println("file to write: "+vfs.diskForFileOfSize(10L));
        }

        double gb = 1024*1024*1024*1.0;
        System.out.println(String.format("%7.2f %7.2f %10d",vfs.getFreeSpace()/gb,vfs.getTotalSpace()/gb,vfs.getNumberOfFiles()));
        System.out.println("VFS free space: "+vfs.getFreeSpace()/gb + " GB");
        System.out.println("VFS total space: "+vfs.getTotalSpace()/gb +" GB");
        System.out.println("VFS number of files: "+vfs.getNumberOfFiles());

        vfs.willShutdown();
        vfs.shutdown();
    }

}
