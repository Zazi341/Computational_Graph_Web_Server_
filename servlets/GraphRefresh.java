package servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import server.RequestParser.RequestInfo;
import views.HtmlGraphWriter;

/**
 * GraphRefresh servlet provides real-time graph visualization by serving updated HTML
 * with current topic values. This servlet generates fresh graph visualizations that
 * reflect the current state of all topics and agents in the computational graph system.
 * 
 * <p>The servlet is designed to be called periodically or on-demand to provide
 * up-to-date visualizations of the computational graph. It integrates with the
 * HtmlGraphWriter to generate SVG or Canvas-based graph representations that
 * include current topic values and agent relationships.
 * 
 * <h3>Supported HTTP Methods</h3>
 * <ul>
 * <li><strong>GET</strong> - Retrieve current graph visualization with live topic values</li>
 * </ul>
 * 
 * <h3>Request Format</h3>
 * <p>Simple GET request with no required parameters:
 * <pre>{@code
 * GET /graph HTTP/1.1
 * Host: localhost:8080
 * }</pre>
 * 
 * <h3>Response Format</h3>
 * <p>Returns HTML content with embedded graph visualization:
 * <ul>
 * <li><strong>Success (200 OK)</strong> - Complete HTML page with SVG/Canvas graph visualization</li>
 * <li><strong>Error (200 OK)</strong> - HTML error page with error message and navigation</li>
 * </ul>
 * 
 * <p>The HTML response includes:
 * <ul>
 * <li>Interactive graph visualization (SVG or Canvas)</li>
 * <li>Current topic values displayed on nodes</li>
 * <li>Agent relationships and data flow arrows</li>
 * <li>Styling and layout for optimal visualization</li>
 * </ul>
 * 
 * <h3>Graph Visualization Features</h3>
 * <p>The generated graph visualization includes:
 * <ul>
 * <li><strong>Topic Nodes</strong> - Circular nodes showing topic names and current values</li>
 * <li><strong>Agent Nodes</strong> - Rectangular nodes representing computational agents</li>
 * <li><strong>Data Flow Arrows</strong> - Directed edges showing message flow between topics and agents</li>
 * <li><strong>Real-time Values</strong> - Current topic values are fetched and displayed</li>
 * <li><strong>Interactive Elements</strong> - Clickable nodes and hover effects</li>
 * </ul>
 * 
 * <h3>Integration with System Components</h3>
 * <ul>
 * <li><strong>HtmlGraphWriter</strong> - Primary component for generating graph HTML</li>
 * <li><strong>TopicManagerSingleton</strong> - Source of current topic values and relationships</li>
 * <li><strong>Agent System</strong> - Provides agent information and computational graph structure</li>
 * </ul>
 * 
 * <h3>Use Cases</h3>
 * <ul>
 * <li>Real-time monitoring of computational graph state</li>
 * <li>Debugging agent interactions and message flow</li>
 * <li>Educational visualization of graph-based computations</li>
 * <li>System status monitoring and health checks</li>
 * </ul>
 * 
 * <h3>Performance Considerations</h3>
 * <p>The servlet generates fresh HTML on each request, which provides real-time accuracy
 * but may have performance implications for large graphs. The visualization is optimized
 * for typical graph sizes and includes efficient rendering strategies.
 * 
 * @see HtmlGraphWriter#getGraphHTML()
 * @see TopicManagerSingleton
 * @see Servlet#handle(RequestInfo, OutputStream)
 * 
 * @since 1.0
 */
public class GraphRefresh implements Servlet {

