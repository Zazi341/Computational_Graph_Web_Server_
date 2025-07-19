package graph;

/**
 * Agent defines the contract for computational units in the publish-subscribe graph system.
 * 
 * <p>Agents are the core processing elements that subscribe to topics, receive messages,
 * perform computations, and publish results to other topics. They implement the observer
 * pattern where agents are notified via callbacks when messages are published to topics
 * they subscribe to.
 * 
 * <h3>Agent Lifecycle</h3>
 * <p>The typical agent lifecycle follows this pattern:
 * <ol>
 * <li><strong>Creation:</strong> Agent is instantiated with input/output topic arrays</li>
 * <li><strong>Registration:</strong> Agent subscribes to input topics and registers as publisher for output topics</li>
 * <li><strong>Processing:</strong> Agent receives callbacks when messages arrive on subscribed topics</li>
 * <li><strong>Reset:</strong> Agent state can be reset to initial values via {@link #reset()}</li>
 * <li><strong>Cleanup:</strong> Agent resources are cleaned up via {@link #close()}</li>
 * </ol>
 * 
 * <h3>Message Processing Pattern</h3>
 * <p>Agents typically follow these patterns for message processing:
 * <ul>
 * <li><strong>Stateful Processing:</strong> Agents maintain internal state and wait for multiple inputs before producing output</li>
 * <li><strong>Stateless Processing:</strong> Agents immediately process and forward each received message</li>
 * <li><strong>Conditional Publishing:</strong> Agents only publish when certain conditions are met (e.g., all inputs received)</li>
 * </ul>
 * 
 * <h3>Thread Safety Considerations</h3>
 * <p>Agent implementations must be thread-safe as callbacks can be invoked concurrently
 * from multiple threads when messages are published to subscribed topics. Consider:
 * <ul>
 * <li>Use volatile fields for simple state variables</li>
 * <li>Use synchronized blocks for complex state updates</li>
 * <li>Avoid blocking operations in callback methods</li>
 * <li>Handle concurrent access to shared resources properly</li>
 * </ul>
 * 
 * <h3>Complete Agent Development Examples</h3>
 * 
 * <h4>Basic Binary Operation Agent</h4>
 * <pre>{@code
 * // Simple addition agent that waits for two inputs
 * public class AddAgent implements Agent {
 *     private final String name;
 *     private final String[] inputTopics;
 *     private final String[] outputTopics;
 *     private volatile double value1 = 0;
 *     private volatile double value2 = 0;
 *     private volatile boolean value1Set = false;
 *     private volatile boolean value2Set = false;
 *     
 *     public AddAgent(String[] inputs, String[] outputs) {
 *         this.name = "AddAgent";
 *         this.inputTopics = inputs;
 *         this.outputTopics = outputs;
 *         
 *         // Subscribe to input topics
 *         TopicManager manager = TopicManagerSingleton.get();
 *         if (inputs.length >= 2) {
 *             manager.getTopic(inputs[0]).subscribe(this);
 *             manager.getTopic(inputs[1]).subscribe(this);
 *         }
 *         
 *         // Register as publisher for output topic
 *         if (outputs.length >= 1) {
 *             manager.getTopic(outputs[0]).addPublisher(this);
 *         }
 *     }
 *     
 *     @Override
 *     public String getName() {
 *         return name;
 *     }
 *     
 *     @Override
 *     public void reset() {
 *         value1 = 0;
 *         value2 = 0;
 *         value1Set = false;
 *         value2Set = false;
 *     }
 *     
 *     @Override
 *     public void callback(String topic, Message msg) {
 *         // Validate configuration
 *         if (inputTopics.length < 2 || outputTopics.length < 1) return;
 *         
 *         // Process input based on topic
 *         if (topic.equals(inputTopics[0])) {
 *             value1 = msg.asDouble;
 *             value1Set = !Double.isNaN(value1);
 *         } else if (topic.equals(inputTopics[1])) {
 *             value2 = msg.asDouble;
 *             value2Set = !Double.isNaN(value2);
 *         }
 *         
 *         // Publish result when both values are available
 *         if (value1Set && value2Set) {
 *             double result = value1 + value2;
 *             TopicManagerSingleton.get().getTopic(outputTopics[0])
 *                 .publish(new Message(result));
 *         }
 *     }
 *     
 *     @Override
 *     public void close() {
 *         // No resources to clean up
 *     }
 * }
 * }</pre>
 * 
 * <h4>Immediate Processing Agent (Unary Operation)</h4>
 * <pre>{@code
 * // Square root agent that processes each input immediately
 * public class SquareRootAgent implements Agent {
 *     private final String name;
 *     private final String[] inputTopics;
 *     private final String[] outputTopics;
 *     
 *     public SquareRootAgent(String[] inputs, String[] outputs) {
 *         this.name = "SquareRootAgent";
 *         this.inputTopics = inputs;
 *         this.outputTopics = outputs;
 *         
 *         TopicManager manager = TopicManagerSingleton.get();
 *         
 *         // Subscribe to single input topic
 *         if (inputs.length >= 1) {
 *             manager.getTopic(inputs[0]).subscribe(this);
 *         }
 *         
 *         // Register as publisher for output topic
 *         if (outputs.length >= 1) {
 *             manager.getTopic(outputs[0]).addPublisher(this);
 *         }
 *     }
 *     
 *     @Override
 *     public String getName() {
 *         return name;
 *     }
 *     
 *     @Override
 *     public void reset() {
 *         // No state to reset for immediate processing agent
 *     }
 *     
 *     @Override
 *     public void callback(String topic, Message msg) {
 *         // Validate configuration
 *         if (inputTopics.length < 1 || outputTopics.length < 1) return;
 *         
 *         // Validate input value
 *         if (Double.isNaN(msg.asDouble) || msg.asDouble < 0) {
 *             return; // Skip invalid or negative values
 *         }
 *         
 *         // Process immediately and publish result
 *         double result = Math.sqrt(msg.asDouble);
 *         TopicManagerSingleton.get().getTopic(outputTopics[0])
 *             .publish(new Message(result));
 *     }
 *     
 *     @Override
 *     public void close() {
 *         // No resources to clean up
 *     }
 * }
 * }</pre>
 * 
 * <h4>Stateful Accumulator Agent</h4>
 * <pre>{@code
 * // Running average agent that maintains state across multiple inputs
 * public class RunningAverageAgent implements Agent {
 *     private final String name;
 *     private final String[] inputTopics;
 *     private final String[] outputTopics;
 *     private final Object stateLock = new Object();
 *     
 *     private double sum = 0;
 *     private int count = 0;
 *     private final int windowSize;
 *     
 *     public RunningAverageAgent(String[] inputs, String[] outputs) {
 *         this(inputs, outputs, 10); // Default window size of 10
 *     }
 *     
 *     public RunningAverageAgent(String[] inputs, String[] outputs, int windowSize) {
 *         this.name = "RunningAverageAgent";
 *         this.inputTopics = inputs;
 *         this.outputTopics = outputs;
 *         this.windowSize = windowSize;
 *         
 *         TopicManager manager = TopicManagerSingleton.get();
 *         
 *         // Subscribe to input topic
 *         if (inputs.length >= 1) {
 *             manager.getTopic(inputs[0]).subscribe(this);
 *         }
 *         
 *         // Register as publisher for output topic
 *         if (outputs.length >= 1) {
 *             manager.getTopic(outputs[0]).addPublisher(this);
 *         }
 *     }
 *     
 *     @Override
 *     public String getName() {
 *         return name;
 *     }
 *     
 *     @Override
 *     public void reset() {
 *         synchronized (stateLock) {
 *             sum = 0;
 *             count = 0;
 *         }
 *     }
 *     
 *     @Override
 *     public void callback(String topic, Message msg) {
 *         // Validate configuration
 *         if (inputTopics.length < 1 || outputTopics.length < 1) return;
 *         
 *         // Validate input value
 *         if (Double.isNaN(msg.asDouble)) return;
 *         
 *         synchronized (stateLock) {
 *             // Add new value to running sum
 *             sum += msg.asDouble;
 *             count++;
 *             
 *             // Reset if window size exceeded
 *             if (count > windowSize) {
 *                 sum = msg.asDouble;
 *                 count = 1;
 *             }
 *             
 *             // Calculate and publish running average
 *             double average = sum / count;
 *             TopicManagerSingleton.get().getTopic(outputTopics[0])
 *                 .publish(new Message(average));
 *         }
 *     }
 *     
 *     @Override
 *     public void close() {
 *         // No resources to clean up
 *     }
 * }
 * }</pre>
 * 
 * <h4>Multi-Input Conditional Agent</h4>
 * <pre>{@code
 * // Threshold monitor that requires three inputs: value, threshold, and enable flag
 * public class ThresholdMonitorAgent implements Agent {
 *     private final String name;
 *     private final String[] inputTopics;
 *     private final String[] outputTopics;
 *     
 *     private volatile double currentValue = 0;
 *     private volatile double threshold = 0;
 *     private volatile boolean enabled = false;
 *     private volatile boolean valueSet = false;
 *     private volatile boolean thresholdSet = false;
 *     private volatile boolean enabledSet = false;
 *     
 *     public ThresholdMonitorAgent(String[] inputs, String[] outputs) {
 *         this.name = "ThresholdMonitorAgent";
 *         this.inputTopics = inputs;
 *         this.outputTopics = outputs;
 *         
 *         TopicManager manager = TopicManagerSingleton.get();
 *         
 *         // Subscribe to three input topics: value, threshold, enable
 *         if (inputs.length >= 3) {
 *             manager.getTopic(inputs[0]).subscribe(this); // value
 *             manager.getTopic(inputs[1]).subscribe(this); // threshold
 *             manager.getTopic(inputs[2]).subscribe(this); // enable flag
 *         }
 *         
 *         // Register as publisher for output topic
 *         if (outputs.length >= 1) {
 *             manager.getTopic(outputs[0]).addPublisher(this);
 *         }
 *     }
 *     
 *     @Override
 *     public String getName() {
 *         return name;
 *     }
 *     
 *     @Override
 *     public void reset() {
 *         currentValue = 0;
 *         threshold = 0;
 *         enabled = false;
 *         valueSet = false;
 *         thresholdSet = false;
 *         enabledSet = false;
 *     }
 *     
 *     @Override
 *     public void callback(String topic, Message msg) {
 *         // Validate configuration
 *         if (inputTopics.length < 3 || outputTopics.length < 1) return;
 *         
 *         // Process input based on topic
 *         if (topic.equals(inputTopics[0])) { // value topic
 *             currentValue = msg.asDouble;
 *             valueSet = !Double.isNaN(currentValue);
 *         } else if (topic.equals(inputTopics[1])) { // threshold topic
 *             threshold = msg.asDouble;
 *             thresholdSet = !Double.isNaN(threshold);
 *         } else if (topic.equals(inputTopics[2])) { // enable topic
 *             enabled = msg.asDouble != 0; // Non-zero means enabled
 *             enabledSet = !Double.isNaN(msg.asDouble);
 *         }
 *         
 *         // Publish result when all inputs are available and monitoring is enabled
 *         if (valueSet && thresholdSet && enabledSet && enabled) {
 *             // 1.0 if above threshold, -1.0 if below, 0.0 if equal
 *             double result = Double.compare(currentValue, threshold);
 *             TopicManagerSingleton.get().getTopic(outputTopics[0])
 *                 .publish(new Message(result));
 *         }
 *     }
 *     
 *     @Override
 *     public void close() {
 *         // No resources to clean up
 *     }
 * }
 * }</pre>
 * 
 * <h4>Resource-Managing Agent with File I/O</h4>
 * <pre>{@code
 * // Data logger agent that writes received values to a file
 * public class DataLoggerAgent implements Agent {
 *     private final String name;
 *     private final String[] inputTopics;
 *     private final String[] outputTopics;
 *     private final String logFilePath;
 *     private PrintWriter logWriter;
 *     private final Object writerLock = new Object();
 *     
 *     public DataLoggerAgent(String[] inputs, String[] outputs) {
 *         this(inputs, outputs, "data_log.txt");
 *     }
 *     
 *     public DataLoggerAgent(String[] inputs, String[] outputs, String logFilePath) {
 *         this.name = "DataLoggerAgent";
 *         this.inputTopics = inputs;
 *         this.outputTopics = outputs;
 *         this.logFilePath = logFilePath;
 *         
 *         // Initialize log file
 *         try {
 *             logWriter = new PrintWriter(new FileWriter(logFilePath, true));
 *             logWriter.println("# Data log started at " + new Date());
 *             logWriter.flush();
 *         } catch (IOException e) {
 *             System.err.println("Failed to create log file: " + e.getMessage());
 *         }
 *         
 *         TopicManager manager = TopicManagerSingleton.get();
 *         
 *         // Subscribe to input topic
 *         if (inputs.length >= 1) {
 *             manager.getTopic(inputs[0]).subscribe(this);
 *         }
 *         
 *         // Register as publisher for output topic (pass-through)
 *         if (outputs.length >= 1) {
 *             manager.getTopic(outputs[0]).addPublisher(this);
 *         }
 *     }
 *     
 *     @Override
 *     public String getName() {
 *         return name;
 *     }
 *     
 *     @Override
 *     public void reset() {
 *         synchronized (writerLock) {
 *             if (logWriter != null) {
 *                 logWriter.println("# Reset at " + new Date());
 *                 logWriter.flush();
 *             }
 *         }
 *     }
 *     
 *     @Override
 *     public void callback(String topic, Message msg) {
 *         // Validate configuration
 *         if (inputTopics.length < 1) return;
 *         
 *         // Log the received value
 *         synchronized (writerLock) {
 *             if (logWriter != null) {
 *                 long timestamp = System.currentTimeMillis();
 *                 logWriter.printf("%d,%s,%.6f,\"%s\"%n", 
 *                     timestamp, topic, msg.asDouble, msg.asText);
 *                 logWriter.flush();
 *             }
 *         }
 *         
 *         // Pass through to output topic if configured
 *         if (outputTopics.length >= 1) {
 *             TopicManagerSingleton.get().getTopic(outputTopics[0])
 *                 .publish(msg); // Pass through original message
 *         }
 *     }
 *     
 *     @Override
 *     public void close() {
 *         synchronized (writerLock) {
 *             if (logWriter != null) {
 *                 logWriter.println("# Data log ended at " + new Date());
 *                 logWriter.close();
 *                 logWriter = null;
 *             }
 *         }
 *     }
 * }
 * }</pre>
 * 
 * <h4>Agent Development Best Practices</h4>
 * 
 * <h5>1. Constructor Pattern</h5>
 * <pre>{@code
 * public MyAgent(String[] inputs, String[] outputs) {
 *     // 1. Store configuration
 *     this.inputTopics = inputs;
 *     this.outputTopics = outputs;
 *     
 *     // 2. Initialize state
 *     reset();
 *     
 *     // 3. Subscribe to topics
 *     TopicManager manager = TopicManagerSingleton.get();
 *     for (int i = 0; i < Math.min(inputs.length, expectedInputs); i++) {
 *         manager.getTopic(inputs[i]).subscribe(this);
 *     }
 *     
 *     // 4. Register as publisher
 *     for (int i = 0; i < Math.min(outputs.length, expectedOutputs); i++) {
 *         manager.getTopic(outputs[i]).addPublisher(this);
 *     }
 * }
 * }</pre>
 * 
 * <h5>2. Thread-Safe State Management</h5>
 * <pre>{@code
 * // Use volatile for simple flags and values
 * private volatile boolean inputReady = false;
 * private volatile double lastValue = 0;
 * 
 * // Use synchronized blocks for complex state updates
 * private final Object stateLock = new Object();
 * 
 * public void callback(String topic, Message msg) {
 *     synchronized (stateLock) {
 *         // Update complex state safely
 *         updateInternalState(msg);
 *         
 *         if (readyToPublish()) {
 *             publishResult();
 *         }
 *     }
 * }
 * }</pre>
 * 
 * <h5>3. Input Validation Pattern</h5>
 * <pre>{@code
 * public void callback(String topic, Message msg) {
 *     // 1. Validate configuration
 *     if (inputTopics.length < requiredInputs || outputTopics.length < requiredOutputs) {
 *         return;
 *     }
 *     
 *     // 2. Validate message content
 *     if (Double.isNaN(msg.asDouble)) {
 *         return; // or handle invalid input appropriately
 *     }
 *     
 *     // 3. Process the message
 *     processMessage(topic, msg);
 * }
 * }</pre>
 * 
 * <h5>4. Error Handling and Logging</h5>
 * <pre>{@code
 * public void callback(String topic, Message msg) {
 *     try {
 *         // Process message
 *         processMessage(topic, msg);
 *         
 *     } catch (Exception e) {
 *         System.err.println("[ERROR] " + getName() + " failed to process message from " 
 *             + topic + ": " + e.getMessage());
 *         // Don't rethrow - let other agents continue processing
 *     }
 * }
 * 
 * public void close() {
 *     try {
 *         // Clean up resources
 *         cleanupResources();
 *     } catch (Exception e) {
 *         System.err.println("[ERROR] " + getName() + " cleanup failed: " + e.getMessage());
 *     }
 * }
 * }</pre>
 * 
 * <h5>5. Testing Strategy</h5>
 * <pre>{@code
 * // Unit test example for agent development
 * public class MyAgentTest {
 *     @Test
 *     public void testBasicOperation() {
 *         // Create test topics
 *         String[] inputs = {"test_input1", "test_input2"};
 *         String[] outputs = {"test_output"};
 *         
 *         // Create agent
 *         MyAgent agent = new MyAgent(inputs, outputs);
 *         
 *         // Create test subscriber to capture output
 *         TestSubscriber subscriber = new TestSubscriber();
 *         TopicManagerSingleton.get().getTopic(outputs[0]).subscribe(subscriber);
 *         
 *         // Send test messages
 *         TopicManagerSingleton.get().getTopic(inputs[0]).publish(new Message(5.0));
 *         TopicManagerSingleton.get().getTopic(inputs[1]).publish(new Message(3.0));
 *         
 *         // Verify output
 *         assertEquals(8.0, subscriber.getLastMessage().asDouble, 0.001);
 *         
 *         // Clean up
 *         agent.close();
 *     }
 * }
 * }</pre>
 * 
 * @see Topic
 * @see Message
 * @see graph.TopicManagerSingleton
 * @since 1.0
 */
