package graph;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
//---------------------------------------------------

public class TopicManagerSingleton {

    public static class TopicManager{
    	//---------------------------------------------------
    	private static final TopicManager instance = new TopicManager();
    	
        private final ConcurrentHashMap<String, Topic> topics = new ConcurrentHashMap<>();
        private volatile long lastClearTime = 0;

        private TopicManager() {}

        public Topic getTopic(String name) {
            System.out.println("[DEBUG] getTopic called for: " + name);
            Topic topic = topics.computeIfAbsent(name, n -> {
                System.out.println("[DEBUG] Creating NEW topic: " + n);
                return new Topic(n);
            });
            System.out.println("[DEBUG] Returning topic '" + name + "' with value: " + topic.getLastValue());
        	return topic;
        }

        public Collection<Topic> getTopics() {
            return topics.values();
        }

        public void clear() {
            System.out.println("[DEBUG] TopicManager.clear() called");
            
            // First, reset all topic values to ensure clean state
            for (Topic topic : topics.values()) {
                System.out.println("[DEBUG] Clearing topic: " + topic.name + " (was: " + topic.getLastValue() + ")");
                topic.clearAll();
            }
            
            // Then clear the topics map completely
            int sizeBefore = topics.size();
            topics.clear();
            int sizeAfter = topics.size();
            
            System.out.println("[DEBUG] Topics map cleared: " + sizeBefore + " -> " + sizeAfter);
            
            // Record when the clear happened
            this.lastClearTime = System.currentTimeMillis();
            System.out.println("[DEBUG] Clear completed at: " + this.lastClearTime);
        }
        
        public long getLastClearTime() {
            return lastClearTime;
        }
        
    	//---------------------------------------------------
    }
    
  //--------------------------------------------------- 
    public static TopicManager get(){
    	return TopicManager.instance;
    	}
  //---------------------------------------------------
    
}
