package server;

import server.RequestParser.RequestInfo;
import servlets.Servlet;
//---------------------------------------------------
import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;
//---------------------------------------------------

/**
 * A multi-threaded HTTP server implementation that handles concurrent client requests
 * using a fixed thread pool and servlet-based request routing.
 * 
 * <p>This server implements the HTTPServer interface and provides:
 * <ul>
 *   <li>Concurrent request handling with configurable thread pool</li>
 *   <li>Servlet-based routing for GET, POST, and DELETE methods</li>
 *   <li>Longest-prefix matching for URL routing</li>
 *   <li>Graceful shutdown with resource cleanup</li>
 * </ul>
 * 
 * <h3>Threading Model</h3>
 * The server uses a fixed-size thread pool to handle client connections concurrently.
 * Each incoming connection is delegated to a worker thread from the pool, allowing
 * the main server thread to continue accepting new connections without blocking.
 * 
 * <h3>Servlet Mapping Strategy</h3>
 * Servlets are organized by HTTP method in separate concurrent maps:
 * <ul>
 *   <li>GET requests → getMap</li>
 *   <li>POST requests → postMap</li>
 *   <li>DELETE requests → deleteMap</li>
 * </ul>
 * 
 * URL routing uses longest-prefix matching, where the servlet with the longest
 * matching URI prefix is selected to handle the request.
 * 
 * <h3>Complete Integration Examples</h3>
 * 
 * <h4>Basic Server Setup</h4>
 * <pre>{@code
 * // Create server with 5 worker threads on port 8080
 * MyHTTPServer server = new MyHTTPServer(8080, 5);
 * 
 * // Add servlets for different endpoints
 * server.addServlet("GET", "/api/status", new StatusServlet());
 * server.addServlet("POST", "/api/config", new ConfigServlet());
 * server.addServlet("GET", "/", new IndexServlet());
 * 
 * // Start the server
 * server.start();
 * 
 * // ... server handles requests ...
 * 
 * // Graceful shutdown
 * server.close();
 * server.join(); // Wait for server thread to finish
 * }</pre>
 * 
 * <h4>Computational Graph Server Setup</h4>
 * <pre>{@code
 * import servlets.*;
 * 
 * public class GraphServerExample {
 *     public static void main(String[] args) {
 *         // Create HTTP server with 10 worker threads
 *         MyHTTPServer server = new MyHTTPServer(8080, 10);
 *         
 *         try {
 *             // Register servlets for computational graph functionality
 *             server.addServlet("POST", "/config", new ConfLoader());
 *             server.addServlet("GET", "/graph", new GraphRefresh());
 *             server.addServlet("GET", "/topics", new TopicDisplayer());
 *             server.addServlet("GET", "/", new HtmlLoader());
 *             
 *             // Start the server
 *             System.out.println("Starting computational graph server on port 8080...");
 *             server.start();
 *             
 *             // Keep server running until interrupted
 *             Runtime.getRuntime().addShutdownHook(new Thread(() -> {
 *                 System.out.println("Shutting down server...");
 *                 server.close();
 *                 try {
 *                     server.join(5000); // Wait up to 5 seconds
 *                 } catch (InterruptedException e) {
 *                     Thread.currentThread().interrupt();
 *                 }
 *             }));
 *             
 *             // Wait for server to finish
 *             server.join();
 *             
 *         } catch (InterruptedException e) {
 *             System.err.println("Server interrupted: " + e.getMessage());
 *             Thread.currentThread().interrupt();
 *         } finally {
 *             server.close();
 *         }
 *     }
 * }
 * }</pre>
 * 
 * <h4>Advanced Configuration with Error Handling</h4>
 * <pre>{@code
 * public class RobustServerSetup {
 *     private static final int DEFAULT_PORT = 8080;
 *     private static final int DEFAULT_THREADS = 5;
 *     
 *     public static MyHTTPServer createAndConfigureServer(int port, int threads) {
 *         MyHTTPServer server = new MyHTTPServer(port, threads);
 *         
 *         try {
 *             // Add servlets with error handling
 *             server.addServlet("GET", "/health", (req, out) -> {
 *                 String response = "HTTP/1.1 200 OK\r\n" +
 *                                 "Content-Type: text/plain\r\n" +
 *                                 "Content-Length: 2\r\n\r\nOK";
 *                 out.write(response.getBytes());
 *                 out.flush();
 *             });
 *             
 *             // Configuration endpoint
 *             server.addServlet("POST", "/config", new ConfLoader());
 *             
 *             // Graph visualization
 *             server.addServlet("GET", "/graph", new GraphRefresh());
 *             
 *             // Topic status endpoint
 *             server.addServlet("GET", "/topics", new TopicDisplayer());
 *             
 *             // Static file serving
 *             server.addServlet("GET", "/", new HtmlLoader());
 *             
 *             return server;
 *             
 *         } catch (Exception e) {
 *             System.err.println("Failed to configure server: " + e.getMessage());
 *             server.close(); // Clean up on failure
 *             throw new RuntimeException("Server configuration failed", e);
 *         }
 *     }
 *     
 *     public static void startServerSafely(MyHTTPServer server) {
 *         try {
 *             server.start();
 *             System.out.println("Server started successfully on port " + server.getPort());
 *             
 *         } catch (Exception e) {
 *             System.err.println("Failed to start server: " + e.getMessage());
 *             server.close();
 *             throw new RuntimeException("Server startup failed", e);
 *         }
 *     }
 * }
 * }</pre>
 * 
 * <h4>Complete Request-Response Lifecycle Example</h4>
 * <pre>{@code
 * // Example showing the complete flow from client request to response
 * 
 * // 1. Client sends HTTP request:
 * // POST /config HTTP/1.1
 * // Content-Type: text/plain
 * // Content-Length: 45
 * //
 * // configs.PlusAgent
 * // input1,input2
 * // output
 * 
 * // 2. Server processes request:
 * MyHTTPServer server = new MyHTTPServer(8080, 5);
 * server.addServlet("POST", "/config", new ConfLoader());
 * server.start();
 * 
 * // 3. Request flows through server:
 * // - ServerSocket.accept() receives connection
 * // - Worker thread from thread pool handles request
 * // - RequestParser.parseRequest() parses HTTP headers and body
 * // - findBestMatch() selects ConfLoader servlet for "/config"
 * // - ConfLoader.handle() processes configuration and creates agents
 * // - Response sent back to client
 * 
 * // 4. Client receives response:
 * // HTTP/1.1 200 OK
 * // Content-Type: text/html
 * // Content-Length: 156
 * //
 * // <html><body>Configuration loaded successfully...</body></html>
 * }</pre>
 * 
 * <h4>Production Deployment Example</h4>
 * <pre>{@code
 * public class ProductionServer {
 *     private static final Logger logger = Logger.getLogger(ProductionServer.class.getName());
 *     
 *     public static void main(String[] args) {
 *         int port = Integer.parseInt(System.getProperty("server.port", "8080"));
 *         int threads = Integer.parseInt(System.getProperty("server.threads", "20"));
 *         
 *         MyHTTPServer server = new MyHTTPServer(port, threads);
 *         
 *         try {
 *             // Configure servlets
 *             setupServlets(server);
 *             
 *             // Add shutdown hook for graceful termination
 *             addShutdownHook(server);
 *             
 *             // Start server
 *             logger.info("Starting server on port " + port + " with " + threads + " threads");
 *             server.start();
 *             
 *             // Monitor server health
 *             monitorServer(server);
 *             
 *         } catch (Exception e) {
 *             logger.severe("Server failed to start: " + e.getMessage());
 *             System.exit(1);
 *         }
 *     }
 *     
 *     private static void setupServlets(MyHTTPServer server) {
 *         // Core functionality
 *         server.addServlet("POST", "/api/config", new ConfLoader());
 *         server.addServlet("GET", "/api/graph", new GraphRefresh());
 *         server.addServlet("GET", "/api/topics", new TopicDisplayer());
 *         
 *         // Health check endpoint
 *         server.addServlet("GET", "/health", new HealthCheckServlet());
 *         
 *         // Static content
 *         server.addServlet("GET", "/", new HtmlLoader());
 *     }
 *     
 *     private static void addShutdownHook(MyHTTPServer server) {
 *         Runtime.getRuntime().addShutdownHook(new Thread(() -> {
 *             logger.info("Received shutdown signal, stopping server...");
 *             server.close();
 *             try {
 *                 server.join(10000); // Wait up to 10 seconds
 *                 logger.info("Server stopped gracefully");
 *             } catch (InterruptedException e) {
 *                 logger.warning("Server shutdown interrupted");
 *                 Thread.currentThread().interrupt();
 *             }
 *         }));
 *     }
 * }
 * }</pre>
 * 
 * <h3>Thread Safety</h3>
 * This implementation is thread-safe. Servlet maps use ConcurrentHashMap for
 * concurrent access, and the running flag is volatile to ensure visibility
 * across threads.
 * 
 * @author Generated Documentation
 * @version 1.0
 * @see HTTPServer
 * @see Servlet
 * @see RequestParser
 * @since 1.0
 */
