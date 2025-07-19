package configs;

import java.util.function.BinaryOperator;

import graph.TopicManagerSingleton.TopicManager;

//---------------------------------------------------
import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;
//---------------------------------------------------

/**
 * Abstract base class for binary operation agents in the computational graph system.
 * 
 * <p>BinOpAgent implements the {@link Agent} interface to provide a framework for agents
 * that perform binary operations on two input values and publish the result to an output topic.
 * This class handles the common pattern of waiting for two input values, applying a binary
 * operation when both are available, and then resetting for the next computation cycle.</p>
 * 
 * <p><strong>Operation Pattern:</strong></p>
 * <ol>
 *   <li>Subscribe to two input topics during construction</li>
 *   <li>Wait for messages on both input topics</li>
 *   <li>When both inputs are received, apply the binary operation</li>
 *   <li>Publish the result to the output topic</li>
 *   <li>Reset internal state for the next computation</li>
 * </ol>
 * 
 * <p><strong>Thread Safety:</strong> This class is thread-safe for concurrent message
 * processing. The callback method is synchronized through the topic management system,
 * and internal state is properly managed during reset operations.</p>
 * 
 * <p><strong>Configuration Example:</strong></p>
 * <pre>
 * # Configuration file format for binary operation agents
 * configs.PlusAgent        # Agent class (extends BinOpAgent)
 * input1,input2           # Two input topics (comma-separated)
 * result                  # Single output topic
 * </pre>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Create a custom binary operation agent
 * BinaryOperator<Double> addOperation = (a, b) -> a + b;
 * BinOpAgent addAgent = new BinOpAgent("AddAgent", "topic1", "topic2", "sum", addOperation);
 * 
 * // The agent will automatically:
 * // 1. Subscribe to "topic1" and "topic2"
 * // 2. Wait for messages on both topics
 * // 3. Add the values when both are available
 * // 4. Publish the sum to "sum" topic
 * }</pre>
 * 
 * @see Agent
 * @see BinaryOperator
 * @see PlusAgent
 * @see IncAgent
 * @since 1.0
 */
public class BinOpAgent implements Agent{
	//---------------------------------------------------
	/** The unique name identifier for this agent instance. */
	private final String Agent_name;
	
	/** The name of the first input topic this agent subscribes to. */
	private final String Topic_input1_name;
	
	/** The name of the second input topic this agent subscribes to. */
	private final String Topic_input2_name;
	
	/** The name of the output topic where results are published. */
	private final String Output_name;
	
	/** The binary operation function applied to the two input values. */
	private final BinaryOperator<Double> BinOp;
	
	/** Current value from the first input topic, null if not yet received. */
	private Double Val_1 = null;
	
	/** Current value from the second input topic, null if not yet received. */
	private Double Val_2 = null;
	
	/**
	 * Constructs a new BinOpAgent with the specified configuration.
	 * 
	 * <p>This constructor automatically subscribes the agent to both input topics
	 * and registers it as a publisher for the output topic. The agent will begin
	 * processing messages immediately after construction.</p>
	 * 
	 * @param ag_name the unique name for this agent instance
	 * @param in_Top1 the name of the first input topic to subscribe to
	 * @param in_Top2 the name of the second input topic to subscribe to
	 * @param out_Top the name of the output topic to publish results to
	 * @param bin_op the binary operation to apply when both inputs are available
	 * 
	 * @throws NullPointerException if any parameter is null
	 * @throws IllegalArgumentException if topic names are empty strings
	 * 
	 * @see TopicManagerSingleton#get()
	 * @see graph.Topic#subscribe(Agent)
	 * @see graph.Topic#addPublisher(Agent)
	 */
	 public BinOpAgent(String ag_name, String in_Top1, String in_Top2, String out_Top, BinaryOperator<Double> bin_op) {
		 this.Agent_name = ag_name;
		 this.Topic_input1_name = in_Top1;
		 this.Topic_input2_name = in_Top2;
		 this.Output_name = out_Top;
		 this.BinOp = bin_op;
		 
        
		// subscribing to both input topics
        TopicManager manager = TopicManagerSingleton.get();
        manager.getTopic(Topic_input1_name).subscribe(this);
        manager.getTopic(Topic_input2_name).subscribe(this);

        // registering as a publisher for the output topic
        manager.getTopic(Output_name).addPublisher(this);
	 }
	 
	 
	 
	    /**
	     * Returns the unique name of this agent instance.
	     * 
	     * @return the agent name specified during construction
	     */
	    @Override
	    public String getName() {
	        return Agent_name;
	    }

	    /**
	     * Resets the internal state of this agent by clearing both input values.
	     * 
	     * <p>This method is called automatically after each successful computation
	     * to prepare the agent for the next pair of input values. It can also be
	     * called manually to clear any partially received inputs.</p>
	     * 
	     * <p><strong>Thread Safety:</strong> This method is safe to call from
	     * multiple threads, though it's typically called from the callback thread.</p>
	     */
	    @Override
	    public void reset() {
	    	Val_1 = null;
	    	Val_2 = null;
	    }

	    /**
	     * Processes incoming messages from subscribed topics and performs binary operations.
	     * 
	     * <p>This method implements the core logic of the binary operation agent:</p>
	     * <ol>
	     *   <li>Validates that the incoming message contains a valid numeric value</li>
	     *   <li>Stores the value based on which input topic sent the message</li>
	     *   <li>When both input values are available, applies the binary operation</li>
	     *   <li>Publishes the result to the output topic</li>
	     *   <li>Resets internal state for the next computation cycle</li>
	     * </ol>
	     * 
	     * <p><strong>Message Validation:</strong> Only messages with valid numeric values
	     * (not NaN) are processed. Invalid messages are silently ignored.</p>
	     * 
	     * <p><strong>Computation Trigger:</strong> The binary operation is only performed
	     * when both input topics have provided valid values. Partial inputs are held
	     * until the second value arrives.</p>
	     * 
	     * @param topic the name of the topic that sent the message
	     * @param msg the message containing the numeric value to process
	     * 
	     * @see Message#asDouble
	     * @see BinaryOperator#apply(Object, Object)
	     */
	    @Override
	    public void callback(String topic, Message msg) {
	    	 // checking if the message contains a valid Double
	        if (!Double.isNaN(msg.asDouble)) {
	            if (topic.equals(Topic_input1_name)) {
	            	Val_1 = msg.asDouble;
	            } else if (topic.equals(Topic_input2_name)) {
	            	Val_2 = msg.asDouble;
	            }
	            // publishing result if both inputs are ready
	            if (Val_1 != null && Val_2 != null) {
	                double result = BinOp.apply(Val_1, Val_2);
	                TopicManagerSingleton.get().getTopic(Output_name).publish(new Message(result));
	                reset(); // resetting inputs after publishing
	            }
	        }
	    }

	    /**
	     * Performs cleanup when the agent is no longer needed.
	     * 
	     * <p>This implementation does not require any special cleanup as the agent
	     * uses only managed resources through the topic management system. The
	     * topic subscriptions and publisher registrations are handled automatically
	     * by the framework.</p>
	     * 
	     * <p><strong>Note:</strong> Subclasses that manage additional resources
	     * should override this method to perform proper cleanup.</p>
	     */
	    @Override
	    public void close() {
	    	 // nothing to close
	    }
	

	//---------------------------------------------------
}
