package configs;

import graph.TopicManagerSingleton.TopicManager;

//---------------------------------------------------
import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;
//---------------------------------------------------

/**
 * Logical XOR agent that performs bitwise XOR (exclusive OR) operations on two input values.
 * 
 * <p>XorAgent implements the {@link Agent} interface to provide bitwise XOR functionality
 * in the computational graph system. This agent subscribes to two input topics, waits for
 * numeric values from both, and publishes the bitwise XOR result to an output topic.</p>
 * 
 * <p><strong>Bitwise XOR Operation:</strong></p>
 * <ul>
 *   <li>Converts input decimal values to integers</li>
 *   <li>Performs bitwise XOR operation (a ^ b)</li>
 *   <li>Converts result back to decimal format</li>
 *   <li>Publishes result only when both inputs are valid and available</li>
 * </ul>
 * 
 * <p><strong>XOR Logic:</strong> The XOR operation returns 1 for each bit position where
 * the corresponding bits of the operands are different, and 0 where they are the same.
 * This makes XOR useful for toggle operations and parity checking.</p>
 * 
 * <p><strong>Input Validation:</strong> The agent only processes messages with valid
 * numeric values (not NaN). Invalid inputs are ignored and the corresponding input
 * flag is reset to false.</p>
 * 
 * <p><strong>Configuration Example:</strong></p>
 * <pre>
 * # Configuration file format for XOR agent
 * configs.XorAgent         # Agent class name
 * input1,input2           # Two input topics (comma-separated)
 * result                  # Single output topic
 * </pre>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Configuration creates an XOR agent that:
 * // 1. Subscribes to "signal_a" and "signal_b" topics
 * // 2. Performs bitwise XOR when both values are received
 * // 3. Publishes result to "xor_output" topic
 * 
 * // Example computation:
 * // Input: signal_a = 5.0 (binary: 101), signal_b = 3.0 (binary: 011)
 * // Output: xor_output = 6.0 (binary: 110, decimal: 6)
 * }</pre>
 * 
 * <p><strong>Thread Safety:</strong> This agent is thread-safe for concurrent message
 * processing. Input state is properly managed and synchronized through the topic system.</p>
 * 
 * @see Agent
 * @see BitwiseUtils#bitwiseXor(double, double)
 * @see AndAgent
 * @see OrAgent
 * @since 1.0
 */
public class XorAgent implements Agent {
	//---------------------------------------------------
	/** The fixed name identifier for this XOR agent instance. */
	private final String The_Name;
	
	/** Array of input topic names this agent subscribes to (expects exactly 2). */
    private final String[] subs;
    
    /** Array of output topic names this agent publishes to (expects exactly 1). */
    private final String[] pubs;

    /** Current value from the first input topic. */
    private double x = 0;
    
    /** Current value from the second input topic. */
    private double y = 0;
    
    /** Flag indicating whether the first input value has been explicitly set. */
    private boolean xSet = false;
    
    /** Flag indicating whether the second input value has been explicitly set. */
    private boolean ySet = false;

    /**
     * Constructs a new XorAgent with the specified input and output topics.
     * 
     * <p>This constructor automatically subscribes the agent to the first two input topics
     * (if available) and registers it as a publisher for the first output topic (if available).
     * The agent will begin processing messages immediately after construction.</p>
     * 
     * <p><strong>Topic Requirements:</strong></p>
     * <ul>
     *   <li>At least 2 input topics must be provided for proper operation</li>
     *   <li>At least 1 output topic must be provided for result publishing</li>
     *   <li>If insufficient topics are provided, the agent will not process messages</li>
     * </ul>
     * 
     * @param subs array of input topic names to subscribe to (minimum 2 required)
     * @param pubs array of output topic names to publish to (minimum 1 required)
     * 
     * @throws NullPointerException if subs or pubs arrays are null
     * 
     * @see TopicManagerSingleton#get()
     * @see graph.Topic#subscribe(Agent)
     * @see graph.Topic#addPublisher(Agent)
     */
    public XorAgent(String[] subs, String[] pubs) {
        this.The_Name = "XorAgent";
        this.subs = subs;
        this.pubs = pubs;

        TopicManager manager = TopicManagerSingleton.get();

        // subscribing to two input topics if available
        if (subs.length >= 2) {
            manager.getTopic(subs[0]).subscribe(this);
            manager.getTopic(subs[1]).subscribe(this);
        }

        // registering as publisher to the output topic
        if (pubs.length >= 1) {
            manager.getTopic(pubs[0]).addPublisher(this);
        }
    }

