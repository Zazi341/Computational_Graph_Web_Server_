package servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import server.RequestParser.RequestInfo;
import graph.TopicManagerSingleton;
import graph.Topic;
import graph.Message;

/**
 * TopicDisplayer servlet provides a comprehensive interface for viewing and interacting
 * with topics in the computational graph system. This servlet displays current topic
 * values in an HTML table format and handles message publishing to input-only topics.
 * 
 * <p>The servlet serves dual purposes:
 * <ol>
 * <li><strong>Topic Status Display</strong> - Shows all topics with their current values,
 *     subscribers, publishers, and status information</li>
 * <li><strong>Message Publishing</strong> - Allows publishing messages to input-only topics
 *     while protecting intermediate and output topics from direct manipulation</li>
 * </ol>
 * 
 * <h3>Supported HTTP Methods</h3>
 * <ul>
 * <li><strong>GET</strong> - Display topic status table</li>
 * <li><strong>POST</strong> - Publish message to topic and display updated table</li>
 * </ul>
 * 
 * <h3>Request Format</h3>
 * <p>For topic status display (GET request):
 * <pre>{@code
 * GET /topics HTTP/1.1
 * Host: localhost:8080
 * }</pre>
 * 
 * <p>For message publishing (POST request with parameters):
 * <pre>{@code
 * POST /topics?topic=input1&msg=42 HTTP/1.1
 * Host: localhost:8080
 * }</pre>
 * 
 * <h3>Response Format</h3>
 * <p>Returns HTML content with topic status table:
 * <ul>
 * <li><strong>Success (200 OK)</strong> - HTML page with topic status table</li>
 * <li><strong>Error (200 OK)</strong> - HTML error page or JavaScript alert for protected topics</li>
 * </ul>
 * 
 * <h3>Topic Status Table Features</h3>
 * <p>The generated HTML table includes:
 * <ul>
 * <li><strong>Topic Name</strong> - Name of each topic in the system</li>
 * <li><strong>Last Value</strong> - Current value of the topic (N/A if no value)</li>
 * <li><strong>Subscribers</strong> - List of agents subscribed to the topic</li>
 * <li><strong>Publishers</strong> - List of agents that publish to the topic</li>
 * <li><strong>Status</strong> - Topic classification (Input-Only, Output-Only, Intermediate, Inactive)</li>
 * </ul>
 * 
 * <h3>Topic Protection System</h3>
 * <p>The servlet implements a protection system to prevent invalid message publishing:
 * <ul>
 * <li><strong>Input-Only Topics</strong> - Allow direct message publishing (no publishers, has subscribers)</li>
 * <li><strong>Output-Only Topics</strong> - Block direct publishing (has publishers, no subscribers)</li>
 * <li><strong>Intermediate Topics</strong> - Block direct publishing (has both publishers and subscribers)</li>
 * <li><strong>Inactive Topics</strong> - Allow publishing but warn about lack of connections</li>
 * </ul>
 * 
 * <h3>Real-time Updates</h3>
 * <p>The servlet includes JavaScript functionality for:
 * <ul>
 * <li>Automatic refresh when configuration changes are detected</li>
 * <li>Cross-window communication using localStorage events</li>
 * <li>PostMessage API integration for window coordination</li>
 * <li>Cache prevention headers to ensure fresh data</li>
 * </ul>
 * 
 * <h3>Integration with System Components</h3>
 * <ul>
 * <li><strong>TopicManagerSingleton</strong> - Primary source of topic data and management</li>
 * <li><strong>Topic</strong> - Individual topic instances with values and relationships</li>
 * <li><strong>Message</strong> - Message objects for publishing to topics</li>
 * <li><strong>Agent System</strong> - Provides subscriber and publisher information</li>
 * </ul>
 * 
 * <h3>Security Features</h3>
 * <ul>
 * <li>HTML escaping to prevent XSS attacks</li>
 * <li>Topic protection to maintain system integrity</li>
 * <li>Input validation for topic names and message values</li>
 * </ul>
 * 
 * @see TopicManagerSingleton
 * @see Topic#publish(Message)
 * @see Message
 * @see Servlet#handle(RequestInfo, OutputStream)
 * 
 * @since 1.0
 */
public class TopicDisplayer implements Servlet {

