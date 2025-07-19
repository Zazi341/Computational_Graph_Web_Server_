package servlets;

import java.io.IOException;
import java.io.OutputStream;

import server.RequestParser.RequestInfo;

/**
 * Servlet defines the contract for HTTP request handlers in the servlet-based web server architecture.
 * 
 * <p>Servlets are lightweight, stateless components that process HTTP requests and generate responses.
 * They implement the Command pattern where each servlet handles a specific type of request or endpoint.
 * The HTTP server routes incoming requests to appropriate servlets based on URL patterns and HTTP methods.
 * 
 * <h3>Request Handling Pattern</h3>
 * <p>The servlet processing model follows this flow:
 * <ol>
 * <li><strong>Request Routing:</strong> HTTP server matches request URL to registered servlet</li>
 * <li><strong>Request Processing:</strong> Servlet's {@link #handle(RequestInfo, OutputStream)} method is called</li>
 * <li><strong>Response Generation:</strong> Servlet writes HTTP response to the output stream</li>
 * <li><strong>Resource Cleanup:</strong> Server ensures proper stream handling and connection cleanup</li>
 * </ol>
 * 
 * <h3>Thread Safety Considerations</h3>
 * <p>Servlet instances may be shared across multiple concurrent requests, so implementations must be thread-safe:
 * <ul>
 * <li>Avoid instance variables that store request-specific data</li>
 * <li>Use local variables for request processing state</li>
 * <li>Synchronize access to shared resources if necessary</li>
 * <li>Consider using thread-local storage for complex state management</li>
 * </ul>
 * 
 * <h3>HTTP Response Format</h3>
 * <p>Servlets must generate properly formatted HTTP responses including:
 * <ul>
 * <li><strong>Status Line:</strong> HTTP version, status code, and reason phrase</li>
 * <li><strong>Headers:</strong> Content-Type, Content-Length, and other relevant headers</li>
 * <li><strong>Body:</strong> The actual response content (HTML, JSON, binary data, etc.)</li>
 * </ul>
 * 
 * <h3>Common Response Patterns</h3>
 * <pre>{@code
 * // HTML Response
 * String html = "<html><body><h1>Hello World</h1></body></html>";
 * byte[] content = html.getBytes(StandardCharsets.UTF_8);
 * String response = "HTTP/1.1 200 OK\r\n" +
 *                  "Content-Type: text/html; charset=utf-8\r\n" +
 *                  "Content-Length: " + content.length + "\r\n" +
 *                  "\r\n";
 * toClient.write(response.getBytes(StandardCharsets.UTF_8));
 * toClient.write(content);
 * toClient.flush();
 * 
 * // JSON Response
 * String json = "{\"status\":\"success\",\"data\":\"value\"}";
 * byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
 * String jsonResponse = "HTTP/1.1 200 OK\r\n" +
 *                      "Content-Type: application/json\r\n" +
 *                      "Content-Length: " + jsonBytes.length + "\r\n" +
 *                      "\r\n";
 * toClient.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
 * toClient.write(jsonBytes);
 * toClient.flush();
 * 
 * // Error Response
 * String error = "HTTP/1.1 404 Not Found\r\n\r\nResource not found";
 * toClient.write(error.getBytes(StandardCharsets.UTF_8));
 * toClient.flush();
 * }</pre>
 * 
 * <h3>Usage Example</h3>
 * <pre>{@code
 * public class HelloServlet implements Servlet {
 *     public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
 *         // Extract request information
 *         String method = ri.getHttpCommand();
 *         String uri = ri.getUri();
 *         Map<String, String> params = ri.getParameters();
 *         
 *         // Process request based on method and parameters
 *         String responseContent;
 *         if ("GET".equals(method)) {
 *             String name = params.getOrDefault("name", "World");
 *             responseContent = "<html><body><h1>Hello " + escapeHtml(name) + "!</h1></body></html>";
 *         } else {
 *             responseContent = "<html><body><h1>Method not supported</h1></body></html>";
 *         }
 *         
 *         // Send HTTP response
 *         sendHtmlResponse(toClient, responseContent);
 *     }
 *     
 *     private void sendHtmlResponse(OutputStream toClient, String html) throws IOException {
 *         byte[] content = html.getBytes(StandardCharsets.UTF_8);
 *         String response = "HTTP/1.1 200 OK\r\n" +
 *                          "Content-Type: text/html; charset=utf-8\r\n" +
 *                          "Content-Length: " + content.length + "\r\n" +
 *                          "\r\n";
 *         toClient.write(response.getBytes(StandardCharsets.UTF_8));
 *         toClient.write(content);
 *         toClient.flush();
 *     }
 *     
 *     public void close() throws IOException {
 *         // Clean up resources if needed
 *     }
 * }
 * }</pre>
 * 
 * @see server.HTTPServer
 * @see server.RequestParser.RequestInfo
 * @since 1.0
 */
public interface Servlet {
    
