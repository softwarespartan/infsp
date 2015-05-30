package com.infsp.tests;


import com.infsp.FileServer.FileServer;
import com.infsp.vfs.VfsFileMap;
import com.infsp.vfs.VirtualFileSystem;
import ipworks.IPWorksException;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 7/19/11
 * Time: 11:16 AM
 */
public class XferTest {

    //The following method shuts down an ExecutorService in two phases, first by calling shutdown to reject incoming tasks, and then calling shutdownNow, if necessary, to cancel any lingering tasks:
    public static void shutdownAndAwaitTermination(ExecutorService pool) {
       pool.shutdown(); // Disable new tasks from being submitted
       try {
         // Wait a while for existing tasks to terminate
         if (!pool.awaitTermination(60*15, TimeUnit.SECONDS)) {
           pool.shutdownNow(); // Cancel currently executing tasks
           // Wait a while for tasks to respond to being cancelled
           if (!pool.awaitTermination(60*15, TimeUnit.SECONDS))
               System.err.println("Pool did not terminate");
         }
       } catch (InterruptedException ie) {
         // (Re-)Cancel if current thread also interrupted
         pool.shutdownNow();
         // Preserve interrupt status
         Thread.currentThread().interrupt();
       }
    }

    public static void main(String[] args) throws IOException,InterruptedException,UnknownHostException,IPWorksException{


        final int port = 10101;
        //final String hostname = "10.0.1.5";
        final String hostname = "localhost";
        final String vfsConfigXml = "/Users/abel/IdeaProjects/InfStoragePlatform/src/infsp.xml";
        int NUM_THREADS = 8;

        // lots of small files here ~ 220K each
        final File filesRoot = new File("/infSP/testdata/smallFiles");

        // initialize a file system
        VirtualFileSystem vfs = new VirtualFileSystem(vfsConfigXml);

        // create distributed file map
        VfsFileMap vfm = new VfsFileMap();

        // add file map as listener for vfsEvents
        vfs.registerListener(vfm);

        // initialze a file server for our file system
        final FileServer fileServer = new FileServer(NUM_THREADS,vfs);

        // start the file server thread
        fileServer.start();

        // create execution service to run gets and puts

        ExecutorService exe ;

        // get the list of files
        File[] files = filesRoot.listFiles();
        int numFilesToTest = files.length;
        try{
            // submit each file for put
            exe = Executors.newFixedThreadPool(NUM_THREADS);
            for (int i = 0;i<numFilesToTest;i++){
                exe.execute(new PutFile(hostname,files[i].getPath()));
            }
            System.out.println("PUT "+numFilesToTest+" files to the infsp");
            XferTest.shutdownAndAwaitTermination(exe);


            exe = Executors.newFixedThreadPool(NUM_THREADS);
            for (int i = 0; i<numFilesToTest; i++){
                exe.execute(new GetFile(hostname, files[i].getPath(), "/infSP/dump/"));
            }
            System.out.println("GET "+numFilesToTest+" files to the infsp");
            XferTest.shutdownAndAwaitTermination(exe);


            exe = Executors.newFixedThreadPool(NUM_THREADS);
            for (int i = 0; i<numFilesToTest; i++){
                exe.execute(new DelFile(hostname, files[i].getPath()));
            }
            System.out.println("DEL "+numFilesToTest+" files to the infsp");
            XferTest.shutdownAndAwaitTermination(exe);



        } finally {
            vfm.willShutdown();
            vfm.shutdown();
        }
    }


}