    /**
     * Handles HTTP requests for topic status display and message publishing.
     * This method serves dual purposes: displaying the current status of all topics
     * in an HTML table format and processing message publishing requests to input-only topics.
     * 
     * <p>The method performs the following operations:
     * <ol>
     * <li>Extracts topic name and message value from request parameters (if present)</li>
     * <li>If publishing parameters are provided, validates the target topic</li>
     * <li>Applies topic protection rules to prevent invalid publishing</li>
     * <li>Publishes the message if the topic allows direct input</li>
     * <li>Generates and returns an HTML table showing all topic statuses</li>
     * </ol>
     * 
     * <h3>Request Parameter Processing</h3>
     * <p>The method looks for the following URL parameters:
     * <ul>
     * <li><strong>topic</strong> - Name of the topic to publish to</li>
     * <li><strong>msg</strong> - Message value to publish</li>
     * </ul>
     * 
     * <h3>Topic Protection Logic</h3>
     * <p>The method implements protection rules to maintain system integrity:
     * <ul>
     * <li><strong>Input-Only Topics</strong> - Allow publishing (no publishers, has subscribers)</li>
     * <li><strong>Output-Only Topics</strong> - Block publishing with alert (has publishers, no subscribers)</li>
     * <li><strong>Intermediate Topics</strong> - Block publishing with alert (has both publishers and subscribers)</li>
     * <li><strong>Non-existent Topics</strong> - Ignore publishing attempt</li>
     * </ul>
     * 
     * <h3>HTML Response Generation</h3>
     * <p>The method always returns an HTML table containing:
     * <ul>
     * <li>Topic names and current values</li>
     * <li>Subscriber and publisher information</li>
     * <li>Topic status classifications</li>
     * <li>Real-time refresh capabilities</li>
     * <li>Styling and interactive features</li>
     * </ul>
     * 
     * <h3>Error Handling</h3>
     * <p>The method handles various error conditions:
     * <ul>
     * <li>Protected topic publishing attempts result in JavaScript alerts</li>
     * <li>System exceptions are caught and result in error pages</li>
     * <li>All errors are logged for debugging purposes</li>
     * </ul>
     * 
     * @param ri the RequestInfo containing HTTP request data and parameters
     * @param toClient the OutputStream to write the HTML response to
     * @throws IOException if there are errors writing to the output stream
     * 
     * @see #findExistingTopic(String)
     * @see #generateTopicsTable()
     * @see Topic#publish(Message)
     * @see Message#Message(String)
     */
    @Override
    public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        System.out.println("[DEBUG] TopicDisplayer invoked");
        
        // Debug: Print current topic values when table is requested
        System.out.println("[DEBUG] Current topics in TopicDisplayer:");
        for (var topic : TopicManagerSingleton.get().getTopics()) {
            System.out.println("[DEBUG] - Topic '" + topic.name + "' = '" + topic.getLastValue() + "'");
        }

        try {
            // Extract topic and message from request parameters
            String topicName = ri.getParameters().get("topic");
            String messageValue = ri.getParameters().get("msg"); // Form sends 'msg' parameter

            if (topicName != null && messageValue != null) {
                System.out.println("[DEBUG] Received publish request - Topic: '" + topicName + "', Value: '" + messageValue + "'");
                
                // Check if topic exists (don't create new ones)
                Topic topic = findExistingTopic(topicName);
                if (topic != null) {
                    System.out.println("[DEBUG] Topic '" + topicName + "' found. Current value before publish: '" + topic.getLastValue() + "'");
                    
                    // Check if this topic should be protected from direct input
                    boolean hasPublishers = !topic.getPublishers().isEmpty();
                    boolean hasSubscribers = !topic.getSubscribers().isEmpty();
                    boolean isProtected = hasPublishers; // Any topic with publishers (output or intermediate) is protected
                    
                    if (isProtected) {
                        String topicType = hasSubscribers ? "intermediate" : "output-only";
                        System.out.println("[DEBUG] Blocked attempt to publish to " + topicType + " topic '" + topicName + "'");
                        
                        // Send error response with alert message
                        String alertMessage = "Cannot publish to " + topicType + " topic '" + topicName + "'. This topic is managed by agents.";
                        String alertScript = "<script>alert('" + alertMessage.replace("'", "\\'") + "'); history.back();</script>";
                        sendHtmlResponse(toClient, alertScript);
                        return;
                    } else {
                        Message message = new Message(messageValue);
                        topic.publish(message);
                        System.out.println("[DEBUG] Published message '" + messageValue + "' to topic '" + topicName + "'");
                        System.out.println("[DEBUG] Topic '" + topicName + "' value after publish: '" + topic.getLastValue() + "'");
                    }
                } else {
                    System.out.println("[DEBUG] Topic '" + topicName + "' not found");
                }
            }

            // Generate HTML table with all topics and their values
            String htmlResponse = generateTopicsTable();
            sendHtmlResponse(toClient, htmlResponse);
            
            // If a message was published, also regenerate the graph to show updated values
            if (topicName != null && messageValue != null) {
                // Note: The graph will be updated next time it's viewed since values are now stored in topics
                System.out.println("[DEBUG] Message published - graph will show updated values on next view");
            }

        } catch (Exception e) {
            System.err.println("[ERROR] TopicDisplayer error: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(toClient, "Error processing request: " + e.getMessage());
        }
    }

