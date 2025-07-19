package graph;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
//---------------------------------------------------

/**
 * Represents a communication channel in the publish-subscribe pattern for the computational graph system.
 * 
 * <p>A Topic acts as a message broker between agents, allowing publishers to send messages
 * and subscribers to receive them. Topics maintain lists of both publishers and subscribers,
 * and store the last published message for retrieval.
 * 
 * <h3>Publish-Subscribe Pattern</h3>
 * The Topic class implements the core publish-subscribe messaging pattern:
 * <ul>
 *   <li><strong>Publishers</strong> - Agents that send messages to the topic</li>
 *   <li><strong>Subscribers</strong> - Agents that receive messages from the topic</li>
 *   <li><strong>Message Routing</strong> - When a message is published, all subscribers are notified</li>
 * </ul>
 * 
 * <h3>Thread Safety</h3>
 * This class is designed for concurrent access:
 * <ul>
 *   <li>Uses {@link CopyOnWriteArrayList} for subscriber and publisher lists</li>
 *   <li>Volatile {@code lastMessage} field ensures visibility across threads</li>
 *   <li>Safe for multiple threads to publish and subscribe simultaneously</li>
 * </ul>
 * 
 * <h3>Message Flow Example</h3>
 * <pre>{@code
 * // Create a topic
 * Topic temperatureTopic = new Topic("temperature");
 * 
 * // Register agents
 * temperatureTopic.addPublisher(sensorAgent);
 * temperatureTopic.subscribe(displayAgent);
 * temperatureTopic.subscribe(alertAgent);
 * 
 * // When sensor publishes a message
 * Message tempReading = new Message("25.5", 25.5);
 * temperatureTopic.publish(tempReading);
 * // Both displayAgent and alertAgent receive the message via callback
 * }</pre>
 * 
 * <h3>Concurrent Access Patterns</h3>
 * The CopyOnWriteArrayList ensures that:
 * <ul>
 *   <li>Iteration over subscribers during publish() is safe even if the list is modified</li>
 *   <li>Adding/removing subscribers doesn't block message publishing</li>
 *   <li>Multiple threads can safely modify subscriber/publisher lists</li>
 * </ul>
 * 
 * @author Generated Documentation
 * @version 1.0
 * @see Agent
 * @see Message
 * @see TopicManagerSingleton
 * @since 1.0
 */
public class Topic {
    public final String name;
    
  //---------------------------------------------------
    private final List<Agent> subs = new CopyOnWriteArrayList<>();
    private final List<Agent> pubs = new CopyOnWriteArrayList<>();
    private volatile Message lastMessage = null;
  //---------------------------------------------------
    
    
    /**
     * Creates a new topic with the specified name.
     * 
     * <p>Package-private constructor used by {@link TopicManagerSingleton} to create
     * topic instances. Topics should be obtained through the TopicManagerSingleton
     * rather than created directly.
     * 
     * @param name the unique name identifier for this topic
     * 
     * @see TopicManagerSingleton#getTopic(String)
     */
    Topic(String name){
        this.name=name;
    }

    /**
     * Registers an agent as a subscriber to this topic.
     * 
     * <p>Once subscribed, the agent will receive all messages published to this topic
     * via its {@link Agent#callback(String, Message)} method. If the agent is already
     * subscribed, this method has no effect (no duplicate subscriptions).
     * 
     * <p><strong>Thread Safety:</strong> This method is safe to call concurrently
     * with other subscription operations and message publishing.
     * 
     * @param a the agent to subscribe to this topic
     * @throws NullPointerException if the agent is null
     * 
     * @see #unsubscribe(Agent)
     * @see #publish(Message)
     * @see Agent#callback(String, Message)
     */
    public void subscribe(Agent a){
    	//---------------------------------------------------
    	if (!subs.contains(a)) subs.add(a);
    	//---------------------------------------------------
    }
    
    /**
     * Removes an agent from the subscriber list for this topic.
     * 
     * <p>After unsubscribing, the agent will no longer receive messages published
     * to this topic. If the agent was not subscribed, this method has no effect.
     * 
     * <p><strong>Thread Safety:</strong> This method is safe to call concurrently
     * with other subscription operations and message publishing.
     * 
     * @param a the agent to unsubscribe from this topic
     * 
     * @see #subscribe(Agent)
     */
    public void unsubscribe(Agent a){
    	//---------------------------------------------------
    	subs.remove(a);
    	//---------------------------------------------------
    }

