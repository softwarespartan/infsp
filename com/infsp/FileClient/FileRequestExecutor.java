package com.infsp.FileClient;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.MultiMap;
import com.infsp.vfs.VfsFileMap;
import org.apache.log4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 7/28/11
 * Time: 1:54 PM
 */
public class FileRequestExecutor extends ThreadPoolExecutor {

    private final MultiMap<String,String> fileMap;

    static final Logger LOGGER = Logger.getLogger(FileRequestExecutor.class);

    public FileRequestExecutor(int poolSize){

        // configure thread pool
        super(poolSize, poolSize,
              60, TimeUnit.SECONDS,
              new LinkedBlockingQueue<Runnable>(),
              new ThreadFactory() {
                  public Thread newThread(Runnable runnable) {
                      return new Thread(runnable);
                  }
              });

        this.fileMap = Hazelcast.getMultiMap(VfsFileMap.mapName);

        LOGGER.debug("FileRequestExecutor started ...");
    }


    @Override
    protected void beforeExecute(Thread thread, Runnable runnable) {
        try{
            FileRequest fileRequest = (FileRequest)runnable;
            fileRequest.fileMap     = this.fileMap;
        } finally {
            super.beforeExecute(thread, runnable);
        }
    }

    @Override
    protected void afterExecute(Runnable runnable, Throwable throwable) {
        super.afterExecute(runnable, throwable);
    }

    @Override
    protected void terminated() {
        LOGGER.info("FileRequestExecutor is down");
        super.terminated();
    }

    public void willShutdown(){
        this.shutdown();
    }

    public void kill(){
        this.shutdownNow();
    }


}
