package servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import server.RequestParser.RequestInfo;
import configs.GenericConfig;
import graph.TopicManagerSingleton;
import views.HtmlGraphWriter;

/**
 * ConfLoader servlet handles configuration file uploads via HTTP POST requests and generates
 * graph visualizations. This servlet processes multipart form data containing configuration
 * files, parses them using GenericConfig, and returns HTML graph visualizations.
 * 
 * <p>The servlet supports the following workflow:
 * <ol>
 * <li>Receives multipart form data with configuration file content</li>
 * <li>Extracts filename and file content from the multipart data</li>
 * <li>Saves the configuration file to the config_files directory</li>
 * <li>Clears existing topics and agents from the system</li>
 * <li>Loads the new configuration using GenericConfig</li>
 * <li>Generates and returns HTML graph visualization</li>
 * </ol>
 * 
 * <h3>Supported HTTP Methods</h3>
 * <ul>
 * <li><strong>POST</strong> - Upload configuration file and generate graph</li>
 * </ul>
 * 
 * <h3>Request Format</h3>
 * <p>Expects multipart/form-data with a file field containing the configuration file:
 * <pre>{@code
 * POST /config HTTP/1.1
 * Content-Type: multipart/form-data; boundary=----WebKitFormBoundary...
 * 
 * ------WebKitFormBoundary...
 * Content-Disposition: form-data; name="file"; filename="example.conf"
 * Content-Type: text/plain
 * 
 * configs.PlusAgent
 * input1,input2
 * output1
 * ------WebKitFormBoundary...--
 * }</pre>
 * 
 * <h3>Response Format</h3>
 * <p>Returns HTML content with the graph visualization:
 * <ul>
 * <li><strong>Success (200 OK)</strong> - HTML page with SVG/Canvas graph visualization</li>
 * <li><strong>Error (200 OK)</strong> - HTML error page with error message and back button</li>
 * </ul>
 * 
 * <h3>Configuration File Format</h3>
 * <p>The uploaded configuration file must follow the 3-line format per agent:
 * <pre>{@code
 * configs.PlusAgent          # Agent class name
 * topic1,topic2             # Input topics (comma-separated)
 * result                    # Output topics (comma-separated)
 * 
 * configs.IncAgent
 * result                    # Input topics
 * final                     # Output topics
 * }</pre>
 * 
 * <h3>Error Handling</h3>
 * <p>The servlet handles various error conditions:
 * <ul>
 * <li>Empty or missing configuration content</li>
 * <li>Invalid multipart form data format</li>
 * <li>Configuration file parsing errors</li>
 * <li>Agent instantiation failures</li>
 * <li>File system errors during configuration saving</li>
 * </ul>
 * 
 * <h3>Integration with System Components</h3>
 * <ul>
 * <li><strong>GenericConfig</strong> - Used for parsing and loading configuration files</li>
 * <li><strong>TopicManagerSingleton</strong> - Manages topic lifecycle and clearing</li>
 * <li><strong>HtmlGraphWriter</strong> - Generates HTML graph visualizations</li>
 * </ul>
 * 
 * <h3>Thread Safety</h3>
 * <p>This servlet maintains a static reference to the current configuration and properly
 * handles cleanup of previous configurations. The servlet is thread-safe for concurrent
 * requests, though configuration loading is synchronized through the singleton pattern.
 * 
 * @see GenericConfig#setConfFile(String)
 * @see GenericConfig#create()
 * @see TopicManagerSingleton#clear()
 * @see HtmlGraphWriter#getGraphHTML()
 * @see Servlet#handle(RequestInfo, OutputStream)
 * 
 * @since 1.0
 */
public class ConfLoader implements Servlet {

    private static GenericConfig currentConfig = null; // Store current configuration

    /**
     * Handles HTTP POST requests containing configuration file uploads in multipart form data format.
     * This method processes the uploaded configuration file, clears existing system state, loads the
     * new configuration, and returns an HTML graph visualization.
     * 
     * <p>The method performs the following operations in sequence:
     * <ol>
     * <li>Extracts and validates multipart form data from the request</li>
     * <li>Parses filename and file content from the multipart data</li>
     * <li>Saves the configuration file to the config_files directory</li>
     * <li>Closes any existing configuration and clears all topics</li>
     * <li>Creates a new GenericConfig instance and loads the configuration</li>
     * <li>Generates HTML graph visualization using HtmlGraphWriter</li>
     * <li>Returns the HTML response with embedded refresh scripts</li>
     * </ol>
     * 
     * <h3>Request Processing</h3>
     * <p>The method expects the request to contain multipart/form-data with a file field.
     * The file content is extracted using custom multipart parsing logic that handles
     * boundary detection and content extraction.
     * 
     * <h3>System State Management</h3>
     * <p>Before loading a new configuration, the method:
     * <ul>
     * <li>Closes the previous GenericConfig instance if it exists</li>
     * <li>Clears all topics from TopicManagerSingleton</li>
     * <li>Verifies that the clearing operation was successful</li>
     * </ul>
     * 
     * <h3>Error Handling</h3>
     * <p>The method handles various error conditions gracefully:
     * <ul>
     * <li>Empty or missing request content</li>
     * <li>Invalid multipart form data structure</li>
     * <li>Configuration file parsing errors</li>
     * <li>Agent instantiation failures</li>
     * <li>File system I/O errors</li>
     * </ul>
     * All errors result in HTML error pages being returned to the client.
     * 
     * @param ri the RequestInfo containing HTTP request data including multipart content
     * @param toClient the OutputStream to write the HTTP response to
     * @throws IOException if there are errors writing to the output stream or file system operations
     * 
     * @see #extractFilename(String)
     * @see #extractFileContent(String)
     * @see #saveConfigFile(String, String)
     * @see GenericConfig#create()
     * @see HtmlGraphWriter#getGraphHTML()
     */
    @Override
    public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        System.out.println("[DEBUG] ConfLoader invoked");

