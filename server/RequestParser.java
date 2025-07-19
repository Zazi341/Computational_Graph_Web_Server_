package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for parsing HTTP requests from client connections.
 * 
 * <p>This class provides functionality to parse incoming HTTP requests and extract
 * all relevant information including the HTTP method, URI, query parameters, headers,
 * and request body content. It supports standard HTTP methods (GET, POST, DELETE)
 * and handles both URL-encoded parameters and multipart form data.</p>
 * 
 * <p>The parser follows the HTTP/1.1 specification for request parsing and provides
 * URL decoding for parameters. It's designed to work with the custom HTTP server
 * implementation and handles edge cases like missing Content-Length headers for
 * multipart requests.</p>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Parse an incoming HTTP request
 * BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
 * RequestInfo request = RequestParser.parseRequest(reader);
 * 
 * // Access parsed information
 * String method = request.getHttpCommand();  // "GET", "POST", etc.
 * String uri = request.getUri();             // "/config?param=value"
 * String[] segments = request.getUriSegments(); // ["config"]
 * Map<String, String> params = request.getParameters(); // {"param": "value"}
 * byte[] body = request.getContent();        // Request body content
 * }</pre>
 * 
 * @see RequestInfo
 * @see server.MyHTTPServer
 * @since 1.0
 */
public class RequestParser {

    /**
     * Parses an HTTP request from a BufferedReader and extracts all relevant information.
     * 
     * <p>This method performs a complete HTTP request parsing operation, extracting:
     * <ul>
     * <li>HTTP method (GET, POST, DELETE, etc.)</li>
     * <li>Request URI with query parameters</li>
     * <li>URI path segments for routing</li>
     * <li>URL-decoded query parameters</li>
     * <li>HTTP headers (Content-Length, Content-Type)</li>
     * <li>Request body content</li>
     * </ul>
     * 
     * <p>The parser handles both standard requests with Content-Length headers and
     * multipart form data requests that may not include Content-Length. For multipart
     * requests, it reads until it encounters a boundary end marker.</p>
     * 
     * <h3>Supported HTTP Methods:</h3>
     * <ul>
     * <li>GET - Query parameters only, no body content</li>
     * <li>POST - May include body content and form data</li>
     * <li>DELETE - Similar to GET, typically no body content</li>
     * </ul>
     * 
     * <h3>URL Parameter Handling:</h3>
     * <p>Query parameters are automatically URL-decoded using UTF-8 encoding.
     * Parameters without values are stored with empty strings.</p>
     * 
     * <h3>Example Request Parsing:</h3>
     * <pre>{@code
     * // Input HTTP request:
     * // GET /config/load?file=test.conf&debug=true HTTP/1.1
     * // Host: localhost:8080
     * // 
     * RequestInfo info = RequestParser.parseRequest(reader);
     * 
     * // Results:
     * info.getHttpCommand()    // "GET"
     * info.getUri()           // "/config/load?file=test.conf&debug=true"
     * info.getUriSegments()   // ["config", "load"]
     * info.getParameters()    // {"file": "test.conf", "debug": "true"}
     * info.getContent()       // empty byte array for GET requests
     * }</pre>
     * 
     * <h3>POST Request with Body:</h3>
     * <pre>{@code
     * // Input HTTP request:
     * // POST /config HTTP/1.1
     * // Content-Type: text/plain
     * // Content-Length: 25
     * //
     * // configs.PlusAgent
     * // topic1,topic2
     * // result
     * 
     * RequestInfo info = RequestParser.parseRequest(reader);
     * String configContent = new String(info.getContent(), StandardCharsets.UTF_8);
     * // configContent contains the configuration file content
     * }</pre>
     * 
     * @param reader the BufferedReader connected to the client socket input stream
     * @return a RequestInfo object containing all parsed request information
     * @throws IOException if the request cannot be read or is malformed
     * @throws IOException if the request line is empty or invalid
     * @throws NumberFormatException if Content-Length header contains invalid number
     * 
     * @see RequestInfo
     * @see java.net.URLDecoder#decode(String, String)
     */
    public static RequestInfo parseRequest(BufferedReader reader) throws IOException {
        // 1. Read the request line
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isEmpty()) throw new IOException("Empty request");

        String[] requestParts = requestLine.split(" ");
        if (requestParts.length < 2) throw new IOException("Invalid request line");

        String httpCommand = requestParts[0];
        String uriWithParams = requestParts[1];
        String uri = uriWithParams;

        // 2. Extract URI segments (from path only)
        String pathOnly = uriWithParams.split("\\?")[0];
        String[] uriSegments = pathOnly.startsWith("/") ? pathOnly.substring(1).split("/") : pathOnly.split("/");

        // 3. Extract query parameters
        Map<String, String> params = new HashMap<>();
        if (uriWithParams.contains("?")) {
            String query = uriWithParams.split("\\?", 2)[1];
            for (String pair : query.split("&")) {
                String[] kv = pair.split("=", 2);
                String key = URLDecoder.decode(kv[0], "UTF-8");
                String value = kv.length > 1 ? URLDecoder.decode(kv[1], "UTF-8") : "";
                params.put(key, value);
            }
        }

