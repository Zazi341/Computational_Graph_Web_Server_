import server.MyHTTPServer;
import servlets.*;

/**
 * Background server version that doesn't wait for user input
 */
public class MainServer {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting Computational Graph Server in background mode...");

        MyHTTPServer server = new MyHTTPServer(8080, 5);
        
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

        server.start();

        System.out.println(" Server started successfully on http://localhost:8080");
        System.out.println(" Open your browser and navigate to: http://localhost:8080/app/index.html");
        System.out.println(" Server running in background mode...");
        System.out.println(" To stop: kill this process or use Ctrl+C");

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n Shutting down server...");
            server.close();
            System.out.println(" Server stopped successfully");
        }));

        // Keep running
        try {
            server.join();
        } catch (InterruptedException e) {
            server.close();
        }
    }
}