        try {
            // Parse multipart form data
            String content = new String(ri.getContent(), StandardCharsets.UTF_8);

            if (content == null || content.trim().isEmpty()) {
                sendErrorResponse(toClient, "No configuration content received");
                return;
            }

            // Extract filename and file content from multipart data
            String filename = extractFilename(content);
            String fileContent = extractFileContent(content);

            if (filename == null || fileContent == null) {
                sendErrorResponse(toClient, "Could not parse multipart form data");
                return;
            }

            System.out.println("[DEBUG] Processing config file: " + filename);
            System.out.println("[DEBUG] Content preview: " + fileContent.substring(0, Math.min(100, fileContent.length())));

            // Save file content on server side
            String savedFilePath = saveConfigFile(filename, fileContent);

            // Close previous configuration if exists
            if (currentConfig != null) {
                currentConfig.close();
            }

            // Clear all existing topics before loading new configuration
            int topicsBefore = TopicManagerSingleton.get().getTopics().size();
            System.out.println("[DEBUG] Before clear - topics: " + topicsBefore);
            
            // Print current topic values before clearing
            for (var topic : TopicManagerSingleton.get().getTopics()) {
                System.out.println("[DEBUG] Topic '" + topic.name + "' has value: " + topic.getLastValue());
            }
            
            TopicManagerSingleton.get().clear();
            int topicsAfter = TopicManagerSingleton.get().getTopics().size();
            System.out.println("[DEBUG] After clear - topics: " + topicsAfter);
            
            // Verify topics are actually cleared
            if (topicsAfter == 0) {
                System.out.println("[DEBUG] ✅ Topics successfully cleared");
            } else {
                System.out.println("[DEBUG] ❌ WARNING: Topics not fully cleared!");
            }

            // Load GenericConfig and create agents
            currentConfig = new GenericConfig();
            currentConfig.setConfFile(savedFilePath);
            currentConfig.create();

            System.out.println("[DEBUG] Configuration loaded successfully");

            // Generate HTML visualization of the graph using HtmlGraphWriter
            String graphHtml = HtmlGraphWriter.getGraphHTML();
            
            // Add script to refresh any open topic table windows
            String enhancedGraphHtml = addTopicTableRefreshScript(graphHtml);
            sendHtmlResponse(toClient, enhancedGraphHtml);
            
            System.out.println("[DEBUG] Configuration loaded - topics reset and graph generated");

        } catch (Exception e) {
            System.err.println("[ERROR] ConfLoader error: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(toClient, "Error processing configuration: " + e.getMessage());
        }
    }

