/**
 * HTTP server implementation providing a multi-threaded web server with servlet-based request handling.
 * 
 * <p>This package implements a custom HTTP server architecture that supports concurrent request processing
 * through a thread pool model. The server follows the servlet pattern for handling different types of
 * HTTP requests (GET, POST, DELETE) and uses longest-prefix matching for URL routing.</p>
 * 
 * <h2>Core Architecture</h2>
 * 
 * <p>The server architecture consists of three main components:</p>
 * <ul>
 *   <li><strong>HTTPServer Interface</strong> - Defines the contract for HTTP server implementations</li>
 *   <li><strong>MyHTTPServer Implementation</strong> - Concrete server with thread pool and servlet management</li>
 *   <li><strong>RequestParser Utility</strong> - Handles HTTP request parsing and parameter extraction</li>
 * </ul>
 * 
 * <h2>Threading Model</h2>
 * 
 * <p>The server uses a fixed-size thread pool to handle concurrent client connections:</p>
 * <ul>
 *   <li>Main server thread accepts incoming connections on a ServerSocket</li>
 *   <li>Each client connection is delegated to a worker thread from the pool</li>
 *   <li>Worker threads handle the complete request-response lifecycle</li>
 *   <li>Graceful shutdown ensures all threads are properly terminated</li>
 * </ul>
 * 
 * <h2>Servlet Pattern</h2>
 * 
 * <p>Request handling follows the servlet pattern with method-based routing:</p>
 * <ul>
 *   <li>Servlets are registered for specific HTTP methods and URI prefixes</li>
 *   <li>Longest-prefix matching algorithm selects the most specific servlet</li>
 *   <li>Each servlet handles the complete request processing and response generation</li>
 *   <li>Servlets are managed in separate concurrent maps for each HTTP method</li>
 * </ul>
 * 
 * <h2>Request Handling Lifecycle</h2>
 * 
 * <ol>
 *   <li><strong>Connection Accept</strong> - Server accepts incoming socket connection</li>
 *   <li><strong>Request Parsing</strong> - RequestParser extracts HTTP method, URI, parameters, and body</li>
 *   <li><strong>Servlet Selection</strong> - Longest-prefix matching finds appropriate servlet</li>
 *   <li><strong>Request Processing</strong> - Selected servlet processes request and generates response</li>
 *   <li><strong>Response Delivery</strong> - Response is written to client socket</li>
 *   <li><strong>Connection Cleanup</strong> - Socket and resources are properly closed</li>
 * </ol>
 * 
 * <h2>Key Classes</h2>
 * 
 * <dl>
 *   <dt>{@link server.HTTPServer}</dt>
 *   <dd>Interface defining the HTTP server contract with servlet management and lifecycle methods</dd>
 *   
 *   <dt>{@link server.MyHTTPServer}</dt>
 *   <dd>Concrete implementation providing multi-threaded HTTP server with servlet routing</dd>
 *   
 *   <dt>{@link server.RequestParser}</dt>
 *   <dd>Utility class for parsing HTTP requests and extracting request information</dd>
 *   
 *   <dt>{@link server.RequestParser.RequestInfo}</dt>
 *   <dd>Data structure encapsulating parsed HTTP request data including method, URI, parameters, and body</dd>
 * </dl>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * // Create server with port 8080 and 5 worker threads
 * MyHTTPServer server = new MyHTTPServer(8080, 5);
 * 
 * // Register servlets for different endpoints
 * server.addServlet("GET", "/api/status", new StatusServlet());
 * server.addServlet("POST", "/api/data", new DataServlet());
 * 
 * // Start the server
 * server.start();
 * 
 * // Server is now accepting connections...
 * 
 * // Graceful shutdown
 * server.close();
 * }</pre>
 * 
 * @since 1.0
 * @see servlets
 */
package server;