package configs;

import graph.TopicManagerSingleton.TopicManager;

//---------------------------------------------------
import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;
//---------------------------------------------------

/**
 * Logical NOT agent that performs bitwise NOT (complement) operations on single input values.
 * 
 * <p>NotAgent implements the {@link Agent} interface to provide bitwise NOT functionality
 * in the computational graph system. Unlike other logical agents that require two inputs,
 * this agent subscribes to a single input topic and immediately publishes the bitwise NOT
 * result to an output topic upon receiving each valid message.</p>
 * 
 * <p><strong>Bitwise NOT Operation:</strong></p>
 * <ul>
 *   <li>Converts input decimal value to integer</li>
 *   <li>Performs bitwise NOT operation (~a) - flips all bits</li>
 *   <li>Converts result back to decimal format</li>
 *   <li>Publishes result immediately for each valid input</li>
 * </ul>
 * 
 * <p><strong>NOT Logic:</strong> The NOT operation flips every bit in the binary representation
 * of the input value. This is also known as the one's complement operation. For example,
 * if the input represents binary 101, the output will represent binary 010 (with sign extension
 * for negative numbers in two's complement representation).</p>
 * 
 * <p><strong>Immediate Processing:</strong> Unlike binary operation agents that wait for two
 * inputs, the NOT agent processes each message immediately as it arrives. There is no internal
 * state to maintain between messages.</p>
 * 
 * <p><strong>Configuration Example:</strong></p>
 * <pre>
 * # Configuration file format for NOT agent
 * configs.NotAgent         # Agent class name
 * input_signal            # Single input topic
 * inverted_signal         # Single output topic
 * </pre>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Configuration creates a NOT agent that:
 * // 1. Subscribes to "digital_input" topic
 * // 2. Performs bitwise NOT on each received value
 * // 3. Publishes inverted result to "digital_output" topic
 * 
 * // Example computations:
 * // Input: 5.0 (binary: 101) → Output: -6.0 (binary: ...11111010, two's complement)
 * // Input: 0.0 (binary: 000) → Output: -1.0 (binary: ...11111111, two's complement)
 * // Input: -1.0 (binary: ...11111111) → Output: 0.0 (binary: 000)
 * }</pre>
 * 
 * <p><strong>Digital Logic Applications:</strong></p>
 * <ul>
 *   <li>Signal inversion in digital circuits</li>
 *   <li>Boolean logic operations (logical NOT)</li>
 *   <li>Bit manipulation and masking operations</li>
 *   <li>Complement arithmetic in computational graphs</li>
 * </ul>
 * 
 * <p><strong>Thread Safety:</strong> This agent is thread-safe for concurrent message
 * processing. Since there is no internal state to maintain, each message is processed
 * independently without synchronization concerns.</p>
 * 
 * @see Agent
 * @see BitwiseUtils#bitwiseNot(double)
 * @see AndAgent
 * @see OrAgent
 * @see XorAgent
 * @since 1.0
 */
public class NotAgent implements Agent {
	//---------------------------------------------------
    /** The fixed name identifier for this NOT agent instance. */
    private final String The_Name;
    
    /** Array of input topic names this agent subscribes to (expects exactly 1). */
    private final String[] subs;
    
    /** Array of output topic names this agent publishes to (expects exactly 1). */
    private final String[] pubs;
    
    /**
     * Constructs a new NotAgent with the specified input and output topics.
     * 
     * <p>This constructor automatically subscribes the agent to the first input topic
     * (if available) and registers it as a publisher for the first output topic (if available).
     * The agent will begin processing messages immediately after construction.</p>
     * 
     * <p><strong>Topic Requirements:</strong></p>
     * <ul>
     *   <li>At least 1 input topic must be provided for proper operation</li>
     *   <li>At least 1 output topic must be provided for result publishing</li>
     *   <li>If insufficient topics are provided, the agent will not process messages</li>
     * </ul>
     * 
     * <p><strong>Single Input Design:</strong> Unlike binary operation agents, the NOT agent
     * only requires and uses one input topic. Additional input topics in the array are ignored.</p>
     * 
     * @param The_subs_Inp array of input topic names to subscribe to (minimum 1 required)
     * @param The_pubs_Inp array of output topic names to publish to (minimum 1 required)
     * 
     * @throws NullPointerException if The_subs_Inp or The_pubs_Inp arrays are null
     * 
     * @see TopicManagerSingleton#get()
     * @see graph.Topic#subscribe(Agent)
     * @see graph.Topic#addPublisher(Agent)
     */
    public NotAgent(String[] The_subs_Inp, String[] The_pubs_Inp) {
        this.The_Name = "NotAgent";
        this.subs = The_subs_Inp;
        this.pubs = The_pubs_Inp;

        TopicManager manager = TopicManagerSingleton.get();

        // subscribing to the first input topic (if exists)
        if (subs.length >= 1) {
            manager.getTopic(subs[0]).subscribe(this);
        }

        // registering as publisher to the first output topic (if exists)
        if (pubs.length >= 1) {
            manager.getTopic(pubs[0]).addPublisher(this);
        }
    }