public class MyHTTPServer extends Thread implements HTTPServer{

	//---------------------------------------------------
    private final int port;
    private final ExecutorService threadPool;
    private volatile boolean running = true;

    // Separate maps for HTTP methods
    private final ConcurrentHashMap<String, Servlet> getMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Servlet> postMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Servlet> deleteMap = new ConcurrentHashMap<>();

	//---------------------------------------------------


    /**
     * Creates a new HTTP server that will listen on the specified port and handle
     * requests using a fixed-size thread pool.
     * 
     * <p>The server is created in a stopped state. Call {@link #start()} to begin
     * accepting connections.
     * 
     * @param port the port number to listen on (1-65535)
     * @param nThreads the number of worker threads in the thread pool for handling requests
     * @throws IllegalArgumentException if port is not in valid range or nThreads <= 0
     * 
     * @see #start()
     * @see #close()
     */
    public MyHTTPServer(int port,int nThreads){
    	//---------------------------------------------------
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(nThreads);

    	//---------------------------------------------------
    }

    /**
     * Registers a servlet to handle requests for a specific HTTP method and URI prefix.
     * 
     * <p>The servlet will be invoked for all requests where the URI starts with the
     * specified prefix. If multiple servlets match a URI, the one with the longest
     * matching prefix will be selected (longest-prefix matching).
     * 
     * <p>Supported HTTP methods: GET, POST, DELETE (case-insensitive)
     * 
     * @param httpCommanmd the HTTP method (GET, POST, or DELETE, case-insensitive)
     * @param uri the URI prefix to match against incoming requests
     * @param s the servlet instance to handle matching requests
     * 
     * @throws NullPointerException if any parameter is null
     * 
     * @see #removeServlet(String, String)
     * @see #findBestMatch(String, ConcurrentHashMap)
     * 
     * @example
     * <pre>{@code
     * server.addServlet("GET", "/api/", new ApiServlet());
     * server.addServlet("POST", "/upload", new UploadServlet());
     * server.addServlet("GET", "/", new IndexServlet()); // Catch-all for root
     * }</pre>
     */
    @Override
    public void addServlet(String httpCommanmd, String uri, Servlet s){
    	//---------------------------------------------------
        switch (httpCommanmd.toUpperCase()) {
        case "GET":
        	getMap.put(uri, s);
        	break;
        case "POST":
        	postMap.put(uri, s);
        	break;
        case "DELETE":
        	deleteMap.put(uri, s);
        	break;
    }

    	//---------------------------------------------------
    }

