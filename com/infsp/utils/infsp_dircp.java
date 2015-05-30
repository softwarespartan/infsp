package com.infsp.utils;

import com.infsp.FileClient.FileRequestXmlEncoder;
import com.infsp.FileClient.TellFileClient;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 7/30/11
 * Time: 6:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class infsp_dircp{

    private final static FileRequestXmlEncoder encoder
            = new FileRequestXmlEncoder();

    public static volatile int fileCount = 0;

    private static void traverse(File dir){

        if (dir.isDirectory() && ! dir.isHidden()) {
            String[] children = dir.list();
            if (children == null){return;}
            for (int i=0; i<children.length; i++) {
                traverse(new File(dir, children[i]));
            }
        } else {
            processFile(dir);
        }
    }

    private static void processFile(File file){
        // defensive check for file once again
        if (file.isFile() && ! file.isHidden()){
            encoder.putFile(file.getAbsolutePath(),3);
            //encoder.getFile(file.getAbsolutePath(),"/infSP/dump");
            //encoder.delFile(file.getAbsolutePath());
            //TellFileClient.submit(encoder.encode());
            fileCount += 1;
            //encoder.clear();
        }
    }


    public static void main(String[] args){

        if (args.length == 0){
            System.err.println("USAGE infsp_dircp /some/path/to/dir");
            System.exit(1);
        }

        // get the directory to traverse
        String dir = args[0];

        // init new file from input args
        File src = new File(dir);

        // make sure that input exists
        if (!src.exists()){
            System.err.println("path "+src.getAbsolutePath()+" does not exist");
            System.exit(2);
        }

        // make sure that input path is a directory
        if(!src.isDirectory()){
            System.err.println("path "+src.getAbsolutePath()+" is not a directory");
            System.exit(3);
        }

        System.out.println("Traversing " + src);

        traverse(src);

        String response;
        TellFileClient.submit(encoder.encode());
        ///System.out.println(response);
        System.out.println("Processed "+fileCount+" files");

    }
}