    /**
     * Returns the fixed name identifier for this NOT agent.
     * 
     * @return always returns "NotAgent"
     */
    @Override
    public String getName() {
        return The_Name;
    }

    /**
     * Resets the internal state of this NOT agent.
     * 
     * <p>This method is a no-op for the NOT agent since it maintains no internal state
     * between message processing operations. Each incoming message is processed independently
     * and immediately, without any need for state management or cleanup.</p>
     * 
     * <p><strong>Stateless Design:</strong> Unlike binary operation agents that must wait
     * for multiple inputs and maintain state, the NOT agent processes each message
     * independently as it arrives.</p>
     */
    @Override
    public void reset() {
        // there is no state that needs to do a reset in this agent
    }

    /**
     * Processes incoming messages and immediately performs bitwise NOT operations.
     * 
     * <p>This method implements the core NOT gate logic with immediate processing:</p>
     * <ol>
     *   <li>Validates that both input and output topics are configured</li>
     *   <li>Validates that the received message contains a valid numeric value</li>
     *   <li>Performs bitwise NOT operation on the input value</li>
     *   <li>Immediately publishes the result to the output topic</li>
     * </ol>
     * 
     * <p><strong>Input Validation:</strong></p>
     * <ul>
     *   <li>Messages with invalid values (NaN) are silently ignored</li>
     *   <li>Processing is skipped if insufficient topics are configured</li>
     *   <li>Only the first input and output topics are used</li>
     * </ul>
     * 
     * <p><strong>Immediate Processing:</strong> Unlike binary operation agents, there is
     * no waiting or state accumulation. Each valid message triggers an immediate
     * computation and result publication.</p>
     * 
     * <p><strong>Bitwise NOT Examples:</strong></p>
     * <pre>{@code
     * // Positive numbers (showing 8-bit representation for clarity):
     * // Input: 5.0 (00000101) → Output: -6.0 (11111010 in two's complement)
     * // Input: 0.0 (00000000) → Output: -1.0 (11111111 in two's complement)
     * 
     * // Negative numbers:
     * // Input: -1.0 (11111111) → Output: 0.0 (00000000)
     * // Input: -6.0 (11111010) → Output: 5.0 (00000101)
     * 
     * // Digital logic applications:
     * // Input: 1.0 (logical HIGH) → Output: -2.0 (bitwise complement)
     * // Input: 0.0 (logical LOW) → Output: -1.0 (bitwise complement)
     * }</pre>
     * 
     * <p><strong>Two's Complement Behavior:</strong> The bitwise NOT operation follows
     * standard two's complement arithmetic, where ~n = -(n+1). This is important for
     * understanding the relationship between input and output values.</p>
     * 
     * @param topic the name of the topic that sent the message (must match subscribed topic)
     * @param msg the message containing the numeric value to invert
     * 
     * @see BitwiseUtils#bitwiseNot(double)
     * @see Message#asDouble
     */
    @Override
    public void callback(String topic, Message msg) {
    	// ignoring if input or output is missing
        if (subs.length < 1 || pubs.length < 1) return;
        
        // ignoring non-numeric messages
        if (Double.isNaN(msg.asDouble)) return;
        

        // performing bitwise NOT operation and publishing it
        double result = BitwiseUtils.bitwiseNot(msg.asDouble);
        TopicManagerSingleton.get().getTopic(pubs[0]).publish(new Message(result));
    }

    /**
     * Performs cleanup when the NOT agent is no longer needed.
     * 
     * <p>This implementation does not require any special cleanup as the agent
     * uses only managed resources through the topic management system. Topic
     * subscriptions and publisher registrations are handled automatically.</p>
     * 
     * <p><strong>Stateless Cleanup:</strong> Since the NOT agent maintains no internal
     * state or resources, cleanup is minimal and handled by the framework.</p>
     */
    @Override
    public void close() {
    	// nothing to close
    }
    
	
	//---------------------------------------------------
    
}