    /**
     * Returns the fixed name identifier for this XOR agent.
     * 
     * @return always returns "XorAgent"
     */
    @Override
    public String getName() {
        return The_Name;
    }

    /**
     * Resets the internal state of this XOR agent by clearing input values and flags.
     * 
     * <p>This method clears both input values and resets the corresponding flags to false,
     * preparing the agent for the next computation cycle. Unlike BinOpAgent, this agent
     * does not automatically reset after each computation, allowing for persistent state
     * until explicitly reset.</p>
     * 
     * <p><strong>Reset Actions:</strong></p>
     * <ul>
     *   <li>Sets both input values (x, y) to 0</li>
     *   <li>Sets both input flags (xSet, ySet) to false</li>
     *   <li>Prepares agent to receive new input values</li>
     * </ul>
     */
    @Override
    public void reset() {
    	// resetting internal values
        x = 0;
        y = 0;
        
        xSet = false;
        ySet = false;
    }

    /**
     * Processes incoming messages and performs bitwise XOR operations when both inputs are available.
     * 
     * <p>This method implements the core XOR gate logic:</p>
     * <ol>
     *   <li>Validates that sufficient input and output topics are configured</li>
     *   <li>Identifies which input topic sent the message and stores the value</li>
     *   <li>Validates that the received value is not NaN (resets flag if invalid)</li>
     *   <li>When both valid inputs are available, performs bitwise XOR operation</li>
     *   <li>Publishes the result to the output topic</li>
     * </ol>
     * 
     * <p><strong>Input Processing:</strong></p>
     * <ul>
     *   <li>Messages from the first input topic (subs[0]) are stored in variable x</li>
     *   <li>Messages from the second input topic (subs[1]) are stored in variable y</li>
     *   <li>Invalid values (NaN) cause the corresponding input flag to be reset</li>
     * </ul>
     * 
     * <p><strong>Computation Trigger:</strong> The bitwise XOR operation is performed
     * only when both input flags (xSet and ySet) are true, indicating valid values
     * have been received from both input topics.</p>
     * 
     * <p><strong>XOR Truth Table:</strong></p>
     * <pre>
     * Input A | Input B | Output
     * --------|---------|-------
     *    0    |    0    |   0
     *    0    |    1    |   1
     *    1    |    0    |   1
     *    1    |    1    |   0
     * </pre>
     * 
     * <p><strong>Example:</strong></p>
     * <pre>{@code
     * // Input: x = 5.0 (binary: 101), y = 3.0 (binary: 011)
     * // Bitwise XOR: 101 ^ 011 = 110
     * // Output: 6.0
     * }</pre>
     * 
     * @param topic the name of the topic that sent the message
     * @param msg the message containing the numeric value to process
     * 
     * @see BitwiseUtils#bitwiseXor(double, double)
     * @see Message#asDouble
     */
    @Override
    public void callback(String topic, Message msg) {
        if (subs.length < 2 || pubs.length < 1) return;

        if (topic.equals(subs[0])) {
            x = msg.asDouble;
            xSet = true;
        } 
        if (Double.isNaN(x)){
            xSet = false;
        }
        
        if (topic.equals(subs[1])) {
            y = msg.asDouble;
            ySet = true;
        }
        if (Double.isNaN(y)){
            ySet = false;
        }

        // only publishing when both values have been set explicitly
        if (xSet && ySet) {
            double result = BitwiseUtils.bitwiseXor(x, y);
            
            TopicManagerSingleton.get().getTopic(pubs[0]).publish(new Message(result));
        }
        
    }

    /**
     * Performs cleanup when the XOR agent is no longer needed.
     * 
     * <p>This implementation does not require any special cleanup as the agent
     * uses only managed resources through the topic management system. Topic
     * subscriptions and publisher registrations are handled automatically.</p>
     */
    @Override
    public void close() {
    	// nothing to close
    }
	
	//---------------------------------------------------
    
}