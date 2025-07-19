package graph;

import java.util.Date;

//---------------------------------------------------
import java.nio.charset.StandardCharsets;
//---------------------------------------------------

/**
 * Represents a message flowing through the computational graph with multiple data representations.
 * 
 * <p>The Message class is a core data structure in the publish-subscribe system that provides
 * a unified way to handle data flowing between topics and agents. Each message maintains
 * multiple representations of the same data to support different computational needs:</p>
 * 
 * <ul>
 * <li><strong>Raw bytes:</strong> Original binary data for exact preservation</li>
 * <li><strong>Text representation:</strong> UTF-8 decoded string for human readability</li>
 * <li><strong>Numeric representation:</strong> Parsed double value for mathematical operations</li>
 * <li><strong>Timestamp:</strong> Creation time for temporal tracking</li>
 * </ul>
 * 
 * <p>This dual representation pattern allows agents to work with data in their preferred
 * format while maintaining data integrity and type safety throughout the computational
 * graph. The class is immutable and thread-safe, making it suitable for concurrent
 * message passing between agents.</p>
 * 
 * <h3>Usage Patterns:</h3>
 * 
 * <h4>Numeric Computation:</h4>
 * <pre>{@code
 * // Agent processing numeric values
 * public void callback(String topic, Message message) {
 *     if (!Double.isNaN(message.asDouble)) {
 *         double result = message.asDouble * 2.0;
 *         publishToTopic("output", new Message(result));
 *     }
 * }
 * }</pre>
 * 
 * <h4>Text Processing:</h4>
 * <pre>{@code
 * // Agent processing text data
 * public void callback(String topic, Message message) {
 *     String text = message.asText;
 *     if (text.startsWith("command:")) {
 *         processCommand(text.substring(8));
 *     }
 * }
 * }</pre>
 * 
 * <h4>Binary Data Handling:</h4>
 * <pre>{@code
 * // Agent working with raw binary data
 * public void callback(String topic, Message message) {
 *     byte[] rawData = message.data;
 *     if (rawData.length > 0) {
 *         processRawBytes(rawData);
 *     }
 * }
 * }</pre>
 * 
 * <h3>Type Conversion Strategy:</h3>
 * <p>The class automatically attempts to parse text content as numeric values.
 * If parsing fails, the {@code asDouble} field is set to {@code Double.NaN},
 * allowing agents to detect and handle non-numeric data appropriately.</p>
 * 
 * <h3>Thread Safety:</h3>
 * <p>All fields are final and the class is immutable, making Message instances
 * completely thread-safe for use in the concurrent agent system. Multiple agents
 * can safely access the same Message instance simultaneously.</p>
 * 
 * <h3>Memory Efficiency:</h3>
 * <p>The class stores data in multiple formats for performance reasons, avoiding
 * repeated conversions during agent processing. While this uses more memory per
 * message, it significantly improves processing speed in computational graphs
 * with many agents.</p>
 * 
 * @see graph.Topic#publish(Message)
 * @see graph.Agent#callback(String, Message)
 * @see graph.TopicManagerSingleton
 * @since 1.0
 */
public class Message {
    /**
     * The raw binary data of the message.
     * 
     * <p>This field contains the original byte representation of the message data,
     * preserved exactly as received. It's useful for agents that need to work with
     * binary data or when exact byte-level preservation is required.</p>
     */
    public final byte[] data;
    
    /**
     * The UTF-8 decoded text representation of the message.
     * 
     * <p>This field provides a human-readable string version of the message data,
     * automatically decoded from the raw bytes using UTF-8 encoding. It's the
     * primary representation for text-based processing and logging.</p>
     */
    public final String asText;
    
    /**
     * The numeric representation of the message as a double value.
     * 
     * <p>This field contains the parsed numeric value of the message text.
     * If the text cannot be parsed as a valid number, this field will be
     * set to {@code Double.NaN}. Agents should check {@code Double.isNaN()}
     * before using this value for mathematical operations.</p>
     * 
     * @see Double#isNaN(double)
     */
    public final double asDouble;
    
