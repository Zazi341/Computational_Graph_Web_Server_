package servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import server.RequestParser.RequestInfo;

/**
 * HtmlLoader servlet serves static HTML files from a specified directory.
 * Handles requests that start with /app/ and serves corresponding HTML files.
 */
public class HtmlLoader implements Servlet {

    private final String htmlDirectory;

    /**
     * Constructor that accepts the directory containing HTML files
     * @param htmlDirectory The directory path containing HTML files
     */
    public HtmlLoader(String htmlDirectory) {
        this.htmlDirectory = htmlDirectory;
    }

    @Override
    public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        System.out.println("[DEBUG] HtmlLoader invoked for URI: " + ri.getUri());

        try {
            // Extract filename from URI
            // URI format: /app/filename.html
            String uri = ri.getUri();
            String filename = extractFilename(uri);

            if (filename == null) {
                sendErrorResponse(toClient, "Invalid file request", 400);
                return;
            }

            // Load the HTML file
            Path filePath = Paths.get(htmlDirectory, filename);

            if (!Files.exists(filePath)) {
                sendNotFoundResponse(toClient, filename);
                return;
            }

            // Read file content
            String htmlContent = Files.readString(filePath, StandardCharsets.UTF_8);
            sendHtmlResponse(toClient, htmlContent);

        } catch (Exception e) {
            System.err.println("[ERROR] HtmlLoader error: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(toClient, "Internal server error: " + e.getMessage(), 500);
        }
    }

    /**
     * Extract filename from URI path
     * Examples:
     * /app/index.html -> index.html
     * /app/form.html -> form.html
     */
    private String extractFilename(String uri) {
        if (uri == null || !uri.startsWith("/app/")) {
            return null;
        }

        String[] segments = uri.split("/");
        if (segments.length < 3) {
            return "index.html"; // Default file
        }

        String filename = segments[2];

        // Ensure it's an HTML file
        if (!filename.endsWith(".html")) {
            filename += ".html";
        }

        // Security check - prevent directory traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return null;
        }

        return filename;
    }

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

    private void sendNotFoundResponse(OutputStream toClient, String filename) throws IOException {
        String html = "<!DOCTYPE html><html><head><title>File Not Found</title></head><body>" +
                "<h3>404 - File Not Found</h3>" +
                "<p>The requested file '" + escapeHtml(filename) + "' was not found.</p>" +
                "<p>Available files should be placed in the '" + htmlDirectory + "' directory.</p>" +
                "<a href='/app/index.html'>Go to Index</a>" +
                "</body></html>";

        byte[] contentBytes = html.getBytes(StandardCharsets.UTF_8);
        String response = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: " + contentBytes.length + "\r\n" +
                "\r\n";

        toClient.write(response.getBytes(StandardCharsets.UTF_8));
        toClient.write(contentBytes);
        toClient.flush();
    }

    private void sendErrorResponse(OutputStream toClient, String message, int statusCode) throws IOException {
        String statusText = statusCode == 400 ? "Bad Request" : "Internal Server Error";
        String html = "<!DOCTYPE html><html><head><title>Error</title></head><body>" +
                "<h3>" + statusCode + " - " + statusText + "</h3>" +
                "<p>" + escapeHtml(message) + "</p>" +
                "<a href='javascript:history.back()'>Go Back</a>" +
                "</body></html>";

        byte[] contentBytes = html.getBytes(StandardCharsets.UTF_8);
        String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: " + contentBytes.length + "\r\n" +
                "\r\n";

        toClient.write(response.getBytes(StandardCharsets.UTF_8));
        toClient.write(contentBytes);
        toClient.flush();
    }

    /**
     * Escape HTML special characters to prevent XSS
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
        System.out.println("[DEBUG] HtmlLoader closed");
    }
}