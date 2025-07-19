package views;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import graph.TopicManagerSingleton;
import graph.Topic;
import graph.Agent;

/**
 * Utility class for generating HTML visualizations of computational graphs.
 * 
 * <p>This class provides comprehensive graph visualization capabilities by analyzing
 * the current state of the TopicManagerSingleton and generating interactive HTML
 * representations. It supports multiple rendering strategies including Canvas-based
 * interactive visualizations and SVG-based static diagrams.</p>
 * 
 * <p>The visualization system automatically calculates optimal layouts for topics
 * and agents, generates connection lines showing data flow, and provides real-time
 * updates of topic values. The generated HTML includes embedded JavaScript for
 * interactive features and responsive design.</p>
 * 
 * <h3>Key Features:</h3>
 * <ul>
 * <li><strong>Canvas Rendering:</strong> Interactive graph visualization with real-time updates</li>
 * <li><strong>SVG Generation:</strong> Static vector graphics for high-quality output</li>
 * <li><strong>Automatic Layout:</strong> Grid-based positioning algorithm for optimal readability</li>
 * <li><strong>Template System:</strong> Flexible HTML template loading with fallback support</li>
 * <li><strong>JSON Data Export:</strong> Structured data format for client-side rendering</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Generate complete HTML visualization
 * String htmlContent = HtmlGraphWriter.getGraphHTML();
 * 
 * // Send to client via HTTP response
 * response.setContentType("text/html");
 * response.getWriter().write(htmlContent);
 * 
 * // The generated HTML will show:
 * // - Topic nodes as rectangles with current values
 * // - Agent nodes as circles with type information
 * // - Connection lines showing data flow direction
 * // - Interactive features for exploring the graph
 * }</pre>
 * 
 * <h3>Graph Layout Algorithm:</h3>
 * <p>The class uses a two-tier layout system:
 * <ul>
 * <li><strong>Topic Layer:</strong> Topics arranged in a grid pattern in the upper area</li>
 * <li><strong>Agent Layer:</strong> Agents positioned in a horizontal row below topics</li>
 * <li><strong>Connections:</strong> Lines drawn between related topics and agents</li>
 * </ul>
 * 
 * <h3>Template System:</h3>
 * <p>The visualization system supports external HTML templates loaded from
 * {@code html_files/graph.html} with placeholder replacement for dynamic content.
 * If the template file is not available, an embedded template provides fallback
 * functionality with full Canvas-based rendering capabilities.</p>
 * 
 * <h3>Thread Safety:</h3>
 * <p>This class is stateless and thread-safe. All methods are static and do not
 * maintain any shared state, making it safe for concurrent use in the HTTP server's
 * multi-threaded environment.</p>
 * 
 * @see graph.TopicManagerSingleton
 * @see graph.Topic
 * @see graph.Agent
 * @see servlets.GraphRefresh
 * @since 1.0
 */
public class HtmlGraphWriter {

    private static final String GRAPH_TEMPLATE_PATH = "html_files/graph.html";

