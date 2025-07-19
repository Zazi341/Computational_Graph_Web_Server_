package configs;

import graph.TopicManagerSingleton.TopicManager;

//---------------------------------------------------
import graph.Agent;
import graph.Message;
import graph.TopicManagerSingleton;
//---------------------------------------------------

public class PlusAgent implements Agent {
	//---------------------------------------------------
	private final String The_Name;
    private final String[] subs;
    private final String[] pubs;

    private double x = 0;
    private double y = 0;
    
    private boolean xSet = false;
    private boolean ySet = false;

    
    // creating a plus agent that listens to two input topics and publishes the sum
    public PlusAgent(String[] subs, String[] pubs) {
        this.The_Name = "PlusAgent";
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

    @Override
    public String getName() {
        return The_Name;
    }

    @Override
    public void reset() {
    	// resetting internal values
        x = 0;
        y = 0;
        
        xSet = false;
        ySet = false;
    }

    @Override
    public void callback(String topic, Message msg) {
        if (subs.length < 2 || pubs.length < 1) return;
//        if (Double.isNaN(msg.asDouble)) return;

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
        
//        System.out.println("callback on: " + topic + " | x=" + x + ", y=" + y + " | xSet=" + xSet + ", ySet=" + ySet);

        // only publishing when both values have been set explicitly
        if (xSet && ySet) {
            double result = x + y;
            
//            System.out.println("callback on: " + topic + " | x=" + x + ", y=" + y + " | xSet=" + xSet + ", ySet=" + ySet + " | result= " + result);
            
            TopicManagerSingleton.get().getTopic(pubs[0]).publish(new Message(result));
        }
        
    }

        



    @Override
    public void close() {
    	// nothing to close
    }
	
	
	//---------------------------------------------------
    
}
