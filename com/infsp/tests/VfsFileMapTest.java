package com.infsp.tests;

/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 7/25/11
 * Time: 6:41 PM
 * To change this template use File | Settings | File Templates.
 */

import com.hazelcast.core.MultiMap;
import com.hazelcast.core.Hazelcast;
import org.apache.log4j.Logger;

import java.net.UnknownHostException;
import java.util.Collection;

public class VfsFileMapTest {

    static final Logger LOGGER = Logger.getLogger(VfsFileMapTest.class);

    public static void main(String[] args) throws InterruptedException {

        MultiMap<String,String> fileMap = Hazelcast.getMultiMap("com.infsp.vfs.FileMap");

        while(true){
            System.out.println("VfsFileMaps size: "+fileMap.size());
            Thread.sleep(10*1000);
        }

    }
}