    /**
     * Generates a comprehensive HTML table displaying all topics and their current status.
     * This method creates a complete HTML page with styling, JavaScript functionality,
     * and a detailed table showing topic information.
     * 
     * <p>The generated HTML includes:
     * <ul>
     * <li><strong>Responsive Table</strong> - Shows topic names, values, relationships, and status</li>
     * <li><strong>Real-time JavaScript</strong> - Auto-refresh capabilities and cross-window communication</li>
     * <li><strong>Status Indicators</strong> - Visual indicators for different topic types</li>
     * <li><strong>Styling</strong> - Professional CSS styling with hover effects and color coding</li>
     * </ul>
     * 
     * <h3>Table Columns</h3>
     * <ul>
     * <li><strong>Topic Name</strong> - The name of each topic in the system</li>
     * <li><strong>Last Value</strong> - Current value or "N/A" if no value has been set</li>
     * <li><strong>Subscribers</strong> - List of agents that subscribe to this topic</li>
     * <li><strong>Publishers</strong> - List of agents that publish to this topic</li>
     * <li><strong>Status</strong> - Classification (Input-Only, Output-Only, Intermediate, Inactive)</li>
     * </ul>
     * 
     * <h3>Status Classifications</h3>
     * <ul>
     * <li><strong>📥 Input-Only</strong> - No publishers, has subscribers (accepts direct input)</li>
     * <li><strong>🔒 Output-Only</strong> - Has publishers, no subscribers (agent output)</li>
     * <li><strong>🔒 Intermediate</strong> - Has both publishers and subscribers (agent-managed)</li>
     * <li><strong>🔴 Inactive</strong> - No publishers or subscribers (unused)</li>
     * </ul>
     * 
     * <h3>JavaScript Features</h3>
     * <p>The embedded JavaScript provides:
     * <ul>
     * <li>localStorage event listening for configuration updates</li>
     * <li>PostMessage API support for cross-window communication</li>
     * <li>Automatic page refresh when system state changes</li>
     * </ul>
     * 
     * @return complete HTML page as a string with topic status table
     * 
     * @see TopicManagerSingleton#getTopics()
     * @see #escapeHtml(String)
     */
    private String generateTopicsTable() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<title>Topics Status</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 10px; background: #f5f5f5; }");
        html.append(".container { background: white; padding: 15px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
        html.append("table { border-collapse: collapse; width: 100%; margin-top: 10px; }");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        html.append("th { background-color: #1976d2; color: white; }");
        html.append("tr:nth-child(even) { background-color: #f9f9f9; }");
        html.append("tr:hover { background-color: #e3f2fd; }");
        html.append(".subscribers, .publishers { font-size: 11px; color: #666; }");
        html.append(".no-data { text-align: center; color: #999; font-style: italic; }");
        html.append(".refresh-info { margin-top: 10px; font-size: 12px; color: #666; }");
        html.append(".last-value { font-weight: bold; color: #2e7d32; }");
        html.append("</style>");
        html.append("<script>");
        html.append("// Listen for config updates and auto-refresh");
        html.append("window.addEventListener('storage', function(e) {");
        html.append("  if (e.key === 'configUpdated') {");
        html.append("    console.log('Config updated - refreshing topic table');");
        html.append("    window.location.reload();");
        html.append("  }");
        html.append("});");
        html.append("// Also listen for postMessage from other windows");
        html.append("window.addEventListener('message', function(e) {");
        html.append("  if (e.data === 'refreshTopicTable') {");
        html.append("    console.log('Received refresh signal - reloading');");
        html.append("    window.location.reload();");
        html.append("  }");
        html.append("});");
        html.append("</script>");
        html.append("</head><body>");

        html.append("<div class='container'>");
        html.append("<h3>📊 Current Topic Values</h3>");

        html.append("<table>");
        html.append("<tr>");
        html.append("<th>Topic Name</th>");
        html.append("<th>Last Value</th>");
        html.append("<th>Subscribers</th>");
        html.append("<th>Publishers</th>");
        html.append("<th>Status</th>");
        html.append("</tr>");

        // Get all topics from TopicManagerSingleton
        Collection<Topic> topics = TopicManagerSingleton.get().getTopics();

        if (topics.isEmpty()) {
            html.append("<tr><td colspan='5' class='no-data'>No topics available yet. Upload a configuration or send messages to create topics.</td></tr>");
        } else {
            for (Topic topic : topics) {
                html.append("<tr>");

                // Topic name
                html.append("<td><strong>").append(escapeHtml(topic.name)).append("</strong></td>");

                // Last value - show actual last message value
                html.append("<td class='last-value'>");
                String lastValue = topic.getLastValue();
                if ("N/A".equals(lastValue)) {
                    html.append("<span style='color: #999;'>N/A</span>");
                } else {
                    html.append(escapeHtml(lastValue));
                }
                html.append("</td>");

                // Subscribers
                html.append("<td class='subscribers'>");
                if (topic.getSubscribers().isEmpty()) {
                    html.append("None");
                } else {
                    boolean first = true;
                    for (var agent : topic.getSubscribers()) {
                        if (!first) html.append(", ");
                        html.append(escapeHtml(agent.getName()));
                        first = false;
                    }
                }
                html.append("</td>");

                // Publishers
                html.append("<td class='publishers'>");
                if (topic.getPublishers().isEmpty()) {
                    html.append("None");
                } else {
                    boolean first = true;
                    for (var agent : topic.getPublishers()) {
                        if (!first) html.append(", ");
                        html.append(escapeHtml(agent.getName()));
                        first = false;
                    }
                }
                html.append("</td>");

                // Status
                html.append("<td>");
                boolean hasPublishers = !topic.getPublishers().isEmpty();
                boolean hasSubscribers = !topic.getSubscribers().isEmpty();
                boolean isOutputOnly = hasPublishers && !hasSubscribers;
                boolean isInputOnly = !hasPublishers && hasSubscribers;
                boolean isIntermediate = hasPublishers && hasSubscribers;
                
                if (!hasSubscribers && !hasPublishers) {
                    html.append("🔴 Inactive");
                } else if (isOutputOnly) {
                    html.append("🔒 Output-Only");
                } else if (isInputOnly) {
                    html.append("📥 Input-Only");
                } else if (isIntermediate) {
                    html.append("🔒 Intermediate");
                } else {
                    html.append("🟡 Partial");
                }
                html.append("</td>");

                html.append("</tr>");
            }
        }

        html.append("</table>");
        html.append("<div class='refresh-info'>");
        html.append("💡 <strong>Tip:</strong> Refreshes automatically to see latest values. ");
        html.append("Total topics: ").append(topics.size());
        
        // Check if there was a recent configuration change
        long lastClear = TopicManagerSingleton.get().getLastClearTime();
        long timeSinceClear = System.currentTimeMillis() - lastClear;
        if (timeSinceClear < 10000) { // Within last 10 seconds
            html.append(" <span style='color: #2e7d32; font-weight: bold;'>✅ Recently updated</span>");
        }
        
        html.append("</div>");
        html.append("</div>");
        html.append("</body></html>");

        return html.toString();
    }

