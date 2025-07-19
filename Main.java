import server.MyHTTPServer;
import servlets.*;

/**
 * Main class for the Computational Graph Web Server
 *
 * This server provides a web interface for:
 * - Uploading configuration files to create computational graphs
 * - Publishing messages to topics
 * - Viewing graph visualizations and topic status
 */
public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting Computational Graph Server...");

        // Create HTTP server on port 8080 with thread pool of 5
        MyHTTPServer server = new MyHTTPServer(8080, 5);

        // Register servlets for different endpoints
        server.addServlet("GET",  "/publish", new TopicDisplayer());
        server.addServlet("POST", "/upload",  new ConfLoader());
        server.addServlet("GET",  "/graph",   new GraphRefresh());
        server.addServlet("GET",  "/app/",    new HtmlLoader("html_files"));

        System.out.println("Server configured with endpoints:");
        System.out.println("  GET  /publish - Topic message publisher and status display");
        System.out.println("  POST /upload  - Configuration file upload and graph generation");
        System.out.println("  GET  /graph   - Refresh graph with current topic values");
        System.out.println("  GET  /app/    - Static HTML file server");
        System.out.println();

        // Start the server (non-blocking)
        server.start();

        System.out.println("Server started successfully on http://localhost:8080");
        System.out.println("Open your browser and navigate to: http://localhost:8080/app/index.html");
        System.out.println("Press <Enter> to stop the server...");
        System.out.println();

        // Wait for user input to stop
        System.in.read();

        // Clean shutdown
        server.close();
        System.out.println("Server stopped successfully");
        System.out.println("done");
    }
}