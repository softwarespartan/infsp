package com.infsp.vfs;

/**
 * Created by IntelliJ IDEA.
 * User: abelbrown
 * Date: 7/14/11
 * Time: 10:42 AM
 */
public interface VfsListener {

    void putFile(String disk, String relativeFilePath, double fileSize);
    void getFile(String relativeFilePath);
    void delFile(String relativeFilePath);

    void xferDidStart(String relativeFilePath);
    void xferDidFinish(String relativeFilePath);

    void willShutdown();
    void shutdown();
}
