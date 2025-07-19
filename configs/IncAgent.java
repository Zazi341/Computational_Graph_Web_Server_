package configs;

import graph.TopicManagerSingleton.TopicManager;

//---------------------------------------------------
import graph.Agent;
import graph.Message;
import graph.Topic;
import graph.TopicManagerSingleton;
//---------------------------------------------------

public class IncAgent implements Agent{
	//---------------------------------------------------
    private final String The_Name;
    private final String[] subs;
    private final String[] pubs;
    
    
    // creating an increment agent that listens to one topic and publishes to another
    public IncAgent(String[] The_subs_Inp, String[] The_pubs_Inp) {
        this.The_Name = "IncAgent";
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

    @Override
    public String getName() {
        return The_Name;
    }

    @Override
    public void reset() {
        // there is no state that needs to do a reset in this agent
    }

    @Override
    public void callback(String topic, Message msg) {
    	// ignoring if input or output is missing
        if (subs.length < 1 || pubs.length < 1) return;
        
//        System.out.println("callback on: " + topic + " | " + msg.asDouble );
        
        // ignoring non-numeric messages
        if (Double.isNaN(msg.asDouble)) return;
        

        // incrementing the value and publishing it
        double result = msg.asDouble + 1;
        TopicManagerSingleton.get().getTopic(pubs[0]).publish(new Message(result));
        
//        System.out.println("callback on: " + topic + " | " + msg.asDouble + " | result= " + result);
    }

    @Override
    public void close() {
    	// nothing to close
    }
    
	
	//---------------------------------------------------
    
}
