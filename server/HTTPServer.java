package server;

import servlets.Servlet;

/**
 * HTTPServer defines the contract for a multi-threaded HTTP server that supports
 * servlet-based request handling with URL routing and concurrent client processing.
 * 
 * <p>This interface provides a servlet container pattern where different servlets
 * can be registered to handle specific HTTP methods (GET, POST, DELETE) and URI patterns.
 * The server uses longest-prefix matching for URL routing and maintains separate
 * servlet mappings for each HTTP method.
 * 
 * <h3>Threading Model</h3>
 * <p>Implementations should use a thread pool to handle concurrent client requests.
 * The server runs on a dedicated thread (implements Runnable) and delegates client
 * handling to worker threads from the pool. This allows multiple clients to be
 * served simultaneously without blocking.
 * 
 * <h3>Servlet Registration</h3>
 * <p>Servlets are registered with specific HTTP methods and URI prefixes. When a
 * request arrives, the server finds the servlet with the longest matching URI prefix
 * for the requested method and delegates the request handling to that servlet.
 * 
 * <h3>Server Lifecycle</h3>
 * <ol>
 * <li>Create server instance with port and thread pool configuration</li>
 * <li>Register servlets using {@link #addServlet(String, String, Servlet)}</li>
 * <li>Start the server with {@link #start()}</li>
 * <li>Server runs until {@link #close()} is called</li>
 * <li>Graceful shutdown includes thread pool termination and servlet cleanup</li>
 * </ol>
 * 
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Create and configure HTTP server
 * HTTPServer server = new MyHTTPServer(8080, 5);
 * 
 * // Register servlets for different endpoints
 * server.addServlet("GET", "/api/status", new StatusServlet());
 * server.addServlet("POST", "/api/config", new ConfigServlet());
 * server.addServlet("DELETE", "/api/reset", new ResetServlet());
 * 
 * // Start the server (non-blocking)
 * server.start();
 * 
 * // Server is now accepting requests...
 * 
 * // Graceful shutdown
 * server.close();
 * }</pre>
 * 
 * @see Servlet
 * @see server.MyHTTPServer
 * @since 1.0
 */
public interface HTTPServer extends Runnable{
    
    /**
     * Registers a servlet to handle requests for a specific HTTP method and URI prefix.
     * 
     * <p>The server uses longest-prefix matching to route requests. When multiple
     * servlets match a URI, the one with the longest matching prefix is selected.
     * This allows for hierarchical URL structures and flexible routing patterns.
     * 
     * <p>Servlets are stored in separate maps for each HTTP method, allowing the
     * same URI prefix to be handled by different servlets depending on the request method.
     * 
     * @param httpCommanmd the HTTP method (case-insensitive). Supported values are:
     *                     "GET", "POST", "DELETE". Other methods are ignored.
     * @param uri the URI prefix to match against incoming requests. Must not be null.
     *            Examples: "/api", "/config", "/status"
     * @param s the servlet instance to handle matching requests. Must not be null.
     *          The servlet's {@link Servlet#handle(server.RequestParser.RequestInfo, java.io.OutputStream)}
     *          method will be called for each matching request.
     * 
     * @throws NullPointerException if uri or s is null
     * 
     * @see #removeServlet(String, String)
     * @see Servlet#handle(server.RequestParser.RequestInfo, java.io.OutputStream)
     */
    public void addServlet(String httpCommanmd, String uri, Servlet s);
    
    /**
     * Removes a previously registered servlet for the specified HTTP method and URI prefix.
     * 
     * <p>This method removes the servlet mapping but does not call {@link Servlet#close()}
     * on the servlet instance. Servlet cleanup should be handled separately or during
     * server shutdown via {@link #close()}.
     * 
     * <p>If no servlet is registered for the given method and URI combination,
     * this method has no effect and does not throw an exception.
     * 
     * @param httpCommanmd the HTTP method (case-insensitive). Supported values are:
     *                     "GET", "POST", "DELETE". Other methods are ignored.
     * @param uri the URI prefix that was used during servlet registration.
     *            Must match exactly the URI used in {@link #addServlet(String, String, Servlet)}.
     * 
     * @see #addServlet(String, String, Servlet)
     */
    public void removeServlet(String httpCommanmd, String uri);
    
    /**
     * Starts the HTTP server in a new thread to begin accepting client connections.
     * 
     * <p>This method is non-blocking and returns immediately after starting the server thread.
     * The server will continue running until {@link #close()} is called or the application
     * terminates.
     * 
     * <p>The server binds to the port specified during construction and begins listening
     * for incoming HTTP connections. Each client connection is handled by a worker thread
     * from the configured thread pool.
     * 
     * <h3>Server Socket Configuration</h3>
     * <ul>
     * <li>Uses a timeout to periodically check the running flag</li>
     * <li>Handles SocketTimeoutException gracefully during shutdown</li>
     * <li>Logs IOException only when not shutting down</li>
     * </ul>
     * 
     * @throws RuntimeException if the server fails to bind to the specified port
     *                         or encounters a fatal error during startup
     * 
     * @see #close()
     * @see Thread#start()
     */
    public void start();
    
    /**
     * Initiates graceful shutdown of the HTTP server and cleans up all resources.
     * 
     * <p>The shutdown process includes:
     * <ol>
     * <li>Setting the running flag to false to stop accepting new connections</li>
     * <li>Shutting down the thread pool and waiting for active requests to complete</li>
     * <li>Calling {@link Servlet#close()} on all registered servlets</li>
     * <li>Closing the server socket</li>
     * </ol>
     * 
     * <p>This method blocks until the shutdown process completes or times out.
     * The thread pool is given a reasonable amount of time to terminate gracefully
     * before being forcibly shut down.
     * 
     * <h3>Thread Pool Shutdown</h3>
     * <p>The method calls {@code shutdownNow()} on the thread pool and waits up to
     * 2 seconds for termination. If threads don't terminate within this time,
     * a debug message is logged but the shutdown continues.
     * 
     * <h3>Servlet Cleanup</h3>
     * <p>All unique servlet instances (across GET, POST, DELETE mappings) are
     * identified and their {@code close()} methods are called. Exceptions during
     * servlet cleanup are logged but don't prevent the shutdown from completing.
     * 
     * @see Servlet#close()
     * @see java.util.concurrent.ExecutorService#shutdownNow()
     */
    public void close();
}