    /**
     * Publishes a message to all subscribers of this topic.
     * 
     * <p>This method implements the core publish operation of the publish-subscribe pattern:
     * <ol>
     *   <li>Stores the message as the last published message</li>
     *   <li>Notifies all current subscribers by calling their callback method</li>
     * </ol>
     * 
     * <p>The message is delivered to subscribers by invoking
     * {@link Agent#callback(String, Message)} with this topic's name and the message.
     * 
     * <p><strong>Thread Safety:</strong> This method is safe to call concurrently.
     * The CopyOnWriteArrayList ensures that iteration over subscribers is safe
     * even if other threads are modifying the subscriber list during publication.
     * 
     * <p><strong>Message Persistence:</strong> The published message becomes the
     * "last message" and can be retrieved later via {@link #getLastMessage()}.
     * 
     * @param m the message to publish to all subscribers
     * @throws NullPointerException if the message is null
     * 
     * @see #subscribe(Agent)
     * @see #getLastMessage()
     * @see Agent#callback(String, Message)
     */
    public void publish(Message m){
    	//---------------------------------------------------
        this.lastMessage = m; // Store the last message
        for (Agent a : subs) {
            a.callback(name, m);
        }
    	//---------------------------------------------------
    }

    /**
     * Registers an agent as a publisher for this topic.
     * 
     * <p>This method maintains a list of agents that are known to publish messages
     * to this topic. While not required for the publish operation itself, this
     * information is useful for graph visualization and debugging purposes.
     * 
     * <p>If the agent is already registered as a publisher, this method has no effect.
     * 
     * <p><strong>Thread Safety:</strong> This method is safe to call concurrently
     * with other publisher operations and message publishing.
     * 
     * @param a the agent to register as a publisher
     * @throws NullPointerException if the agent is null
     * 
     * @see #removePublisher(Agent)
     * @see #getPublishers()
     */
    public void addPublisher(Agent a){
    	//---------------------------------------------------
    	if (!pubs.contains(a)) pubs.add(a);
    	//---------------------------------------------------
    }

    /**
     * Removes an agent from the publisher list for this topic.
     * 
     * <p>This method removes the agent from the list of known publishers.
     * If the agent was not registered as a publisher, this method has no effect.
     * 
     * <p><strong>Thread Safety:</strong> This method is safe to call concurrently
     * with other publisher operations and message publishing.
     * 
     * @param a the agent to remove from the publisher list
     * 
     * @see #addPublisher(Agent)
     * @see #getPublishers()
     */
    public void removePublisher(Agent a){
    	//---------------------------------------------------
    	pubs.remove(a);
    	//---------------------------------------------------
    }
    
  //---------------------------------------------------
    /**
     * Returns a list of all agents currently subscribed to this topic.
     * 
     * <p>The returned list is the actual internal list used by the topic.
     * Since it's a CopyOnWriteArrayList, it's safe for concurrent iteration
     * but modifications should be done through the topic's methods.
     * 
     * @return the list of subscriber agents
     * 
     * @see #subscribe(Agent)
     * @see #unsubscribe(Agent)
     */
    public List<Agent> getSubscribers() {
        return subs;
    }

    /**
     * Returns a list of all agents registered as publishers for this topic.
     * 
     * <p>The returned list is the actual internal list used by the topic.
     * Since it's a CopyOnWriteArrayList, it's safe for concurrent iteration
     * but modifications should be done through the topic's methods.
     * 
     * @return the list of publisher agents
     * 
     * @see #addPublisher(Agent)
     * @see #removePublisher(Agent)
     */
    public List<Agent> getPublishers() {
        return pubs;
    }
    
    /**
     * Returns the last message published to this topic.
     * 
     * <p>This method provides access to the most recently published message,
     * which is useful for displaying current topic values or for agents that
     * need to access the current state without subscribing to future updates.
     * 
     * @return the last published message, or null if no message has been published yet
     * 
     * @see #publish(Message)
     * @see #getLastValue()
     */
    public Message getLastMessage() {
        return lastMessage;
    }
    
    /**
     * Returns the text representation of the last published message.
     * 
     * <p>This is a convenience method that extracts the text value from the
     * last published message. If no message has been published, returns "N/A".
     * 
     * @return the text value of the last message, or "N/A" if no message exists
     * 
     * @see #getLastMessage()
     * @see Message#asText
     */
    public String getLastValue() {
        if (lastMessage == null) return "N/A";
        return lastMessage.asText;
    }
    
    /**
     * Clears the stored last message for this topic.
     * 
     * <p>After calling this method, {@link #getLastMessage()} will return null
     * and {@link #getLastValue()} will return "N/A" until a new message is published.
     * 
     * <p>This method does not affect the subscriber or publisher lists.
     * 
     * @see #clearAll()
     * @see #getLastMessage()
     */
    public void clearValue() {
        this.lastMessage = null;
    }
    
    /**
     * Clears all data associated with this topic.
     * 
     * <p>This method performs a complete reset of the topic:
     * <ul>
     *   <li>Clears the last message</li>
     *   <li>Removes all subscribers</li>
     *   <li>Removes all publishers</li>
     * </ul>
     * 
     * <p>After calling this method, the topic will be in the same state as
     * when it was first created.
     * 
     * <p><strong>Thread Safety:</strong> This method is safe to call concurrently,
     * but may cause concurrent publish operations to deliver messages to an
     * empty subscriber list.
     * 
     * @see #clearValue()
     */
    public void clearAll() {
        this.lastMessage = null;
        this.subs.clear();
        this.pubs.clear();
    }
  //---------------------------------------------------


}
