package networking;

import global.Global;
import querytree.QueryParser;
import querytree.QueryTree;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import edu.mit.eecs.parserlib.UnableToParseException;

/**
 * The class running on the head node, accepting user command (CLI),
 * and communicate with the nodes through TCP protocol.<br>
 * Note: This class is not supposed to use anything from package simpledb, the only
 * way to communicate with it should be a network protocol. (Correct it if this is wrong)
 */
public class HeadNode {
    
    private final List<String> childrenIps = new ArrayList<>();
    private final List<Integer> childrenPorts = new ArrayList<>();
    private Result result;

    public HeadNode(){
        this.result = null;
    }
    
    /**
     * Add a child node to the head node
     * @param childIp the child IP address represented by a String
     * @param childPort the port of the child node
     */
    public void addChildNode(String childIp, int childPort) {
        // TODO: check format of the child IP
        childrenIps.add(childIp);
        childrenPorts.add(childPort);
    }

    /**
     * Start the command line interface to accept client typing queries
     * @throws IOException if the I/O is interrupted
     */
    public void getInput() throws IOException{
        final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("SimpleDist> ");
            final String input = in.readLine();
            if (input.equals("exit")) {
                return;
            } else if (input.equals("help")) {
                System.out.println("Hello there! The programmer are still helping themselves.");
                // TODO: Some help messages
            } else {
                try {
                    QueryTree qt = QueryParser.parse(null, input); // null Node does not affect the string
                    processQuery(qt);
                } catch (UnableToParseException e) {
                    System.out.println("Wrong syntax. Type 'help' for help.");
                }
            }
        }
    }

    /**
     * Handles the given query. This will relay the request to all nodes and collect their responses. This method waits
     * for all nodes to finish their response before terminating.
     * @param queryTree query
     * @return
     */
    public Result processQuery(QueryTree queryTree){
        this.result = new Result(queryTree);
        List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < childrenIps.size(); i++){
            final String ip = childrenIps.get(i);
            final int port = childrenPorts.get(i);
            Thread t = new Thread(new NodeRequestWorker(ip, port, queryTree, new Function<String, Void>() {
                @Override
                public Void apply(String s) {
                    System.out.println(s);
                    result.merge(s);
                    return null;
                }
            }));
            t.start();
            workers.add(t);
        }
        workers.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        return this.result;
    }

    /**
     * The main function to start head node and accepting client's requests
     * @param args TODO
     */
    public static void main(String[] args) {
        // TODO: command line arguments for children ips/ports parse format: #.#.#.#:# using regex
        HeadNode head = new HeadNode();
        head.addChildNode(Global.LOCALHOST, 4444); //TODO: Just for test purpose
        try {
            head.getInput();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