public interface Agent {
    
    /**
     * Returns the human-readable name of this agent for identification and debugging.
     * 
     * <p>The name should be descriptive and help identify the agent's purpose in logs
     * and debugging output. Common naming patterns include the agent type (e.g., "PlusAgent",
     * "AndAgent") or a combination of type and instance identifier.
     * 
     * <p>This method should return a consistent value throughout the agent's lifecycle
     * and should not change after the agent is created.
     * 
     * @return the agent's name, never null or empty
     */
    String getName();
    
    /**
     * Resets the agent's internal state to its initial values.
     * 
     * <p>This method is called to clear any accumulated state and return the agent
     * to its initial condition. For stateful agents, this typically means:
     * <ul>
     * <li>Clearing cached input values</li>
     * <li>Resetting computation flags (e.g., "value set" indicators)</li>
     * <li>Clearing any intermediate calculation results</li>
     * <li>Returning counters or accumulators to zero</li>
     * </ul>
     * 
     * <p>For stateless agents that don't maintain internal state, this method
     * may be a no-op but should still be implemented.
     * 
     * <p>This method should be thread-safe as it may be called concurrently
     * with callback invocations.
     * 
     * <h3>Implementation Example</h3>
     * <pre>{@code
     * public void reset() {
     *     // Reset input values
     *     value1 = 0;
     *     value2 = 0;
     *     
     *     // Reset state flags
     *     value1Set = false;
     *     value2Set = false;
     *     
     *     // Clear any cached results
     *     lastResult = null;
     * }
     * }</pre>
     * 
     * @see #callback(String, Message)
     */
    void reset();
    
