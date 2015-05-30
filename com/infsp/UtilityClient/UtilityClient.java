package com.infsp.UtilityClient;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.Member;
import com.hazelcast.core.MultiMap;
import com.hazelcast.impl.ascii.memcache.Stats;
import com.infsp.UtilityClient.NodeStatsRequest;
import com.infsp.UtilityClient.ResolveRequest;
import com.infsp.vfs.VfsFileMap;
import com.infsp.vfs.VirtualFileSystem;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.net.*;
import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 8/4/11
 * Time: 4:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class UtilityClient extends Thread{

    private final ServerSocket serverSocket;

    private final ExecutorService pool;

    private final VfsFileMap fileMap;

    static final Logger LOGGER = Logger.getLogger(UtilityClient.class);

    private final int socketAcceptTimeout = 2000;

    public static final int port = 10104;

    public UtilityClient(int poolSize, VfsFileMap fileMap) throws IOException {

        LOGGER.setLevel(Level.ALL);

        try{

            // create new server listening socket on port
            this.serverSocket = new ServerSocket(this.port);

            // set the time out to some number so that do not
            // sit and wait forever on accept if the server
            // has been shut down.  Otherwise server will get
            // shutdown message but hang in socket.accept() forever
            this.serverSocket.setSoTimeout(this.socketAcceptTimeout);

            // let the log know were done
            LOGGER.debug("UtilityClient opened on port "+ port);

        }catch (IOException ioe){

            // let the log know we've got problems
            LOGGER.error("Failed to start utility client: "+ioe.toString());

            // don't swallow this!
            throw ioe;
        }

        // init rest of the object
        this.pool         = Executors.newFixedThreadPool(poolSize);

        this.fileMap      = fileMap;
    }

        public void run(){

        // let log know we've started
        LOGGER.debug("FileClient entering run loop");

        try {

            // verify pool status
            LOGGER.debug("pool isShutdown: "+pool.isShutdown());

            // loop until the execution pool is closed
            while(!pool.isShutdown()){

                try{

                    // and here we wait for socketAcceptTimeout seconds
                    //LOGGER.debug("waiting for connections ...");

                    // listen for incoming connections
                    final Socket connection = this.serverSocket.accept();

                    String clientAddr
                            = connection.getInetAddress().getHostAddress();

                    // let the log know we've heard the call
                    LOGGER.debug("connection established with "
                            +clientAddr+", executing request");

                    // execute the request via handler
                    pool.execute(new Handler(connection, fileMap.getFileMap()));

                } catch (SocketTimeoutException ste){
                    // healthy timeout here.
                    // a chance to recheck if the pool is open
                }
            }
        } catch (IOException ex){
            pool.shutdown();
        } catch (RejectedExecutionException ree){
            LOGGER.error("rejecting request execution");
        } finally{
            LOGGER.debug("shutting down pool");
            pool.shutdown();
            try{
                if(this.serverSocket != null){
                    LOGGER.debug("shutting down utility server socket");
                    this.serverSocket.close();
                }
            }catch (IOException ioe){

            }
        }

        LOGGER.debug("UtilityServer exiting run loop");
    }

    public void willShutdown(){pool.shutdown();}
    public void shutdown(){}
}

class Handler implements Runnable {

    private final Socket connection;
    private final MultiMap<String,String> fileMap;

    //static final Semaphore mkdirsLock = new Semaphore(1);

    static final Logger LOGGER = Logger.getLogger(Handler.class);

    Handler(Socket socket,MultiMap<String,String> fileMap) {
        LOGGER.setLevel(Level.ALL);
        this.connection = socket;
        this.fileMap = fileMap;
    }

    private void fail(BufferedWriter out,String msg) throws IOException{
        LOGGER.error(msg);
        out.write("fail: "+msg+'\n');
        out.flush();
        if (this.connection != null) this.connection.close();
    }

    public void run() {

        LOGGER.debug("servicing new utility request");

        BufferedReader in  = null;
        BufferedWriter out = null;

        try{
            // where we say stuff to the client
            out = new BufferedWriter(
                    new OutputStreamWriter(
                            connection.getOutputStream()));

            // where the client says stuff to us
            in  = new BufferedReader(
                    new InputStreamReader(
                            connection.getInputStream()));

            // get the mode from the client
            String mode = in.readLine();

            LOGGER.debug("mode: "+mode);

            if (mode.equalsIgnoreCase("nodestats")){

                Set<String> nodeStats = getNodeStats();

                // tell the client how many strings we will send
                out.write(nodeStats.size()+"\n"); out.flush();

                for (String stats : nodeStats){
                    out.write(stats+"\n");
                    out.flush();
                }

                return;


            } else if (mode.equalsIgnoreCase("resolve")){

                // should trap timeout for read
                String filePath = in.readLine();

                Set<String> absPaths = resolve(filePath);

                LOGGER.debug("absPath size: "+absPaths.size());

                out.write(String.valueOf(absPaths.size())+"\n"); out.flush();

                for (String path : absPaths){
                    LOGGER.debug("absPath: "+path);
                    out.write(path+"\n"); out.flush();
                }

                return;
            }

        }catch (IOException ioe){
           LOGGER.error("Fatal error "+ioe.toString());

        } finally {
            // klose the file
            try{
                if (this.connection != null
                        && this.connection.isBound()
                            && !this.connection.isClosed()){
                    this.connection.close();
                }
            }catch (IOException ioe){
                LOGGER.error("Error closing connection: "+ioe.toString());
            }
        }
    }

    private Set<String> getNodeStats(){

        // set of host stats
        Set<String> stats = new HashSet<String>();

        // get hosts
        Set<Member> clusterMembers
                = Hazelcast.getCluster().getMembers();

        // make it easy for ourselves
        Iterator<Member> nodes = clusterMembers.iterator();

        String hostname;
        String nodeStats;
        Member node;
        while(nodes.hasNext()){
            node = nodes.next();
            hostname = node.getInetSocketAddress().getHostName();
            nodeStats = "error";
            try{
                nodeStats = new NodeStatsRequest(hostname).get();
            }catch (Exception e){
                LOGGER.error("Could not get stats for host "
                        +hostname+": "+e.toString());
            }

            stats.add(hostname+":"+nodeStats);
        }

        return stats;
    }

    private Set<String> resolve(String filePath){

        Set<String> hostPaths = new HashSet<String>();
        String error = "";

        if (this.fileMap == null){
            error = "no fileMap for resolve request: "+filePath;
            hostPaths.add(error);
            LOGGER.error(error);
            return hostPaths;
        }

        // first make sure that the file is in the map
        if(!this.fileMap.containsKey(filePath)){
            error = "file "+filePath+" does not exist in file map";
            LOGGER.error(error);
            hostPaths.add(error);
            return hostPaths;
        }

        // get hosts for  file
        Collection<String> hostSet = this.fileMap.get(filePath);

        if(hostSet.size() == 0){
            error =  "No hosts for file "+filePath;
            LOGGER.error(error);
            hostPaths.add(error);
            return hostPaths;
        }

        String response = "error";
        for (String hostname : hostSet){
            try{
                response = new ResolveRequest(hostname).resolve(filePath);
            } catch (Exception e){
                LOGGER.error("could not resolve "
                        +filePath+" on host"+hostname);
            }

            hostPaths.add(hostname+":"+response.trim().replace("\\n",""));
        }

        return hostPaths;
    }
}