    /**
     * Generates complete HTML content for visualizing the current computational graph.
     * 
     * <p>This is the main entry point for graph visualization. It creates a complete HTML
     * document with embedded JavaScript that renders an interactive Canvas-based visualization
     * of the current computational graph state. The method automatically handles template
     * loading, data extraction, and fallback scenarios.</p>
     * 
     * <p>The generated HTML includes:
     * <ul>
     * <li>Interactive Canvas element with graph visualization</li>
     * <li>Real-time topic values and connection information</li>
     * <li>Responsive layout optimized for web browsers</li>
     * <li>Embedded CSS styling for professional appearance</li>
     * <li>JavaScript code for dynamic rendering and updates</li>
     * </ul>
     * 
     * <h3>Visualization Features:</h3>
     * <ul>
     * <li><strong>Topic Nodes:</strong> Blue rectangles showing topic names and current values</li>
     * <li><strong>Agent Nodes:</strong> Green circles displaying agent types and names</li>
     * <li><strong>Connection Lines:</strong> Gray lines showing data flow between components</li>
     * <li><strong>Layout Algorithm:</strong> Automatic positioning for optimal readability</li>
     * </ul>
     * 
     * <h3>Template System:</h3>
     * <p>The method first attempts to load an external template from {@code html_files/graph.html}.
     * If the template file is not found, it falls back to an embedded template with full
     * functionality. The template uses placeholder replacement to inject graph data.</p>
     * 
     * <h3>Error Handling:</h3>
     * <p>If any errors occur during template loading or data processing, the method
     * automatically generates a fallback HTML page with basic graph information and
     * error details for debugging purposes.</p>
     * 
     * <h3>Usage Example:</h3>
     * <pre>{@code
     * // In a servlet handling graph visualization requests
     * public void handle(String httpCommand, RequestInfo requestInfo, OutputStream outputStream) {
     *     String htmlContent = HtmlGraphWriter.getGraphHTML();
     *     
     *     // Send HTML response
     *     String response = "HTTP/1.1 200 OK\r\n" +
     *                      "Content-Type: text/html\r\n" +
     *                      "Content-Length: " + htmlContent.length() + "\r\n\r\n" +
     *                      htmlContent;
     *     outputStream.write(response.getBytes());
     * }
     * }</pre>
     * 
     * @return complete HTML document as a String, ready to be sent to web browsers
     * @see #generateGraphJSON(Collection)
     * @see #generateFallbackHTML(String)
     * @see servlets.GraphRefresh
     */
    public static String getGraphHTML() {
        try {
            // Load the HTML template
            String template = loadTemplate();

            // Analyze current graph state
            Collection<Topic> topics = TopicManagerSingleton.get().getTopics();

            // Generate JSON data for Canvas visualization
            String graphData = generateGraphJSON(topics);

            // Replace placeholder in template
            String result = template.replace("{{GRAPH_DATA}}", graphData);

            return result;

        } catch (Exception e) {
            // Fallback to simple HTML if template loading fails
            return generateFallbackHTML(e.getMessage());
        }
    }

    /**
     * Loads the HTML template file for graph visualization with fallback support.
     * 
     * <p>This method implements a two-tier template loading strategy:
     * <ol>
     * <li>First attempts to load external template from {@code html_files/graph.html}</li>
     * <li>Falls back to embedded template if external file is not found</li>
     * </ol>
     * 
     * <p>The external template allows for customization of the visualization appearance
     * and behavior, while the embedded template ensures the system always functions
     * even in minimal deployment scenarios.</p>
     * 
     * @return HTML template string with {{GRAPH_DATA}} placeholder for data injection
     * @throws IOException if external template exists but cannot be read
     * @see #getEmbeddedTemplate()
     */
    private static String loadTemplate() throws IOException {
        if (Files.exists(Paths.get(GRAPH_TEMPLATE_PATH))) {
            return Files.readString(Paths.get(GRAPH_TEMPLATE_PATH));
        } else {
            // Return embedded template if file doesn't exist
            return getEmbeddedTemplate();
        }
    }

    /**
     * Generates SVG content representing the computational graph.
     */
    private static String generateGraphSVG(Collection<Topic> topics) {
        StringBuilder svg = new StringBuilder();

        if (topics.isEmpty()) {
            svg.append("<text x='400' y='200' text-anchor='middle' style='font-size:16px;fill:#666;'>");
            svg.append("No configuration loaded yet. Upload a config file to see the graph.</text>");
            return svg.toString();
        }

        // Calculate layout positions
        List<Topic> topicList = new ArrayList<>(topics);
        Map<String, Point> positions = calculateLayout(topicList);

        // Generate topic nodes
        for (Topic topic : topicList) {
            Point pos = positions.get(topic.name);
            if (pos != null) {
                svg.append(generateTopicNode(topic, pos));
            }
        }

        // Generate agent nodes and connections
        Set<Agent> allAgents = collectAllAgents(topicList);
        Map<Agent, Point> agentPositions = calculateAgentLayout(allAgents, positions);

        for (Agent agent : allAgents) {
            Point pos = agentPositions.get(agent);
            if (pos != null) {
                svg.append(generateAgentNode(agent, pos));
            }
        }

        // Generate connections
        svg.append(generateConnections(topicList, allAgents, positions, agentPositions));

        return svg.toString();
    }