    /**
     * Finds an existing topic by name without creating a new one.
     * This method searches through all topics managed by TopicManagerSingleton
     * to locate a topic with the specified name.
     * 
     * <p>This method is used to validate topic existence before attempting
     * to publish messages, ensuring that only existing topics can receive
     * direct message publishing.
     * 
     * @param topicName the name of the topic to search for
     * @return the Topic instance if found, or null if no topic with the given name exists
     * 
     * @see TopicManagerSingleton#getTopics()
     */
    private Topic findExistingTopic(String topicName) {
        Collection<Topic> topics = TopicManagerSingleton.get().getTopics();
        for (Topic topic : topics) {
            if (topic.name.equals(topicName)) {
                return topic;
            }
        }
        return null; // Topic doesn't exist
    }

    /**
     * Escapes HTML special characters to prevent Cross-Site Scripting (XSS) attacks.
     * This method converts potentially dangerous characters in user input, topic names,
     * agent names, and other dynamic content to their HTML entity equivalents.
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

    /**
     * Sends an HTML response to the client with proper HTTP headers including cache control.
     * This method constructs a complete HTTP response with appropriate headers to ensure
     * fresh data delivery and prevent caching of dynamic topic information.
     * 
     * <p>The response includes cache prevention headers:
     * <ul>
     * <li>Cache-Control: no-cache, no-store, must-revalidate</li>
     * <li>Pragma: no-cache</li>
     * <li>Expires: 0</li>
     * </ul>
     * 
     * @param toClient the OutputStream to write the response to
     * @param htmlContent the HTML content to send in the response body
     * @throws IOException if there are errors writing to the output stream
     */
    private void sendHtmlResponse(OutputStream toClient, String htmlContent) throws IOException {
        byte[] contentBytes = htmlContent.getBytes(StandardCharsets.UTF_8);

        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: " + contentBytes.length + "\r\n" +
                "Cache-Control: no-cache, no-store, must-revalidate\r\n" +
                "Pragma: no-cache\r\n" +
                "Expires: 0\r\n" +
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
        String html = "<html><body><h3>Error</h3><p>" + escapeHtml(message) + "</p>" +
                "<a href='javascript:history.back()'>Go Back</a></body></html>";
        sendHtmlResponse(toClient, html);
    }

    @Override
    public void close() throws IOException {
        System.out.println("[DEBUG] TopicDisplayer closed");
    }
}