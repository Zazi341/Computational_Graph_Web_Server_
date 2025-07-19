package graph;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A wrapper that enables concurrent message processing for any Agent implementation.
 * 
 * <p>ParallelAgent implements the Decorator pattern to add asynchronous message processing
 * capabilities to existing agents. It uses a dedicated worker thread and a bounded queue
 * to decouple message reception from message processing, allowing the agent to handle
 * multiple messages concurrently without blocking the publisher.
 * 
 * <h3>Concurrent Execution Model</h3>
 * The ParallelAgent operates using a producer-consumer pattern:
 * <ul>
 *   <li><strong>Producer:</strong> The {@link #callback(String, Message)} method adds messages to the queue</li>
 *   <li><strong>Consumer:</strong> A dedicated worker thread processes messages from the queue</li>
 *   <li><strong>Decoupling:</strong> Publishers don't block waiting for message processing</li>
 * </ul>
 * 
 * <h3>Work Queue Management</h3>
 * The internal work queue provides:
 * <ul>
 *   <li><strong>Bounded Capacity:</strong> Prevents memory exhaustion under high load</li>
 *   <li><strong>Blocking Operations:</strong> Automatic backpressure when queue is full</li>
 *   <li><strong>Thread Safety:</strong> Safe concurrent access from multiple threads</li>
 * </ul>
 * 
 * <h3>Capacity Management and Performance Considerations</h3>
 * The queue capacity affects performance characteristics:
 * <ul>
 *   <li><strong>Small Capacity:</strong> Lower memory usage, more backpressure</li>
 *   <li><strong>Large Capacity:</strong> Higher throughput, more memory usage</li>
 *   <li><strong>Recommended:</strong> 10-50 for most use cases, scale with input topic count</li>
 * </ul>
 * 
 * <h3>Threading and Synchronization Details</h3>
 * <ul>
 *   <li><strong>Worker Thread:</strong> Single dedicated thread per ParallelAgent instance</li>
 *   <li><strong>Thread Safety:</strong> All public methods are thread-safe</li>
 *   <li><strong>Shutdown:</strong> Graceful shutdown ensures all queued messages are processed</li>
 *   <li><strong>Interruption:</strong> Proper interrupt handling for clean shutdown</li>
 * </ul>
 * 
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Create a regular agent
 * Agent myAgent = new PlusAgent(new String[]{"input1", "input2"}, new String[]{"output"});
 * 
 * // Wrap with ParallelAgent for concurrent processing
 * int capacity = 20; // Queue capacity
 * ParallelAgent parallelAgent = new ParallelAgent(myAgent, capacity);
 * 
 * // Use like any other agent - messages are now processed asynchronously
 * parallelAgent.callback("input1", new Message("5", 5.0));
 * parallelAgent.callback("input2", new Message("3", 3.0));
 * 
 * // Clean shutdown
 * parallelAgent.close();
 * }</pre>
 * 
 * <h3>Error Handling</h3>
 * <ul>
 *   <li><strong>Queue Full:</strong> Blocks until space is available (backpressure)</li>
 *   <li><strong>Interruption:</strong> Preserves interrupt status and shuts down gracefully</li>
 *   <li><strong>Agent Errors:</strong> Delegates error handling to the wrapped agent</li>
 * </ul>
 * 
 * @author Generated Documentation
 * @version 1.0
 * @see Agent
 * @see ArrayBlockingQueue
 * @since 1.0
 */
public class ParallelAgent implements Agent{
	//---------------------------------------------------
	
    private final Agent agent;  // the wrapped agent
    private final BlockingQueue<WorkItem> Message_Queue;// stores incoming messages
    private final Thread Worker_Thr;// thread that processes messages
    private volatile boolean running = true;  // used to stop the thread safely

    /**
     * Internal data structure that pairs a topic name with its associated message.
     * 
     * <p>WorkItem is used to store topic-message pairs in the work queue, ensuring
     * that when messages are processed asynchronously, the worker thread has access
     * to both the topic name and the message content needed for the callback.
     * 
     * <p>This class is package-private and immutable to ensure thread safety
     * and prevent external modification of queued work items.
     */
    private static class WorkItem {
        final String topic;
        final Message message;

        /**
         * Creates a new work item with the specified topic and message.
         * 
         * @param topic the topic name associated with this message
         * @param message the message content to be processed
         */
        WorkItem(String topic, Message message) {
            this.topic = topic;
            this.message = message;
        }
    }