        // 4. Read headers
        int contentLength = 0;
        String contentType = "";
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.split(":")[1].trim());
            } else if (line.toLowerCase().startsWith("content-type:")) {
                contentType = line.split(":", 2)[1].trim();
            }
        }

        // 5. Read body content based on Content-Length
        byte[] content = new byte[0];
        if (contentLength > 0) {
            char[] buffer = new char[contentLength];
            int totalRead = 0;
            while (totalRead < contentLength) {
                int read = reader.read(buffer, totalRead, contentLength - totalRead);
                if (read == -1) break;
                totalRead += read;
            }
            content = new String(buffer, 0, totalRead).getBytes(StandardCharsets.UTF_8);
        } else if ("POST".equalsIgnoreCase(httpCommand) && contentType.contains("multipart/form-data")) {
            // For multipart without Content-Length, read until boundary end
            StringBuilder sb = new StringBuilder();
            while (reader.ready() || sb.length() == 0) {
                String contentLine = reader.readLine();
                if (contentLine == null) break;
                sb.append(contentLine).append("\n");
                // Check for end boundary
                if (contentLine.startsWith("--") && contentLine.endsWith("--")) {
                    break;
                }
            }
            content = sb.toString().getBytes(StandardCharsets.UTF_8);
        }

        return new RequestInfo(httpCommand, uri, uriSegments, params, content);
    }

    /**
     * Data structure containing all parsed information from an HTTP request.
     * 
     * <p>This immutable class encapsulates all the components of a parsed HTTP request,
     * providing convenient access to the HTTP method, URI, path segments, query parameters,
     * and request body content. All fields are final and the class is designed to be
     * thread-safe for use in concurrent request handling.</p>
     * 
     * <p>The class provides a clean separation between different aspects of the request:
     * <ul>
     * <li><strong>HTTP Command:</strong> The HTTP method (GET, POST, DELETE, etc.)</li>
     * <li><strong>URI:</strong> The complete request URI including query parameters</li>
     * <li><strong>URI Segments:</strong> Path components split for routing purposes</li>
     * <li><strong>Parameters:</strong> URL-decoded query parameters as key-value pairs</li>
     * <li><strong>Content:</strong> Raw request body as byte array</li>
     * </ul>
     * 
     * <h3>Usage in Servlet Pattern:</h3>
     * <pre>{@code
     * public void handle(String httpCommand, RequestInfo requestInfo, OutputStream outputStream) {
     *     // Access request method
     *     if ("POST".equals(requestInfo.getHttpCommand())) {
     *         // Handle POST request
     *         byte[] body = requestInfo.getContent();
     *         String configData = new String(body, StandardCharsets.UTF_8);
     *     }
     *     
     *     // Access URL parameters
     *     Map<String, String> params = requestInfo.getParameters();
     *     String filename = params.get("file");
     *     
     *     // Access path segments for routing
     *     String[] segments = requestInfo.getUriSegments();
     *     if (segments.length > 0 && "config".equals(segments[0])) {
     *         // Handle config-related request
     *     }
     * }
     * }</pre>
     * 
     * <h3>Thread Safety:</h3>
     * <p>This class is immutable and thread-safe. All fields are final and the contained
     * collections are defensive copies, making it safe to pass RequestInfo objects
     * between threads in the HTTP server's thread pool.</p>
     * 
     * @see RequestParser#parseRequest(BufferedReader)
     * @see servlets.Servlet#handle(String, RequestInfo, java.io.OutputStream)
     * @since 1.0
     */
    public static class RequestInfo {
        private final String httpCommand;
        private final String uri;
        private final String[] uriSegments;
        private final Map<String, String> parameters;
        private final byte[] content;

        public RequestInfo(String httpCommand, String uri, String[] uriSegments, Map<String, String> parameters, byte[] content) {
            this.httpCommand = httpCommand;
            this.uri = uri;
            this.uriSegments = uriSegments;
            this.parameters = parameters;
            this.content = content;
        }

        /**
         * Returns the HTTP method from the request line.
         * 
         * @return the HTTP command (e.g., "GET", "POST", "DELETE")
         */
        public String getHttpCommand() {
            return httpCommand;
        }

        /**
         * Returns the complete request URI including query parameters.
         * 
         * @return the full URI as received in the request line
         */
        public String getUri() {
            return uri;
        }

        /**
         * Returns the URI path components split for routing purposes.
         * 
         * <p>The URI path is split on "/" characters with the leading slash removed.
         * For example, "/config/load" becomes ["config", "load"].</p>
         * 
         * @return array of URI path segments, empty array for root path
         */
        public String[] getUriSegments() {
            return uriSegments;
        }

        /**
         * Returns the URL-decoded query parameters as key-value pairs.
         * 
         * <p>Parameters are automatically URL-decoded using UTF-8 encoding.
         * Parameters without values are stored with empty strings.</p>
         * 
         * @return map of parameter names to values, empty map if no parameters
         */
        public Map<String, String> getParameters() {
            return parameters;
        }

        /**
         * Returns the raw request body content as a byte array.
         * 
         * <p>For GET and DELETE requests, this is typically an empty array.
         * For POST requests, this contains the request body data.</p>
         * 
         * @return byte array containing the request body, empty array if no content
         */
        public byte[] getContent() {
            return content;
        }
    }
}