    /**
     * Extracts the filename from multipart form data by parsing the Content-Disposition header.
     * This method searches for the filename parameter in the Content-Disposition header within
     * the multipart form data structure.
     * 
     * <p>The method looks for headers in the format:
     * {@code Content-Disposition: form-data; name="file"; filename="config.txt"}
     * 
     * @param content the raw multipart form data content as a string
     * @return the extracted filename, or "uploaded_config.txt" as a default if no filename is found
     * 
     * @see #extractFileContent(String)
     */
    private String extractFilename(String content) {
        // Look for Content-Disposition header with filename
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.contains("Content-Disposition") && line.contains("filename")) {
                // Extract filename from: Content-Disposition: form-data; name="file"; filename="config.txt"
                int filenameStart = line.indexOf("filename=\"");
                if (filenameStart != -1) {
                    filenameStart += 10; // length of "filename=\""
                    int filenameEnd = line.indexOf("\"", filenameStart);
                    if (filenameEnd != -1) {
                        return line.substring(filenameStart, filenameEnd);
                    }
                }
            }
        }
        return "uploaded_config.txt"; // default filename
    }

    /**
     * Extracts the actual file content from multipart form data by parsing boundaries and headers.
     * This method implements a custom multipart parser that identifies file sections within the
     * multipart data structure and extracts the raw file content.
     * 
     * <p>The parsing process:
     * <ol>
     * <li>Identifies the boundary marker from the first boundary line</li>
     * <li>Locates the file section by finding Content-Disposition headers</li>
     * <li>Skips headers until an empty line indicates data start</li>
     * <li>Extracts content until the next boundary marker</li>
     * <li>Cleans up any remaining boundary markers from the content</li>
     * </ol>
     * 
     * <p>The method handles both "name=\"file\"" and "name=\"conf\"" form field names
     * to provide flexibility in form design.
     * 
     * @param content the raw multipart form data content as a string
     * @return the extracted file content as a string, or the entire content as fallback
     *         if no boundary is found
     * 
     * @see #extractFilename(String)
     */
    private String extractFileContent(String content) {
        // Find the boundary
        String boundary = null;
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.startsWith("--") && boundary == null) {
                boundary = line.trim();
                break;
            }
        }

        if (boundary == null) {
            return content; // fallback - treat entire content as file
        }

        // Find file content between boundaries
        boolean inFileSection = false;
        boolean foundFileData = false;
        StringBuilder fileContent = new StringBuilder();

        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // Skip boundary lines completely
            if (trimmedLine.startsWith("--") && trimmedLine.contains(boundary.substring(2))) {
                if (inFileSection) {
                    break; // end of file section
                }
                inFileSection = false;
                foundFileData = false;
                continue;
            }
            
            if (inFileSection && foundFileData) {
                // Only add non-boundary lines to content
                if (!trimmedLine.startsWith("--")) {
                    fileContent.append(line).append("\n");
                }
            } else if (inFileSection && line.trim().isEmpty()) {
                foundFileData = true; // empty line after headers means data starts
            } else if (line.contains("Content-Disposition") && (line.contains("name=\"file\"") || line.contains("name=\"conf\""))) {
                inFileSection = true;
            }
        }

        // Clean up the result - remove any remaining boundary markers
        String result = fileContent.toString().trim();
        String[] resultLines = result.split("\n");
        StringBuilder cleanResult = new StringBuilder();
        
        for (String line : resultLines) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("--") && !trimmed.isEmpty()) {
                cleanResult.append(line).append("\n");
            }
        }

        return cleanResult.toString().trim();
    }

    /**
     * Saves the configuration file content to the config_files directory on the server.
     * This method creates the config_files directory if it doesn't exist and writes
     * the configuration content to a file with the specified filename.
     * 
     * <p>The method uses UTF-8 encoding and overwrites any existing file with the same name.
     * The file is saved with CREATE and TRUNCATE_EXISTING options to ensure clean writes.
     * 
     * @param filename the name of the configuration file to save
     * @param content the configuration file content as a string
     * @return the absolute path of the saved configuration file
     * @throws IOException if there are errors creating directories or writing the file
     * 
     * @see GenericConfig#setConfFile(String)
     */
    private String saveConfigFile(String filename, String content) throws IOException {
        Path configDir = Paths.get("config_files");
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }

        Path configFile = configDir.resolve(filename);
        Files.write(configFile, content.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println("[DEBUG] Saved config to: " + configFile.toAbsolutePath());
        return configFile.toAbsolutePath().toString();
    }

    /**
     * Sends an HTML response to the client with proper HTTP headers.
     * This method constructs a complete HTTP response with appropriate headers
     * including Content-Type and Content-Length.
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
                "\r\n";

        toClient.write(response.getBytes(StandardCharsets.UTF_8));
        toClient.write(contentBytes);
        toClient.flush();
    }

    /**
     * Adds JavaScript code to the HTML response to refresh any open topic table windows.
     * This method injects client-side JavaScript that signals other browser windows
     * to refresh their topic displays when a new configuration is loaded.
     * 
     * <p>The script uses two mechanisms for cross-window communication:
     * <ul>
     * <li>localStorage events to signal configuration updates</li>
     * <li>postMessage API to directly communicate with opener windows</li>
     * </ul>
     * 
     * @param graphHtml the original HTML graph content
     * @return the enhanced HTML with embedded refresh script
     */
    private String addTopicTableRefreshScript(String graphHtml) {
        String refreshScript = 
            "<script>" +
            "// Signal to refresh any open topic table windows" +
            "try {" +
            "  // Use localStorage to signal other windows" +
            "  localStorage.setItem('configUpdated', Date.now());" +
            "  // Try to refresh topic table in other windows" +
            "  if (window.opener) {" +
            "    window.opener.postMessage('refreshTopicTable', '*');" +
            "  }" +
            "} catch(e) {" +
            "  console.log('Could not signal topic table refresh:', e);" +
            "}" +
            "</script>";
        
        return graphHtml.replace("</body>", refreshScript + "</body>");
    }

    /**
     * Sends an HTML error response to the client with a user-friendly error message.
     * This method creates a simple HTML page containing the error message and a
     * back button for user navigation.
     * 
     * @param toClient the OutputStream to write the error response to
     * @param message the error message to display to the user
     * @throws IOException if there are errors writing to the output stream
     * 
     * @see #sendHtmlResponse(OutputStream, String)
     */
    private void sendErrorResponse(OutputStream toClient, String message) throws IOException {
        String html = "<html><body><h3>Configuration Error</h3><p>" + message + "</p>" +
                "<a href='javascript:history.back()'>Go Back</a></body></html>";
        sendHtmlResponse(toClient, html);
    }

    @Override
    public void close() throws IOException {
        if (currentConfig != null) {
            currentConfig.close();
        }
        System.out.println("[DEBUG] ConfLoader closed");
    }
}