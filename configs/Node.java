//---------------------------------------------------
package configs;

import java.util.List;
import java.util.ArrayList;
import graph.Message;


public class Node {

	private String name;
	private List<Node> edges = new ArrayList<>();
	private Message message = null;
	
    public Node(String name) {
        this.name = name;
    }
    
    // getting the name of the node
    public String getName() {
        return name;
    }


    // setting the name of the node
    public void setName(String name) {
        this.name = name;
    }

    // getting the message associated with this node
    public Message getMessage() {
        return message;
    }

    // setting the message for this node
    public void setMessage(Message message) {
        this.message = message;
    }

    // returning the list of outgoing edges
    public List<Node> getEdges() {
        return edges;
    }

    // adding an edge to another node (if not already exists)
    public void addEdge(Node n) {
        if (!edges.contains(n)) {
            edges.add(n);
        }
    }

    // checking whether this node is part of a cycle
    public boolean hasCycles() {
        return hasCycles(new ArrayList<>());
    }

    // recursively checking for cycles using DFS
    private boolean hasCycles(List<Node> visited) {
        if (visited.contains(this)) {
            return true; // Found a cycle
        }
        visited.add(this);
        for (Node neighbor : edges) {
            if (neighbor.hasCycles(new ArrayList<>(visited))) {
                return true;
            }
        }
        return false;
    }

}
//---------------------------------------------------