    /**
     * Handles incoming messages from subscribed topics and performs agent-specific processing.
     * 
     * <p>This method is the core of the agent's functionality and is called whenever
     * a message is published to a topic that this agent has subscribed to. The agent
     * should process the message according to its computational logic and may publish
     * results to output topics.
     * 
     * <p><strong>Thread Safety:</strong> This method must be thread-safe as it can be
     * called concurrently from multiple threads when messages are published simultaneously
     * to different subscribed topics.
     * 
     * <p><strong>Performance:</strong> This method should complete quickly and avoid
     * blocking operations. Long-running computations should be delegated to background
     * threads if necessary.
     * 
     * <h3>Common Processing Patterns</h3>
     * <ul>
     * <li><strong>Immediate Processing:</strong> Process and forward the message immediately</li>
     * <li><strong>Accumulative Processing:</strong> Store the value and wait for other inputs</li>
     * <li><strong>Conditional Processing:</strong> Only process when certain conditions are met</li>
     * <li><strong>Validation:</strong> Check for valid numeric values using {@code Double.isNaN()}</li>
     * </ul>
     * 
     * <h3>Message Handling Example</h3>
     * <pre>{@code
     * public void callback(String topic, Message msg) {
     *     // Validate input
     *     if (Double.isNaN(msg.asDouble)) {
     *         return; // Skip invalid numeric values
     *     }
     *     
     *     // Process based on topic
     *     if (topic.equals(inputTopic1)) {
     *         processFirstInput(msg.asDouble);
     *     } else if (topic.equals(inputTopic2)) {
     *         processSecondInput(msg.asDouble);
     *     }
     *     
     *     // Publish result if ready
     *     if (readyToPublish()) {
     *         double result = computeResult();
     *         TopicManagerSingleton.get().getTopic(outputTopic)
     *             .publish(new Message(result));
     *     }
     * }
     * }</pre>
     * 
     * @param topic the name of the topic that received the message. This allows agents
     *              subscribed to multiple topics to distinguish the source of the message.
     *              Never null.
     * @param msg the message that was published to the topic. Contains the data in multiple
     *            formats (text, numeric, binary). Never null.
     * 
     * @see Message
     * @see Topic#publish(Message)
     * @see Topic#subscribe(Agent)
     */
    void callback(String topic, Message msg);
    
