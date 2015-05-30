package com.infsp.FileClient;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 7/28/11
 * Time: 7:27 PM
 */
public class FileClientTest{

    public static void main(String[] args){

//        Infsp infsp = new Infsp();

        // file client to transfer files
        //FileClient fileClient = new FileClient(4);

        // lots of small files here ~ 220K each
        final File filesRoot = new File("/infSP/testdata/smallFiles");

        // get the file list
        File[] files = filesRoot.listFiles();

        FileRequestXmlEncoder encoder = new FileRequestXmlEncoder();

        int numFiles = 50;
        for (int i = 0; i < 4; i++){
            encoder.putFile(files[i].getPath(),2);
        }

        System.out.println(encoder.encode());
        TellFileClient.submit(encoder.encode());
        //System.out.println(TellFileClient.submit(encoder.encode()));

//        encoder.clear();
//        for (int j = 0; j < numFiles; j++){
//            encoder.getFile(files[j].getPath(),"/infSP/dump");
//        }
//        //System.out.println(TellFileClient.submit(encoder.encode()));
//
//        encoder.clear();
//        for (int k = 0; k < numFiles; k++){
//            encoder.delFile(files[k].getPath());
//        }
        //System.out.println(TellFileClient.submit(encoder.encode()));
    }
}
