package configs;

import graph.TopicManagerSingleton.TopicManager;

//---------------------------------------------------
import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;
//---------------------------------------------------

/**
 * Logical AND agent that performs bitwise AND operations on two input values.
 * 
 * <p>AndAgent implements the {@link Agent} interface to provide bitwise AND functionality
 * in the computational graph system. This agent subscribes to two input topics, waits for
 * numeric values from both, and publishes the bitwise AND result to an output topic.</p>
 * 
 * <p><strong>Bitwise AND Operation:</strong></p>
 * <ul>
 *   <li>Converts input decimal values to integers</li>
 *   <li>Performs bitwise AND operation (a &amp; b)</li>
 *   <li>Converts result back to decimal format</li>
 *   <li>Publishes result only when both inputs are valid and available</li>
 * </ul>
 * 
 * <p><strong>Input Validation:</strong> The agent only processes messages with valid
 * numeric values (not NaN). Invalid inputs are ignored and the corresponding input
 * flag is reset to false.</p>
 * 
 * <p><strong>Configuration Example:</strong></p>
 * <pre>
 * # Configuration file format for AND agent
 * configs.AndAgent         # Agent class name
 * input1,input2           # Two input topics (comma-separated)
 * result                  # Single output topic
 * </pre>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Configuration creates an AND agent that:
 * // 1. Subscribes to "signal_a" and "signal_b" topics
 * // 2. Performs bitwise AND when both values are received
 * // 3. Publishes result to "and_output" topic
 * 
 * // Example computation:
 * // Input: signal_a = 5.0 (binary: 101), signal_b = 3.0 (binary: 011)
 * // Output: and_output = 1.0 (binary: 001, decimal: 1)
 * }</pre>
 * 
 * <p><strong>Thread Safety:</strong> This agent is thread-safe for concurrent message
 * processing. Input state is properly managed and synchronized through the topic system.</p>
 * 
 * @see Agent
 * @see BitwiseUtils#bitwiseAnd(double, double)
 * @see OrAgent
 * @see XorAgent
 * @since 1.0
 */
public class AndAgent implements Agent {
	//---------------------------------------------------
	/** The fixed name identifier for this AND agent instance. */
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
     * Constructs a new AndAgent with the specified input and output topics.
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
    public AndAgent(String[] subs, String[] pubs) {
        this.The_Name = "AndAgent";
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
     * Returns the fixed name identifier for this AND agent.
     * 
     * @return always returns "AndAgent"
     */
    @Override
    public String getName() {
        return The_Name;
    }

    /**
     * Resets the internal state of this AND agent by clearing input values and flags.
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
     * Processes incoming messages and performs bitwise AND operations when both inputs are available.
     * 
     * <p>This method implements the core AND gate logic:</p>
     * <ol>
     *   <li>Validates that sufficient input and output topics are configured</li>
     *   <li>Identifies which input topic sent the message and stores the value</li>
     *   <li>Validates that the received value is not NaN (resets flag if invalid)</li>
     *   <li>When both valid inputs are available, performs bitwise AND operation</li>
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
     * <p><strong>Computation Trigger:</strong> The bitwise AND operation is performed
     * only when both input flags (xSet and ySet) are true, indicating valid values
     * have been received from both input topics.</p>
     * 
     * <p><strong>Example:</strong></p>
     * <pre>{@code
     * // Input: x = 5.0 (binary: 101), y = 3.0 (binary: 011)
     * // Bitwise AND: 101 & 011 = 001
     * // Output: 1.0
     * }</pre>
     * 
     * @param topic the name of the topic that sent the message
     * @param msg the message containing the numeric value to process
     * 
     * @see BitwiseUtils#bitwiseAnd(double, double)
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
            double result = BitwiseUtils.bitwiseAnd(x, y);
            
            TopicManagerSingleton.get().getTopic(pubs[0]).publish(new Message(result));
        }
        
    }

    /**
     * Performs cleanup when the AND agent is no longer needed.
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