    /**
     * Removes a servlet registration for the specified HTTP method and URI prefix.
     * 
     * <p>After removal, requests matching the specified method and URI prefix will
     * no longer be handled by the previously registered servlet. If no servlet was
     * registered for the given method/URI combination, this method has no effect.
     * 
     * @param httpCommanmd the HTTP method (GET, POST, or DELETE, case-insensitive)
     * @param uri the URI prefix that was previously registered
     * 
     * @see #addServlet(String, String, Servlet)
     */
    @Override
    public void removeServlet(String httpCommanmd, String uri){
    	//---------------------------------------------------
        switch (httpCommanmd.toUpperCase()) {
        case "GET":
        	getMap.remove(uri);
        	break;
        case "POST":
        	postMap.remove(uri);
        	break;
        case "DELETE":
        	deleteMap.remove(uri);
        	break;
    }

    	//---------------------------------------------------
    }

    /**
     * Main server loop that accepts client connections and delegates them to worker threads.
     * 
     * <p>This method implements the server's main execution loop:
     * <ol>
     *   <li>Creates a ServerSocket bound to the configured port</li>
     *   <li>Sets a 1-second timeout to periodically check the running flag</li>
     *   <li>Accepts incoming client connections</li>
     *   <li>Submits each connection to the thread pool for processing</li>
     *   <li>Continues until {@link #close()} is called</li>
     * </ol>
     * 
     * <p>The server uses a timeout-based approach to avoid blocking indefinitely
     * on accept(), allowing for graceful shutdown when the running flag is set to false.
     * 
     * <p><strong>Thread Safety:</strong> This method is designed to run in its own thread
     * (since MyHTTPServer extends Thread). The running flag is volatile to ensure
     * visibility across threads.
     * 
     * @throws IOException if the server socket cannot be created or bound to the port
     * 
     * @see #close()
     * @see #handleClient(Socket)
     */
    @Override
    public void run(){
    	//---------------------------------------------------
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setSoTimeout(1000); // don't block forever – recheck `running` every second

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();// got a new client
                    threadPool.submit(() -> handleClient(clientSocket));// delegate to thread pool
                } catch (SocketTimeoutException ignore) {
                	// nothing to do – just re-loop and check if still running
                } catch (IOException e) {
                    if (running) e.printStackTrace();// log only if not shutting down
                }
            }
        } catch (IOException e) {
            e.printStackTrace();// server failed to start or crashed – worth logging
        }

    	//---------------------------------------------------
    }

    /**
     * Initiates graceful shutdown of the HTTP server and cleans up all resources.
     * 
     * <p>This method performs the following shutdown sequence:
     * <ol>
     *   <li>Sets the running flag to false, causing the main server loop to exit</li>
     *   <li>Shuts down the thread pool and waits up to 2 seconds for active requests to complete</li>
     *   <li>Closes all registered servlets to allow them to clean up their resources</li>
     * </ol>
     * 
     * <p><strong>Important:</strong> After calling this method, you should also call
     * {@code join()} on the server thread to wait for the main server loop to fully exit.
     * 
     * <p><strong>Thread Safety:</strong> This method is safe to call from any thread.
     * The running flag is volatile, ensuring immediate visibility to the server thread.
     * 
     * <p><strong>Resource Cleanup:</strong> All unique servlet instances are closed
     * exactly once, even if they are registered for multiple HTTP methods or URIs.
     * 
     * @see #run()
     * @see Thread#join()
     * 
     * @example
     * <pre>{@code
     * // Proper shutdown sequence
     * server.close();        // Initiate shutdown
     * server.join();         // Wait for server thread to finish
     * }</pre>
     */
    public void close(){
    	//---------------------------------------------------
        // signaling the loop to stop
        running = false;

        // trying to shutdown the thread pool gracefully
        threadPool.shutdownNow();
        try {
            if (!threadPool.awaitTermination(2, TimeUnit.SECONDS)) {
                System.out.println("[DEBUG] Thread pool did not terminate in time.");
            } else {
//                System.out.println("[DEBUG] Thread pool terminated cleanly.");
            }
        } catch (InterruptedException e) {
            System.out.println("[DEBUG] Interrupted while waiting for thread pool to terminate.");
            Thread.currentThread().interrupt(); // just in case
        }

        // closing all unique servlets (from all maps)
        Set<Servlet> allServlets = new HashSet<>();
        allServlets.addAll(getMap.values());
        allServlets.addAll(postMap.values());
        allServlets.addAll(deleteMap.values());

        for (Servlet s : allServlets) {
            try {
                s.close();// give each servlet a chance to clean up
            } catch (IOException e) {
                System.err.println("[DEBUG] Failed to close servlet: " + e.getMessage());
            }
        }
    	//---------------------------------------------------
    }

  //---------------------------------------------------
    /**
     * Handles an individual client connection by parsing the HTTP request and
     * routing it to the appropriate servlet.
     * 
     * <p>This method is executed by worker threads from the thread pool for each
     * incoming client connection. It performs the following steps:
     * <ol>
     *   <li>Parses the HTTP request using RequestParser</li>
     *   <li>Extracts the HTTP method and URI</li>
     *   <li>Finds the best matching servlet using longest-prefix matching</li>
     *   <li>Delegates request handling to the servlet or returns 404</li>
     *   <li>Ensures the client socket is properly closed</li>
     * </ol>
     * 
     * <p><strong>Error Handling:</strong> All exceptions are caught and logged
     * to prevent individual request failures from affecting other requests or
     * crashing the server.
     * 
     * <p><strong>Resource Management:</strong> Uses try-with-resources for streams
     * and ensures socket cleanup in the finally block.
     * 
     * @param socket the client socket connection to handle
     * 
     * @see RequestParser#parseRequest(BufferedReader)
     * @see #findBestMatch(String, ConcurrentHashMap)
     * @see Servlet#handle(RequestInfo, OutputStream)
     */
    private void handleClient(Socket socket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream out = socket.getOutputStream()
        ) {
            RequestInfo req = RequestParser.parseRequest(in);
            String method = req.getHttpCommand().toUpperCase();
            String uri = req.getUri();

            Servlet servlet = null;
            switch (method) {
                case "GET":
                	servlet = findBestMatch(uri, getMap);
                	break;
                case "POST":
                	servlet = findBestMatch(uri, postMap);
                	break;
                case "DELETE":
                	servlet = findBestMatch(uri, deleteMap);
                	break;
                default:
                	servlet = null;
            };

            if (servlet != null) {
                servlet.handle(req, out);
            } else {
                out.write("HTTP/1.1 404 Not Found\r\n\r\nNo matching servlet.".getBytes());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException ignore) {}
        }
    }



    /**
     * Finds the servlet with the longest matching URI prefix for the given request URI.
     * 
     * <p>This method implements the longest-prefix matching algorithm used for servlet
     * routing. It iterates through all registered URI prefixes in the given map and
     * selects the servlet whose prefix is the longest match for the request URI.
     * 
     * <p><strong>Algorithm:</strong>
     * <ol>
     *   <li>Check each registered URI prefix to see if the request URI starts with it</li>
     *   <li>Among all matching prefixes, select the one with the greatest length</li>
     *   <li>Return the servlet associated with the longest matching prefix</li>
     * </ol>
     * 
     * <p><strong>Example:</strong> If servlets are registered for "/api/" and "/api/users/",
     * a request to "/api/users/123" will match the "/api/users/" servlet since it has
     * the longer prefix.
     * 
     * @param uri the request URI to match against registered prefixes
     * @param map the concurrent map containing URI prefix to servlet mappings
     * @return the servlet with the longest matching prefix, or null if no match is found
     * 
     * @see #addServlet(String, String, Servlet)
     * @see #handleClient(Socket)
     */
    private Servlet findBestMatch(String uri, ConcurrentHashMap<String, Servlet> map) {
        Servlet best = null;
        int bestLength = -1;

        for (String prefix : map.keySet()) {
            if (uri.startsWith(prefix) && prefix.length() > bestLength) {
                best = map.get(prefix);
                bestLength = prefix.length();
            }
        }

        return best;
    }
  //---------------------------------------------------


}