    /**
     * Processes an HTTP request and generates an appropriate response.
     * 
     * <p>This method is the core of servlet functionality and is called by the HTTP server
     * for each request routed to this servlet. The implementation should:
     * <ol>
     * <li>Extract relevant information from the RequestInfo object</li>
     * <li>Perform the required processing (business logic, data retrieval, etc.)</li>
     * <li>Generate an appropriate HTTP response</li>
     * <li>Write the response to the OutputStream</li>
     * <li>Flush the output stream to ensure data is sent</li>
     * </ol>
     * 
     * <h3>Request Information Access</h3>
     * <p>The RequestInfo object provides access to:
     * <ul>
     * <li><strong>HTTP Method:</strong> {@code ri.getHttpCommand()} - GET, POST, DELETE, etc.</li>
     * <li><strong>URI:</strong> {@code ri.getUri()} - The full request URI including query parameters</li>
     * <li><strong>URI Segments:</strong> {@code ri.getUriSegments()} - Path components split by '/'</li>
     * <li><strong>Parameters:</strong> {@code ri.getParameters()} - Query parameters and form data</li>
     * <li><strong>Content:</strong> {@code ri.getContent()} - Request body for POST requests</li>
     * </ul>
     * 
     * <h3>Response Generation Guidelines</h3>
     * <ul>
     * <li><strong>Status Codes:</strong> Use appropriate HTTP status codes (200, 404, 500, etc.)</li>
     * <li><strong>Content-Type:</strong> Always specify the correct MIME type</li>
     * <li><strong>Content-Length:</strong> Include accurate content length for proper HTTP compliance</li>
     * <li><strong>Character Encoding:</strong> Use UTF-8 for text content and specify in Content-Type</li>
     * <li><strong>Security:</strong> Escape user input to prevent XSS attacks</li>
     * <li><strong>Caching:</strong> Set appropriate cache headers for static vs. dynamic content</li>
     * </ul>
     * 
     * <h3>Error Handling</h3>
     * <p>Servlets should handle errors gracefully:
     * <pre>{@code
     * public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
     *     try {
     *         // Process request
     *         String result = processRequest(ri);
     *         sendSuccessResponse(toClient, result);
     *     } catch (IllegalArgumentException e) {
     *         sendErrorResponse(toClient, 400, "Bad Request: " + e.getMessage());
     *     } catch (Exception e) {
     *         sendErrorResponse(toClient, 500, "Internal Server Error");
     *         e.printStackTrace(); // Log for debugging
     *     }
     * }
     * }</pre>
     * 
     * <h3>Performance Considerations</h3>
     * <ul>
     * <li>Keep processing time minimal to avoid blocking the server thread pool</li>
     * <li>Use buffered I/O for large responses</li>
     * <li>Avoid creating unnecessary objects in the request path</li>
     * <li>Consider streaming responses for large data sets</li>
     * <li>Implement proper connection handling and timeouts</li>
     * </ul>
     * 
     * @param ri the parsed HTTP request information containing method, URI, parameters,
     *           headers, and body content. Never null.
     * @param toClient the output stream to write the HTTP response to. The servlet
     *                 must write a complete HTTP response including status line,
     *                 headers, and body. Never null.
     * 
     * @throws IOException if an I/O error occurs while reading the request or writing the response.
     *                     This typically indicates network issues or client disconnection.
     * 
     * @see RequestInfo
     * @see #close()
     */
    void handle(RequestInfo ri, OutputStream toClient) throws IOException;
    
    /**
     * Releases any resources held by this servlet and performs cleanup operations.
     * 
     * <p>This method is called during server shutdown or when the servlet is being
     * unregistered from the HTTP server. Implementations should:
     * <ul>
     * <li>Close any open files, database connections, or network resources</li>
     * <li>Stop any background threads or scheduled tasks</li>
     * <li>Release any cached data or temporary resources</li>
     * <li>Perform any necessary state persistence</li>
     * </ul>
     * 
     * <p>For stateless servlets that don't hold external resources, this method
     * may be a no-op but should still be implemented for consistency.
     * 
     * <p>This method should be idempotent - calling it multiple times should not
     * cause errors or unexpected behavior. It should also complete quickly to
     * avoid delaying server shutdown.
     * 
     * <h3>Resource Cleanup Example</h3>
     * <pre>{@code
     * public void close() throws IOException {
     *     // Close database connections
     *     if (dataSource != null) {
     *         try {
     *             dataSource.close();
     *         } catch (SQLException e) {
     *             // Log error but don't throw - cleanup should continue
     *             System.err.println("Error closing database: " + e.getMessage());
     *         }
     *     }
     *     
     *     // Stop background tasks
     *     if (scheduledExecutor != null) {
     *         scheduledExecutor.shutdown();
     *         try {
     *             if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
     *                 scheduledExecutor.shutdownNow();
     *             }
     *         } catch (InterruptedException e) {
     *             scheduledExecutor.shutdownNow();
     *             Thread.currentThread().interrupt();
     *         }
     *     }
     *     
     *     // Clear caches
     *     if (cache != null) {
     *         cache.clear();
     *     }
     *     
     *     // Clear references
     *     dataSource = null;
     *     scheduledExecutor = null;
     *     cache = null;
     * }
     * }</pre>
     * 
     * <h3>Cleanup Best Practices</h3>
     * <ul>
     * <li>Don't throw exceptions from close() - log errors instead</li>
     * <li>Use try-catch blocks around individual cleanup operations</li>
     * <li>Set object references to null after cleanup</li>
     * <li>Use timeouts for operations that might hang</li>
     * <li>Consider using try-with-resources for automatic cleanup</li>
     * </ul>
     * 
     * @throws IOException if an I/O error occurs during resource cleanup.
     *                     Implementations should minimize the likelihood of this
     *                     by handling individual cleanup failures gracefully.
     * 
     * @see #handle(RequestInfo, OutputStream)
     */
    void close() throws IOException;
}