    /**
     * Calculate positions for topics in a grid layout.
     */
    private static Map<String, Point> calculateLayout(List<Topic> topics) {
        Map<String, Point> positions = new HashMap<>();

        if (topics.isEmpty()) return positions;

        // Layout optimized for 600x350 canvas
        int cols = Math.min(3, Math.max(2, (int) Math.ceil(Math.sqrt(topics.size()))));
        int spacingX = 150;
        int spacingY = 100;
        int startX = 120;
        int startY = 70;

        for (int i = 0; i < topics.size(); i++) {
            int row = i / cols;
            int col = i % cols;
            int x = startX + col * spacingX;
            int y = startY + row * spacingY;
            positions.put(topics.get(i).name, new Point(x, y));
        }

        return positions;
    }

    /**
     * Collect all unique agents from all topics.
     */
    private static Set<Agent> collectAllAgents(List<Topic> topics) {
        Set<Agent> agents = new HashSet<>();
        for (Topic topic : topics) {
            agents.addAll(topic.getSubscribers());
            agents.addAll(topic.getPublishers());
        }
        return agents;
    }

    /**
     * Calculate positions for agents between topics.
     */
    private static Map<Agent, Point> calculateAgentLayout(Set<Agent> agents, Map<String, Point> topicPositions) {
        Map<Agent, Point> agentPositions = new HashMap<>();

        List<Agent> agentList = new ArrayList<>(agents);
        if (agentList.isEmpty()) return agentPositions;

        // Place agents in a row below topics, optimized for 350px canvas height
        int agentY = 240; // Fit within 350px canvas (leave 110px margin from bottom)
        int spacingX = 140;
        int startX = 160;

        for (int i = 0; i < agentList.size(); i++) {
            int x = startX + (i * spacingX);
            agentPositions.put(agentList.get(i), new Point(x, agentY));
        }

        return agentPositions;
    }

    /**
     * Generate SVG for a topic node.
     */
    private static String generateTopicNode(Topic topic, Point pos) {
        StringBuilder node = new StringBuilder();

        int width = 80;
        int height = 40;
        int x = pos.x - width / 2;
        int y = pos.y - height / 2;

        // Rectangle for topic
        node.append(String.format(
                "<rect x='%d' y='%d' width='%d' height='%d' class='node-topic'/>\n",
                x, y, width, height
        ));

        // Topic name
        node.append(String.format(
                "<text x='%d' y='%d' class='node-text'>%s</text>\n",
                pos.x, pos.y, truncateText(topic.name, 8)
        ));

        // Connection count info
        int totalConnections = topic.getSubscribers().size() + topic.getPublishers().size();
        node.append(String.format(
                "<text x='%d' y='%d' style='font-size:10px;fill:#666;text-anchor:middle;'>%d conn</text>\n",
                pos.x, pos.y + 15, totalConnections
        ));

        return node.toString();
    }

    /**
     * Generate SVG for an agent node.
     */
    private static String generateAgentNode(Agent agent, Point pos) {
        StringBuilder node = new StringBuilder();

        int radius = 25;

        // Circle for agent
        node.append(String.format(
                "<circle cx='%d' cy='%d' r='%d' class='node-agent'/>\n",
                pos.x, pos.y, radius
        ));

        // Agent name
        node.append(String.format(
                "<text x='%d' y='%d' class='node-text'>%s</text>\n",
                pos.x, pos.y, truncateText(agent.getName(), 6)
        ));

        return node.toString();
    }

