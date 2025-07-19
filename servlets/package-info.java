/**
 * Web servlet implementations providing HTTP endpoints for computational graph management and visualization.
 * 
 * <p>This package contains servlet implementations that handle HTTP requests for the computational graph
 * web server. Each servlet provides specific functionality for configuration management, graph visualization,
 * and topic interaction through a RESTful web interface.</p>
 * 
 * <h2>Servlet Interface</h2>
 * 
 * <p>All servlets implement the {@link servlets.Servlet} interface which defines:</p>
 * <ul>
 *   <li><strong>Request Handling</strong> - Process HTTP requests with RequestInfo and OutputStream</li>
 *   <li><strong>Resource Management</strong> - Proper cleanup through the close() method</li>
 *   <li><strong>Error Handling</strong> - Graceful error responses with appropriate HTTP status codes</li>
 *   <li><strong>Content Generation</strong> - Dynamic HTML, JSON, or other content types</li>
 * </ul>
 * 
 * <h2>Web Endpoints</h2>
 * 
 * <p>The package provides several key web endpoints:</p>
 * <dl>
 *   <dt><strong>POST /config</strong> - {@link servlets.ConfLoader}</dt>
 *   <dd>Handles configuration file uploads and creates computational graphs</dd>
 *   
 *   <dt><strong>GET /graph</strong> - {@link servlets.GraphRefresh}</dt>
 *   <dd>Serves updated graph visualizations with current topic values</dd>
 *   
 *   <dt><strong>GET /topics</strong> - {@link servlets.TopicDisplayer}</dt>
 *   <dd>Displays topic status table and handles message publishing</dd>
 * </dl>
 * 
 * <h2>URL Routing and Response Handling</h2>
 * 
 * <p>The servlet system uses longest-prefix matching for URL routing:</p>
 * <ul>
 *   <li><strong>Method-based Routing</strong> - Different servlets for GET, POST, DELETE methods</li>
 *   <li><strong>Prefix Matching</strong> - URLs are matched against registered servlet prefixes</li>
 *   <li><strong>Parameter Extraction</strong> - Query parameters and form data are parsed automatically</li>
 *   <li><strong>Content Negotiation</strong> - Appropriate Content-Type headers for different response types</li>
 * </ul>
 * 
 * <h2>Request Processing Lifecycle</h2>
 * 
 * <ol>
 *   <li><strong>Request Reception</strong> - HTTP server receives and parses the request</li>
 *   <li><strong>Servlet Selection</strong> - Longest-prefix matching selects appropriate servlet</li>
 *   <li><strong>Parameter Extraction</strong> - Query parameters, form data, and content are extracted</li>
 *   <li><strong>Business Logic</strong> - Servlet processes the request and interacts with the graph system</li>
 *   <li><strong>Response Generation</strong> - Dynamic content is generated (HTML, JSON, etc.)</li>
 *   <li><strong>Response Delivery</strong> - HTTP response with appropriate headers is sent to client</li>
 * </ol>
 * 
 * <h2>Key Classes</h2>
 * 
 * <dl>
 *   <dt>{@link servlets.Servlet}</dt>
 *   <dd>Interface defining the contract for HTTP request handlers with lifecycle management</dd>
 *   
 *   <dt>{@link servlets.ConfLoader}</dt>
 *   <dd>Handles multipart form uploads for configuration files and creates computational graphs</dd>
 *   
 *   <dt>{@link servlets.GraphRefresh}</dt>
 *   <dd>Generates and serves updated HTML visualizations of the computational graph</dd>
 *   
 *   <dt>{@link servlets.TopicDisplayer}</dt>
 *   <dd>Provides topic status display and handles message publishing to input topics</dd>
 *   
 *   <dt>{@link servlets.HtmlLoader}</dt>
 *   <dd>Serves static HTML files and handles basic file serving functionality</dd>
 * </dl>
 * 
 * <h2>Configuration Upload Process</h2>
 * 
 * <p>The ConfLoader servlet handles the complete configuration lifecycle:</p>
 * <ol>
 *   <li><strong>Multipart Parsing</strong> - Extracts filename and content from form data</li>
 *   <li><strong>File Validation</strong> - Validates configuration file format and structure</li>
 *   <li><strong>Topic Cleanup</strong> - Clears existing topics and agent connections</li>
 *   <li><strong>Agent Creation</strong> - Uses GenericConfig to instantiate new agents</li>
 *   <li><strong>Graph Generation</strong> - Creates updated visualization with new configuration</li>
 *   <li><strong>Response Delivery</strong> - Returns HTML visualization to the client</li>
 * </ol>
 * 
 * <h2>Graph Visualization</h2>
 * 
 * <p>The GraphRefresh servlet provides real-time graph visualization:</p>
 * <ul>
 *   <li><strong>Dynamic Content</strong> - Generates HTML with current topic values</li>
 *   <li><strong>SVG Graphics</strong> - Uses HtmlGraphWriter for scalable vector graphics</li>
 *   <li><strong>Interactive Elements</strong> - Clickable nodes and edges for exploration</li>
 *   <li><strong>Real-time Updates</strong> - Shows current message values in topics</li>
 * </ul>
 * 
 * <h2>Topic Management</h2>
 * 
 * <p>The TopicDisplayer servlet provides comprehensive topic interaction:</p>
 * <ul>
 *   <li><strong>Status Display</strong> - HTML table showing all topics and their current values</li>
 *   <li><strong>Message Publishing</strong> - Allows publishing messages to input-only topics</li>
 *   <li><strong>Access Control</strong> - Prevents publishing to intermediate or output topics</li>
 *   <li><strong>Auto-refresh</strong> - Automatic updates when configuration changes</li>
 * </ul>
 * 
 * <h2>Error Handling</h2>
 * 
 * <p>All servlets implement comprehensive error handling:</p>
 * <ul>
 *   <li><strong>Input Validation</strong> - Validates request parameters and content</li>
 *   <li><strong>Exception Handling</strong> - Catches and logs all exceptions gracefully</li>
 *   <li><strong>User-friendly Errors</strong> - Returns HTML error pages with helpful messages</li>
 *   <li><strong>HTTP Status Codes</strong> - Appropriate status codes for different error conditions</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * // Register servlets with HTTP server
 * MyHTTPServer server = new MyHTTPServer(8080, 5);
 * 
 * // Configuration upload endpoint
 * server.addServlet("POST", "/config", new ConfLoader());
 * 
 * // Graph visualization endpoint
 * server.addServlet("GET", "/graph", new GraphRefresh());
 * 
 * // Topic management endpoint
 * server.addServlet("GET", "/topics", new TopicDisplayer());
 * 
 * // Start server
 * server.start();
 * 
 * // Servlets are now handling requests at their respective endpoints
 * }</pre>
 * 
 * <h2>Security Considerations</h2>
 * 
 * <ul>
 *   <li><strong>Input Sanitization</strong> - All user input is escaped to prevent XSS attacks</li>
 *   <li><strong>File Upload Validation</strong> - Configuration files are validated before processing</li>
 *   <li><strong>Access Control</strong> - Topic publishing is restricted based on topic type</li>
 *   <li><strong>Resource Limits</strong> - File upload sizes and processing limits are enforced</li>
 * </ul>
 * 
 * @since 1.0
 * @see server
 * @see views
 */
package servlets;