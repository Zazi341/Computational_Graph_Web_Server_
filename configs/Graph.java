package configs;

import java.util.ArrayList;
import java.util.HashMap;

import graph.TopicManagerSingleton.TopicManager;

//---------------------------------------------------
import graph.Topic;
import graph.Agent;
import graph.TopicManagerSingleton;
//---------------------------------------------------

public class Graph extends ArrayList<Node>{
	//---------------------------------------------------
	// checking if any node in the graph is part of a cycle
    public boolean hasCycles() {
        for (Node node : this) {
            if (node.hasCycles()) {
                return true;
            }
        }
        return false;
    }

    // creating the graph structure based on current topics and agents
    public void createFromTopics() {
        TopicManager manager = TopicManagerSingleton.get();

        HashMap<String, Node> nameToNode = new HashMap<>();

        // creating nodes for all topics
        for (Topic topic : manager.getTopics()) {
            Node node = new Node("T" + topic.name); // T for Topic
            nameToNode.put("T" + topic.name, node);
            this.add(node);
        }

        // creating nodes for all agents (publishers + subscribers)
        for (Topic topic : manager.getTopics()) {
            for (Agent agent : topic.getSubscribers()) { // subs are subscribers = agents who listen
            	String agentKey = "A" + agent.getName();
            	if (!nameToNode.containsKey(agentKey)) {
            	    Node node = new Node(agentKey);
            	    nameToNode.put(agentKey, node);
            	    this.add(node);
            	}
            }
            
            for (Agent agent : topic.getPublishers()) { // pubs are publishers = agents who write
            	String agentKey = "A" + agent.getName();
            	if (!nameToNode.containsKey(agentKey)) {
            	    Node node = new Node(agentKey);
            	    nameToNode.put(agentKey, node);
            	    this.add(node);
            	}

            }
        }

        // connecting the graph: topic → agent for subscriptions, agent → topic for publishing
        for (Topic topic : manager.getTopics()) {
            Node topicNode = nameToNode.get("T" + topic.name);
            for (Agent agent : topic.getSubscribers()) {
                Node agentNode = nameToNode.get("A" + agent.getName());
                topicNode.addEdge(agentNode); // topic → agent
            }
            for (Agent agent : topic.getPublishers()) {
                Node agentNode = nameToNode.get("A" + agent.getName());
                agentNode.addEdge(topicNode); // agent → topic
            }
        }
    } 

  //---------------------------------------------------
}