    /**
     * Generate connections between topics and agents.
     */
    private static String generateConnections(List<Topic> topics, Set<Agent> agents,
                                              Map<String, Point> topicPositions,
                                              Map<Agent, Point> agentPositions) {
        StringBuilder connections = new StringBuilder();

        for (Topic topic : topics) {
            Point topicPos = topicPositions.get(topic.name);
            if (topicPos == null) continue;

            // Connections from topic to subscribers (agents)
            for (Agent subscriber : topic.getSubscribers()) {
                Point agentPos = agentPositions.get(subscriber);
                if (agentPos != null) {
                    connections.append(String.format(
                            "<line x1='%d' y1='%d' x2='%d' y2='%d' class='edge'/>\n",
                            topicPos.x, topicPos.y, agentPos.x, agentPos.y
                    ));
                }
            }

            // Connections from publishers (agents) to topic
            for (Agent publisher : topic.getPublishers()) {
                Point agentPos = agentPositions.get(publisher);
                if (agentPos != null) {
                    connections.append(String.format(
                            "<line x1='%d' y1='%d' x2='%d' y2='%d' class='edge'/>\n",
                            agentPos.x, agentPos.y, topicPos.x, topicPos.y
                    ));
                }
            }
        }

        return connections.toString();
    }

    /**
     * Generates structured JSON data for Canvas-based graph visualization.
     * 
     * <p>This method creates a comprehensive JSON representation of the computational
     * graph that can be consumed by client-side JavaScript for interactive rendering.
     * The JSON structure includes topics, agents, and connections with precise
     * positioning information calculated by the layout algorithms.</p>
     * 
     * <h3>JSON Structure:</h3>
     * <pre>{@code
     * {
     *   "topics": [
     *     {
     *       "name": "topic1",
     *       "x": 120, "y": 70,
     *       "connections": 2,
     *       "value": "current_value"
     *     }
     *   ],
     *   "agents": [
     *     {
     *       "name": "PlusAgent",
     *       "x": 160, "y": 240
     *     }
     *   ],
     *   "connections": [
     *     {
     *       "x1": 120, "y1": 70,
     *       "x2": 160, "y2": 240,
     *       "type": "input"
     *     }
     *   ]
     * }
     * }</pre>
     * 
     * <h3>Connection Types:</h3>
     * <ul>
     * <li><strong>input:</strong> Data flow from topic to agent (topic → agent)</li>
     * <li><strong>output:</strong> Data flow from agent to topic (agent → topic)</li>
     * </ul>
     * 
     * <h3>Layout Integration:</h3>
     * <p>The method integrates with the layout calculation algorithms to ensure
     * consistent positioning between different rendering modes (Canvas, SVG).
     * All coordinates are optimized for the standard 700x400 canvas dimensions.</p>
     * 
     * @param topics collection of topics from TopicManagerSingleton
     * @return JSON string ready for client-side JavaScript consumption
     * @see #calculateLayout(List)
     * @see #calculateAgentLayout(Set, Map)
     * @see #escapeJson(String)
     */
    private static String generateGraphJSON(Collection<Topic> topics) {
        StringBuilder json = new StringBuilder();
        json.append("{ ");
        
        // Topics array
        json.append("\"topics\": [");
        List<Topic> topicList = new ArrayList<>(topics);
        Map<String, Point> topicPositions = calculateLayout(topicList);
        
        for (int i = 0; i < topicList.size(); i++) {
            Topic topic = topicList.get(i);
            Point pos = topicPositions.get(topic.name);
            if (pos != null) {
                if (i > 0) json.append(", ");
                json.append("{ ");
                json.append("\"name\": \"").append(escapeJson(topic.name)).append("\", ");
                json.append("\"x\": ").append(pos.x).append(", ");
                json.append("\"y\": ").append(pos.y).append(", ");
                json.append("\"connections\": ").append(topic.getSubscribers().size() + topic.getPublishers().size()).append(", ");
                json.append("\"value\": \"").append(escapeJson(topic.getLastValue())).append("\"");
                json.append(" }");
            }
        }
        json.append("], ");
        
        // Agents array
        json.append("\"agents\": [");
        Set<Agent> allAgents = collectAllAgents(topicList);
        Map<Agent, Point> agentPositions = calculateAgentLayout(allAgents, topicPositions);
        List<Agent> agentList = new ArrayList<>(allAgents);
        
        for (int i = 0; i < agentList.size(); i++) {
            Agent agent = agentList.get(i);
            Point pos = agentPositions.get(agent);
            if (pos != null) {
                if (i > 0) json.append(", ");
                json.append("{ ");
                json.append("\"name\": \"").append(escapeJson(truncateText(agent.getName(), 8))).append("\", ");
                json.append("\"x\": ").append(pos.x).append(", ");
                json.append("\"y\": ").append(pos.y);
                json.append(" }");
            }
        }
        json.append("], ");
        
        // Connections array
        json.append("\"connections\": [");
        List<String> connections = new ArrayList<>();
        
        for (Topic topic : topicList) {
            Point topicPos = topicPositions.get(topic.name);
            if (topicPos == null) continue;
            
            // Connections from topic to subscribers (INPUT arrows - topic to agent)
            for (Agent subscriber : topic.getSubscribers()) {
                Point agentPos = agentPositions.get(subscriber);
                if (agentPos != null) {
                    connections.add(String.format(
                        "{ \"x1\": %d, \"y1\": %d, \"x2\": %d, \"y2\": %d, \"type\": \"input\" }",
                        topicPos.x, topicPos.y, agentPos.x, agentPos.y
                    ));
                }
            }
            
            // Connections from publishers to topic (OUTPUT arrows - agent to topic)
            for (Agent publisher : topic.getPublishers()) {
                Point agentPos = agentPositions.get(publisher);
                if (agentPos != null) {
                    connections.add(String.format(
                        "{ \"x1\": %d, \"y1\": %d, \"x2\": %d, \"y2\": %d, \"type\": \"output\" }",
                        agentPos.x, agentPos.y, topicPos.x, topicPos.y
                    ));
                }
            }
        }
        
        for (int i = 0; i < connections.size(); i++) {
            if (i > 0) json.append(", ");
            json.append(connections.get(i));
        }
        json.append("] ");
        
        json.append("}");
        return json.toString();
    }

