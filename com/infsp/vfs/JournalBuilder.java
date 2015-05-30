package com.infsp.vfs;

import com.sun.org.apache.bcel.internal.generic.NEW;

import java.io.File;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: abelbrown
 * Date: 7/14/11
 * Time: 11:45 AM
 */

public class JournalBuilder {

    private Set<VfsListener> listeners;

    public JournalBuilder(){}

    public void buildJournal(Set<String> disks,Set<VfsListener> listeners){

        this.listeners = listeners;

        for (String disk : disks){
            buildJournalWithDisk(disk);
        }

        // make sure to remove listeners
        this.listeners = null;
    }

    private void buildJournalWithDisk(String disk){
       traverse(disk, new File(disk));
    }

    private void traverse(String disk,File dir){

        if (dir.isDirectory() && ! dir.isHidden()) {
            String[] children = dir.list();
            if (children == null){return;}
            for (int i=0; i<children.length; i++) {
                traverse(disk, new File(dir, children[i]));
            }
        } else {
            processFile(disk,dir);
        }
    }

    private void processFile(String disk,File file){

        // defensive check for file once again
        if (file.isFile() && ! file.isHidden()){

            // get the relative path for the file
            String relativeFilePath
                    = new File(disk).toURI().relativize(file.toURI()).getPath();

            // make sure relative file path starts with "/" or "\"
            relativeFilePath = File.separator + relativeFilePath;

            // do the deed
            this.notifyAllPut(disk,relativeFilePath,file.length());
        }
    }

    private void notifyAllPut(String disk, String relativeFilePath,double fileSize){
        for (VfsListener listener : this.listeners) {
            listener.putFile(disk,relativeFilePath,fileSize);
        }

    }

    public String toString(){
        return "JournalBuilder";
    }
}
