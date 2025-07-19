package configs;

import graph.TopicManagerSingleton.TopicManager;

//---------------------------------------------------
import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;
//---------------------------------------------------

/**
 * Comparator agent that performs three-way comparison operations on two input values.
 * 
 * <p>ComparatorAgent implements the {@link Agent} interface to provide comparison functionality
 * in the computational graph system. This agent subscribes to two input topics, waits for
 * numeric values from both, and publishes a three-way comparison result to an output topic.</p>
 * 
 * <p><strong>Comparison Logic:</strong></p>
 * <ul>
 *   <li>Returns <code>1.0</code> if the first input is greater than the second (x &gt; y)</li>
 *   <li>Returns <code>-1.0</code> if the first input is less than the second (x &lt; y)</li>
 *   <li>Returns <code>0.0</code> if both inputs are equal (x == y)</li>
 * </ul>
 * 
 * <p><strong>Input Processing:</strong> The agent waits for valid numeric values from both
 * input topics before performing the comparison. Invalid values (NaN) are rejected and
 * cause the corresponding input flag to be reset.</p>
 * 
 * <p><strong>Use Cases:</strong></p>
 * <ul>
 *   <li>Threshold detection in control systems</li>
 *   <li>Signal level comparison in digital circuits</li>
 *   <li>Conditional branching in computational graphs</li>
 *   <li>Sorting and ordering operations</li>
 * </ul>
 * 
 * <p><strong>Configuration Example:</strong></p>
 * <pre>
 * # Configuration file format for Comparator agent
 * configs.ComparatorAgent  # Agent class name
 * signal,threshold        # Two input topics: value to compare, reference value
 * comparison_result       # Single output topic for comparison result
 * </pre>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Configuration creates a comparator agent that:
 * // 1. Subscribes to "temperature" and "setpoint" topics
 * // 2. Compares temperature against setpoint when both values are received
 * // 3. Publishes comparison result to "temp_status" topic
 * 
 * // Example computations:
 * // Input: temperature = 25.0, setpoint = 20.0 → Output: 1.0 (temperature > setpoint)
 * // Input: temperature = 15.0, setpoint = 20.0 → Output: -1.0 (temperature < setpoint)
 * // Input: temperature = 20.0, setpoint = 20.0 → Output: 0.0 (temperature == setpoint)
 * }</pre>
 * 
 * <p><strong>Thread Safety:</strong> This agent is thread-safe for concurrent message
 * processing. Input state is properly managed and synchronized through the topic system.</p>
 * 
 * @see Agent
 * @see Message
 * @see Double#compare(double, double)
 * @since 1.0
 */
public class ComparatorAgent implements Agent {
    //---------------------------------------------------
    /** The fixed name identifier for this Comparator agent instance. */
    private final String The_Name;
    
    /** Array of input topic names this agent subscribes to (expects exactly 2). */
    private final String[] subs;
    
    /** Array of output topic names this agent publishes to (expects exactly 1). */
    private final String[] pubs;

    /** Current value from the first input topic (left operand in comparison). */
    private double x = 0;
    
    /** Current value from the second input topic (right operand in comparison). */
    private double y = 0;
    
    /** Flag indicating whether the first input value has been explicitly set. */
    private boolean xSet = false;
    
    /** Flag indicating whether the second input value has been explicitly set. */
    private boolean ySet = false;

    /**
     * Constructs a new ComparatorAgent with the specified input and output topics.
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
     * <p><strong>Input Topic Order:</strong> The order of input topics matters for comparison:
     * the first topic (subs[0]) provides the left operand, and the second topic (subs[1])
     * provides the right operand in the comparison operation.</p>
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
    public ComparatorAgent(String[] subs, String[] pubs) {
        this.The_Name = "ComparatorAgent";
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
     * Returns the fixed name identifier for this Comparator agent.
     * 
     * @return always returns "ComparatorAgent"
     */
    @Override
    public String getName() {
        return The_Name;
    }

    /**
     * Resets the internal state of this Comparator agent by clearing input values and flags.
     * 
     * <p>This method clears both input values and resets the corresponding flags to false,
     * preparing the agent for the next comparison cycle. The agent does not automatically
     * reset after each computation, allowing for persistent state until explicitly reset.</p>
     * 
     * <p><strong>Reset Actions:</strong></p>
     * <ul>
     *   <li>Sets both input values (x, y) to 0</li>
     *   <li>Sets both input flags (xSet, ySet) to false</li>
     *   <li>Prepares agent to receive new input values for comparison</li>
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
     * Processes incoming messages and performs three-way comparison when both inputs are available.
     * 
     * <p>This method implements the core comparison logic:</p>
     * <ol>
     *   <li>Validates that sufficient input and output topics are configured</li>
     *   <li>Identifies which input topic sent the message and stores the value</li>
     *   <li>Validates that the received value is not NaN (resets flag if invalid)</li>
     *   <li>When both valid inputs are available, performs three-way comparison</li>
     *   <li>Publishes the comparison result to the output topic</li>
     * </ol>
     * 
     * <p><strong>Input Processing:</strong></p>
     * <ul>
     *   <li>Messages from the first input topic (subs[0]) are stored as the left operand (x)</li>
     *   <li>Messages from the second input topic (subs[1]) are stored as the right operand (y)</li>
     *   <li>Invalid values (NaN) cause the corresponding input flag to be reset</li>
     * </ul>
     * 
     * <p><strong>Comparison Logic:</strong> The three-way comparison follows standard conventions:</p>
     * <ul>
     *   <li><code>x > y</code> → result = 1.0 (left operand is greater)</li>
     *   <li><code>x < y</code> → result = -1.0 (left operand is smaller)</li>
     *   <li><code>x == y</code> → result = 0.0 (operands are equal)</li>
     * </ul>
     * 
     * <p><strong>Computation Trigger:</strong> The comparison operation is performed
     * only when both input flags (xSet and ySet) are true, indicating valid values
     * have been received from both input topics.</p>
     * 
     * <p><strong>Examples:</strong></p>
     * <pre>{@code
     * // Temperature monitoring example:
     * // x = 25.0 (current temperature), y = 20.0 (setpoint) → result = 1.0 (too hot)
     * // x = 15.0 (current temperature), y = 20.0 (setpoint) → result = -1.0 (too cold)
     * // x = 20.0 (current temperature), y = 20.0 (setpoint) → result = 0.0 (perfect)
     * 
     * // Signal level comparison:
     * // x = 3.3 (signal voltage), y = 2.5 (threshold) → result = 1.0 (above threshold)
     * // x = 1.8 (signal voltage), y = 2.5 (threshold) → result = -1.0 (below threshold)
     * }</pre>
     * 
     * @param topic the name of the topic that sent the message
     * @param msg the message containing the numeric value to process
     * 
     * @see Message#asDouble
     * @see Double#compare(double, double)
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
            double result;
            
            // comparison logic: 1 if x > y, -1 if x < y, 0 if x == y
            if (x > y) {
                result = 1;
            } else if (x < y) {
                result = -1;
            } else {
                result = 0;
            }
            
            TopicManagerSingleton.get().getTopic(pubs[0]).publish(new Message(result));
        }
    }

    /**
     * Performs cleanup when the Comparator agent is no longer needed.
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