    /**
     * Escape JSON special characters.
     */
    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * Generate a mathematical expression representation of the graph.
     */
    private static String generateExpression(Collection<Topic> topics) {
        if (topics.isEmpty()) {
            return "No configuration loaded";
        }

        // Simple expression based on agents found
        StringBuilder expr = new StringBuilder();
        Set<Agent> allAgents = collectAllAgents(new ArrayList<>(topics));

        boolean hasPlus = false, hasInc = false;
        for (Agent agent : allAgents) {
            if (agent.getName().toLowerCase().contains("plus")) hasPlus = true;
            if (agent.getName().toLowerCase().contains("inc")) hasInc = true;
        }

        if (hasPlus && hasInc) {
            expr.append("Complex computational graph with arithmetic and increment operations");
        } else if (hasPlus) {
            expr.append("Addition-based computation");
        } else if (hasInc) {
            expr.append("Increment-based computation");
        } else {
            expr.append("Custom computational graph (").append(allAgents.size()).append(" agents, ").append(topics.size()).append(" topics)");
        }

        return expr.toString();
    }

    /**
     * Generate fallback HTML when template loading fails.
     */
    private static String generateFallbackHTML(String error) {
        Collection<Topic> topics = TopicManagerSingleton.get().getTopics();
        Set<Agent> agents = collectAllAgents(new ArrayList<>(topics));

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><title>Graph Visualization</title>");
        html.append("<style>body{font-family:Arial;margin:20px;background:#f5f5f5;}</style></head><body>");
        html.append("<div style='background:white;padding:20px;border-radius:8px;'>");
        html.append("<h2>🔗 Computational Graph Loaded</h2>");
        html.append("<p><strong>Template Error:</strong> ").append(error).append("</p>");
        html.append("<h3>📊 Graph Summary</h3>");
        html.append("<ul>");
        html.append("<li><strong>Topics:</strong> ").append(topics.size()).append("</li>");
        html.append("<li><strong>Agents:</strong> ").append(agents.size()).append("</li>");
        html.append("</ul>");

        if (!topics.isEmpty()) {
            html.append("<h3>📁 Topics</h3><ul>");
            for (Topic topic : topics) {
                html.append("<li><strong>").append(topic.name).append("</strong> - ");
                html.append("Subs: ").append(topic.getSubscribers().size());
                html.append(", Pubs: ").append(topic.getPublishers().size()).append("</li>");
            }
            html.append("</ul>");
        }

        if (!agents.isEmpty()) {
            html.append("<h3>⚙️ Agents</h3><ul>");
            for (Agent agent : agents) {
                html.append("<li>").append(agent.getName()).append("</li>");
            }
            html.append("</ul>");
        }

        html.append("<p>✅ <em>Configuration loaded successfully!</em></p>");
        html.append("</div></body></html>");
        return html.toString();
    }

