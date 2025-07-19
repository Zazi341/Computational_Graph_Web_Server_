/**
 * Computational graph implementation using the publish-subscribe pattern for message-based agent communication.
 * 
 * <p>This package provides the core infrastructure for creating and managing computational graphs where
 * agents process messages and communicate through topics. The architecture follows a publish-subscribe
 * pattern that enables loose coupling between computational components and supports both synchronous
 * and asynchronous message processing.</p>
 * 
 * <h2>Publish-Subscribe Architecture</h2>
 * 
 * <p>The computational graph is built on a publish-subscribe messaging system:</p>
 * <ul>
 *   <li><strong>Topics</strong> - Named message channels that route messages between agents</li>
 *   <li><strong>Publishers</strong> - Agents that send messages to topics</li>
 *   <li><strong>Subscribers</strong> - Agents that receive messages from topics</li>
 *   <li><strong>Messages</strong> - Data containers supporting multiple representations (text, numeric, binary)</li>
 * </ul>
 * 
 * <h2>Agent System</h2>
 * 
 * <p>Agents are computational units that process messages and produce results:</p>
 * <ul>
 *   <li><strong>Callback-based Processing</strong> - Agents receive messages through callback methods</li>
 *   <li><strong>Lifecycle Management</strong> - Agents support initialization, reset, and cleanup operations</li>
 *   <li><strong>Parallel Execution</strong> - ParallelAgent wrapper enables asynchronous message processing</li>
 *   <li><strong>Topic Subscription</strong> - Agents can subscribe to multiple input topics</li>
 * </ul>
 * 
 * <h2>Message Flow</h2>
 * 
 * <p>The computational graph processes messages through the following flow:</p>
 * <ol>
 *   <li><strong>Message Creation</strong> - Messages are created with data in multiple formats</li>
 *   <li><strong>Topic Publishing</strong> - Publishers send messages to named topics</li>
 *   <li><strong>Subscriber Notification</strong> - All subscribers receive callback notifications</li>
 *   <li><strong>Agent Processing</strong> - Agents process messages and generate results</li>
 *   <li><strong>Result Publishing</strong> - Agents publish results to output topics</li>
 *   <li><strong>Graph Propagation</strong> - Messages flow through the entire computational graph</li>
 * </ol>
 * 
 * <h2>Concurrency Model</h2>
 * 
 * <p>The package provides thread-safe message processing with multiple concurrency options:</p>
 * <ul>
 *   <li><strong>Synchronous Processing</strong> - Direct agent callbacks in publisher thread</li>
 *   <li><strong>Asynchronous Processing</strong> - ParallelAgent queues messages for background processing</li>
 *   <li><strong>Thread-Safe Collections</strong> - CopyOnWriteArrayList for subscriber management</li>
 *   <li><strong>Concurrent Topic Access</strong> - ConcurrentHashMap for topic management</li>
 * </ul>
 * 
 * <h2>Key Classes</h2>
 * 
 * <dl>
 *   <dt>{@link graph.Agent}</dt>
 *   <dd>Interface defining the contract for computational agents with callback-based message processing</dd>
 *   
 *   <dt>{@link graph.Topic}</dt>
 *   <dd>Message routing hub that manages publishers, subscribers, and message distribution</dd>
 *   
 *   <dt>{@link graph.Message}</dt>
 *   <dd>Multi-format data container supporting text, numeric, and binary representations</dd>
 *   
 *   <dt>{@link graph.ParallelAgent}</dt>
 *   <dd>Wrapper that enables asynchronous message processing with configurable queue capacity</dd>
 *   
 *   <dt>{@link graph.TopicManagerSingleton}</dt>
 *   <dd>Centralized topic registry providing global access to named topics</dd>
 *   
 *   <dt>{@link graph.ObservableFuture}</dt>
 *   <dd>Utility for observing asynchronous operations and handling completion callbacks</dd>
 * </dl>
 * 
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * // Get topic manager instance
 * TopicManager tm = TopicManagerSingleton.get();
 * 
 * // Create topics for communication
 * Topic inputTopic = tm.getTopic("input");
 * Topic outputTopic = tm.getTopic("output");
 * 
 * // Create and configure agent
 * Agent processor = new MyProcessorAgent();
 * inputTopic.subscribe(processor);
 * outputTopic.addPublisher(processor);
 * 
 * // Send message through the graph
 * Message msg = new Message("Hello World");
 * inputTopic.publish(msg);
 * 
 * // Message flows: input -> processor -> output
 * }</pre>
 * 
 * <h2>Design Patterns</h2>
 * 
 * <ul>
 *   <li><strong>Observer Pattern</strong> - Topics notify subscribers of new messages</li>
 *   <li><strong>Singleton Pattern</strong> - TopicManagerSingleton provides global topic access</li>
 *   <li><strong>Decorator Pattern</strong> - ParallelAgent wraps agents with async capabilities</li>
 *   <li><strong>Strategy Pattern</strong> - Different agent implementations provide various processing strategies</li>
 * </ul>
 * 
 * @since 1.0
 * @see configs
 */
package graph;