    /**
     * The timestamp when this message was created.
     * 
     * <p>This field records the exact moment when the Message instance was
     * constructed, useful for temporal analysis, debugging, and message
     * ordering in the computational graph.</p>
     */
    public final Date date;
    
    
    //---------------------------------------------------
    /**
     * Creates a new Message from raw binary data.
     * 
     * <p>This is the primary constructor that initializes all representations of the message
     * from the provided byte array. The constructor performs the following operations:
     * <ol>
     * <li>Stores the raw binary data exactly as provided</li>
     * <li>Decodes the bytes to UTF-8 text representation</li>
     * <li>Attempts to parse the text as a numeric value</li>
     * <li>Records the current timestamp</li>
     * </ol>
     * 
     * <p>This constructor is useful when receiving data from external sources or when
     * working with binary protocols where exact byte preservation is important.</p>
     * 
     * <h3>Example Usage:</h3>
     * <pre>{@code
     * // Create message from binary data
     * byte[] binaryData = "42.5".getBytes(StandardCharsets.UTF_8);
     * Message msg = new Message(binaryData);
     * 
     * // Access different representations
     * System.out.println(msg.asText);   // "42.5"
     * System.out.println(msg.asDouble); // 42.5
     * }</pre>
     * 
     * @param data the raw binary data for this message, must not be null
     * @throws NullPointerException if data is null
     * @see #Message(String)
     * @see #Message(double)
     */
    public Message(byte[] data) {
        this.data = data;
        this.asText = new String(data, StandardCharsets.UTF_8);
        this.asDouble = parseDouble(asText);
        this.date = new Date();
    }

    /**
     * Creates a new Message from a text string.
     * 
     * <p>This convenience constructor creates a message from a string by encoding it
     * to UTF-8 bytes and delegating to the primary constructor. It's the most commonly
     * used constructor for text-based data in the computational graph.</p>
     * 
     * <h3>Example Usage:</h3>
     * <pre>{@code
     * // Create message from text
     * Message textMsg = new Message("Hello World");
     * Message numericMsg = new Message("123.45");
     * 
     * // Both text and numeric parsing work
     * System.out.println(textMsg.asText);     // "Hello World"
     * System.out.println(textMsg.asDouble);   // NaN (not a number)
     * System.out.println(numericMsg.asDouble); // 123.45
     * }</pre>
     * 
     * @param text the text content for this message, must not be null
     * @throws NullPointerException if text is null
     * @see #Message(byte[])
     */
    public Message(String text) {
        this(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Creates a new Message from a numeric value.
     * 
     * <p>This convenience constructor creates a message from a double value by
     * converting it to its string representation and delegating to the string
     * constructor. It's optimized for numeric computations in the graph.</p>
     * 
     * <h3>Example Usage:</h3>
     * <pre>{@code
     * // Create message from numeric value
     * Message msg = new Message(42.5);
     * 
     * // All representations are available
     * System.out.println(msg.asText);   // "42.5"
     * System.out.println(msg.asDouble); // 42.5
     * 
     * // Useful in agent computations
     * double result = inputMessage.asDouble * 2.0;
     * publishToTopic("output", new Message(result));
     * }</pre>
     * 
     * @param value the numeric value for this message
     * @see #Message(String)
     */
    public Message(double value) {
        this(Double.toString(value));
    }

    /**
     * Safely parses a string to a double value with error handling.
     * 
     * <p>This utility method attempts to parse the provided string as a double value.
     * If parsing fails due to invalid format, it returns {@code Double.NaN} instead
     * of throwing an exception. This allows the message system to gracefully handle
     * mixed text and numeric data.</p>
     * 
     * <h3>Parsing Behavior:</h3>
     * <ul>
     * <li>Valid numbers: "42", "3.14", "-123.45" → parsed correctly</li>
     * <li>Invalid formats: "hello", "not-a-number", "" → returns NaN</li>
     * <li>Special values: "NaN", "Infinity", "-Infinity" → parsed correctly</li>
     * </ul>
     * 
     * @param s the string to parse as a double
     * @return the parsed double value, or Double.NaN if parsing fails
     * @see Double#parseDouble(String)
     * @see Double#isNaN(double)
     */
    private static double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
  //---------------------------------------------------


}