    /**
     * Get embedded template if external file doesn't exist.
     */
    private static String getEmbeddedTemplate() {
        return "<!DOCTYPE html><html><head><title>Graph Visualization</title>" +
                "<style>body{font-family:Arial;margin:20px;background:#f5f5f5;}" +
                ".container{background:white;padding:15px;border-radius:8px;}" +
                "#graphCanvas{border:2px solid #ddd;border-radius:8px;background:white;display:block;margin:10px auto;}" +
                ".info{margin-top:10px;font-size:14px;color:#666;text-align:center;}</style></head><body>" +
                "<div class='container'><h2>🔗 Computational Graph</h2>" +
                "<canvas id='graphCanvas' width='700' height='400'></canvas>" +
                "<div class='info' id='graphInfo'>Loading...</div></div>" +
                "<script>window.graphData={{GRAPH_DATA}};function drawGraph(){" +
                "const canvas=document.getElementById('graphCanvas');const ctx=canvas.getContext('2d');" +
                "const data=window.graphData||{topics:[],agents:[],connections:[]};" +
                "ctx.clearRect(0,0,canvas.width,canvas.height);" +
                "if(data.topics.length===0){ctx.fillStyle='#666';ctx.font='16px Arial';ctx.textAlign='center';" +
                "ctx.fillText('No configuration loaded yet.',canvas.width/2,canvas.height/2-10);" +
                "ctx.fillText('Upload a config file to see the graph.',canvas.width/2,canvas.height/2+10);return;}" +
                "ctx.strokeStyle='#666';ctx.lineWidth=2;data.connections.forEach(conn=>{ctx.beginPath();" +
                "ctx.moveTo(conn.x1,conn.y1);ctx.lineTo(conn.x2,conn.y2);ctx.stroke();});" +
                "data.topics.forEach(topic=>{ctx.fillStyle='#e3f2fd';ctx.strokeStyle='#1976d2';ctx.lineWidth=2;" +
                "ctx.fillRect(topic.x-40,topic.y-20,80,40);ctx.strokeRect(topic.x-40,topic.y-20,80,40);" +
                "ctx.fillStyle='#000';ctx.font='bold 12px Arial';ctx.textAlign='center';" +
                "ctx.fillText(topic.name,topic.x,topic.y-2);});" +
                "data.agents.forEach(agent=>{ctx.fillStyle='#e8f5e8';ctx.strokeStyle='#388e3c';ctx.lineWidth=2;" +
                "ctx.beginPath();ctx.arc(agent.x,agent.y,25,0,2*Math.PI);ctx.fill();ctx.stroke();" +
                "ctx.fillStyle='#000';ctx.font='bold 11px Arial';ctx.textAlign='center';" +
                "ctx.fillText(agent.name,agent.x,agent.y+3);});" +
                "document.getElementById('graphInfo').innerHTML='📊 Graph contains '+data.topics.length+' topics and '+data.agents.length+' agents';}" +
                "window.onload=drawGraph;</script></body></html>";
    }

    /**
     * Utility method to truncate text for display.
     */
    private static String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Simple Point class for 2D coordinates.
     */
    private static class Point {
        final int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}