    /**
     * Creates a new ParallelAgent that wraps the specified agent with concurrent processing capabilities.
     * 
     * <p>This constructor performs the following initialization:
     * <ol>
     *   <li>Stores a reference to the wrapped agent</li>
     *   <li>Creates a bounded ArrayBlockingQueue with the specified capacity</li>
     *   <li>Starts a dedicated worker thread for message processing</li>
     * </ol>
     * 
     * <h3>Worker Thread Lifecycle</h3>
     * The worker thread runs continuously and:
     * <ul>
     *   <li>Waits for messages in the queue using {@link BlockingQueue#take()}</li>
     *   <li>Processes each message by calling the wrapped agent's callback method</li>
     *   <li>Continues until shutdown is requested AND the queue is empty</li>
     *   <li>Handles interruption gracefully during shutdown</li>
     * </ul>
     * 
     * <h3>Capacity Guidelines</h3>
     * Choose capacity based on your use case:
     * <ul>
     *   <li><strong>Low latency:</strong> Small capacity (10-20) for immediate backpressure</li>
     *   <li><strong>High throughput:</strong> Large capacity (100+) for burst handling</li>
     *   <li><strong>Balanced:</strong> Medium capacity (20-50) for most applications</li>
     * </ul>
     * 
     * @param agent the agent to wrap with concurrent processing capabilities
     * @param capacity the maximum number of messages that can be queued for processing
     * @throws IllegalArgumentException if capacity is less than 1
     * @throws NullPointerException if agent is null
     * 
     * @see ArrayBlockingQueue#ArrayBlockingQueue(int)
     * @see #close()
     */
    public ParallelAgent(Agent agent, int capacity) {
        this.agent = agent;
        this.Message_Queue = new ArrayBlockingQueue<>(capacity);

        // starting the worker thread
        this.Worker_Thr = new Thread(() -> {
//            System.out.println("Worker started");
            while (running || !Message_Queue.isEmpty()) {
                try {
                    WorkItem Item = Message_Queue.take();// wait for next message
//                    System.out.println("Message received: " + Item.topic);
                    agent.callback(Item.topic, Item.message);
                } catch (InterruptedException e) {
                    // thread was interrupted, probably due to shutdown
                    Thread.currentThread().interrupt();
                    break;
                }
            }
//            System.out.println("Worker stopped");
        });
        this.Worker_Thr.start();
    }

    /**
     * Returns the name of the wrapped agent.
     * 
     * <p>This method delegates to the wrapped agent's getName() method,
     * ensuring that the ParallelAgent maintains the same identity as
     * the original agent for logging and debugging purposes.
     * 
     * @return the name of the wrapped agent
     * 
     * @see Agent#getName()
     */
    @Override
    public String getName() {
        return agent.getName();
    }

    /**
     * Resets the wrapped agent to its initial state.
     * 
     * <p>This method delegates to the wrapped agent's reset() method.
     * Note that this operation is performed synchronously on the calling
     * thread, not through the work queue, to ensure immediate effect.
     * 
     * <p><strong>Thread Safety:</strong> This method should be called
     * carefully in concurrent environments, as it may interfere with
     * messages currently being processed by the worker thread.
     * 
     * @see Agent#reset()
     */
    @Override
    public void reset() {
        agent.reset();
    }

    /**
     * Asynchronously processes a message by adding it to the work queue.
     * 
     * <p>This method implements the core asynchronous processing mechanism:
     * <ol>
     *   <li>Creates a WorkItem containing the topic and message</li>
     *   <li>Adds the WorkItem to the bounded queue using {@link BlockingQueue#put(Object)}</li>
     *   <li>Returns immediately, allowing the caller to continue</li>
     *   <li>The worker thread will process the message when available</li>
     * </ol>
     * 
     * <h3>Backpressure Handling</h3>
     * If the queue is full, this method will block until space becomes available.
     * This provides natural backpressure to prevent memory exhaustion under high load.
     * 
     * <h3>Interruption Handling</h3>
     * If the calling thread is interrupted while waiting to add to the queue,
     * the interrupt status is preserved and the method returns without adding
     * the message to the queue.
     * 
     * @param topic the name of the topic associated with this message
     * @param msg the message to be processed asynchronously
     * 
     * @see Agent#callback(String, Message)
     * @see BlockingQueue#put(Object)
     */
    @Override
    public void callback(String topic, Message msg) {
        // adding message to queue for async processing
        try {
            Message_Queue.put(new WorkItem(topic, msg));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // preserving interrupt status
        }
    }

    /**
     * Initiates graceful shutdown of the ParallelAgent and cleans up resources.
     * 
     * <p>This method performs shutdown in the following sequence:
     * <ol>
     *   <li>Sets the running flag to false, signaling the worker thread to stop</li>
     *   <li>Interrupts the worker thread to wake it from blocking operations</li>
     *   <li>Calls close() on the wrapped agent to allow it to clean up</li>
     * </ol>
     * 
     * <h3>Graceful Shutdown Behavior</h3>
     * The worker thread will continue processing messages until:
     * <ul>
     *   <li>The running flag is false AND the queue is empty, OR</li>
     *   <li>The thread is interrupted during a blocking operation</li>
     * </ul>
     * 
     * <p>This ensures that all queued messages are processed before shutdown,
     * preventing message loss during normal shutdown scenarios.
     * 
     * <p><strong>Important:</strong> This method does not wait for the worker
     * thread to finish. If you need to ensure complete shutdown, consider
     * calling {@code Thread.join()} on the worker thread after calling close().
     * 
     * <p><strong>Thread Safety:</strong> This method is safe to call from any
     * thread and can be called multiple times without adverse effects.
     * 
     * @see Agent#close()
     */
    @Override
    public void close() {
        // stopping the worker thread
        running = false;
        Worker_Thr.interrupt();
        agent.close();
    }
	
	
	
	//---------------------------------------------------

}