    /**
     * Releases any resources held by this agent and performs cleanup operations.
     * 
     * <p>This method is called during system shutdown or when the agent is being
     * removed from the computational graph. Implementations should:
     * <ul>
     * <li>Close any open files, network connections, or other resources</li>
     * <li>Stop any background threads or scheduled tasks</li>
     * <li>Unsubscribe from topics if necessary (though this is typically handled by the system)</li>
     * <li>Release any native resources or external connections</li>
     * </ul>
     * 
     * <p>For simple agents that don't hold external resources, this method may be
     * a no-op but should still be implemented.
     * 
     * <p>This method should be idempotent - calling it multiple times should not
     * cause errors or unexpected behavior.
     * 
     * <h3>Implementation Example</h3>
     * <pre>{@code
     * public void close() {
     *     // Close any file handles
     *     if (logFile != null) {
     *         try {
     *             logFile.close();
     *         } catch (IOException e) {
     *             // Log error but don't throw
     *         }
     *     }
     *     
     *     // Stop background threads
     *     if (backgroundExecutor != null) {
     *         backgroundExecutor.shutdown();
     *     }
     *     
     *     // Clear references
     *     logFile = null;
     *     backgroundExecutor = null;
     * }
     * }</pre>
     * 
     * @see #reset()
     */
    void close();
}