    /**
     * Handles HTTP GET requests to generate and serve real-time graph visualizations.
     * This method creates a fresh HTML representation of the current computational graph
     * state, including all topics, agents, and their current values.
     * 
     * <p>The method performs the following operations:
     * <ol>
     * <li>Invokes HtmlGraphWriter to generate current graph HTML</li>
     * <li>Retrieves real-time topic values from the system</li>
     * <li>Constructs complete HTML response with proper headers</li>
     * <li>Sends the visualization to the client</li>
     * </ol>
     * 
     * <p>The generated HTML includes:
     * <ul>
     * <li>SVG or Canvas-based graph visualization</li>
     * <li>Topic nodes with current values</li>
     * <li>Agent nodes showing computational components</li>
     * <li>Directed edges representing data flow</li>
     * <li>Interactive styling and layout</li>
     * </ul>
     * 
     * <h3>Error Handling</h3>
     * <p>If graph generation fails, the method returns an HTML error page
     * with the error message and navigation options. All exceptions are
     * caught and logged for debugging purposes.
     * 
     * @param ri the RequestInfo containing the HTTP request data (no parameters required)
     * @param toClient the OutputStream to write the HTML response to
     * @throws IOException if there are errors writing to the output stream
     * 
     * @see HtmlGraphWriter#getGraphHTML()
     * @see #sendHtmlResponse(OutputStream, String)
     * @see #sendErrorResponse(OutputStream, String)
     */
    @Override
    public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        System.out.println("[DEBUG] GraphRefresh invoked");

        try {
            // Generate fresh HTML with current topic values
            String graphHtml = HtmlGraphWriter.getGraphHTML();
            sendHtmlResponse(toClient, graphHtml);
            
            System.out.println("[DEBUG] Fresh graph HTML served with current topic values");

        } catch (Exception e) {
            System.err.println("[ERROR] GraphRefresh error: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(toClient, "Error generating graph: " + e.getMessage());
        }
    }

    /**
     * Sends an HTML response to the client with proper HTTP headers.
     * This method constructs a complete HTTP response with appropriate headers
     * including Content-Type and Content-Length for the graph visualization.
     * 
     * @param toClient the OutputStream to write the response to
     * @param htmlContent the HTML content containing the graph visualization
     * @throws IOException if there are errors writing to the output stream
     */
    private void sendHtmlResponse(OutputStream toClient, String htmlContent) throws IOException {
        byte[] contentBytes = htmlContent.getBytes(StandardCharsets.UTF_8);

        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: " + contentBytes.length + "\r\n" +
                "\r\n";

        toClient.write(response.getBytes(StandardCharsets.UTF_8));
        toClient.write(contentBytes);
        toClient.flush();
    }

    /**
     * Sends an HTML error response to the client with a user-friendly error message.
     * This method creates a simple HTML page containing the error message and a
     * back button for user navigation. The error message is HTML-escaped to prevent XSS.
     * 
     * @param toClient the OutputStream to write the error response to
     * @param message the error message to display to the user
     * @throws IOException if there are errors writing to the output stream
     * 
     * @see #escapeHtml(String)
     * @see #sendHtmlResponse(OutputStream, String)
     */
    private void sendErrorResponse(OutputStream toClient, String message) throws IOException {
        String html = "<html><body><h3>Graph Error</h3><p>" + escapeHtml(message) + "</p>" +
                "<a href='javascript:history.back()'>Go Back</a></body></html>";
        sendHtmlResponse(toClient, html);
    }

    /**
     * Escapes HTML special characters to prevent Cross-Site Scripting (XSS) attacks.
     * This method converts potentially dangerous characters in user input or error
     * messages to their HTML entity equivalents.
     * 
     * <p>The following characters are escaped:
     * <ul>
     * <li>{@code &} becomes {@code &amp;}</li>
     * <li>{@code <} becomes {@code &lt;}</li>
     * <li>{@code >} becomes {@code &gt;}</li>
     * <li>{@code "} becomes {@code &quot;}</li>
     * <li>{@code '} becomes {@code &#x27;}</li>
     * </ul>
     * 
     * @param text the text to escape, may be null
     * @return the escaped text safe for HTML output, or empty string if input is null
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    @Override
    public void close() throws IOException {
        System.out.println("[DEBUG] GraphRefresh closed");
    }
}