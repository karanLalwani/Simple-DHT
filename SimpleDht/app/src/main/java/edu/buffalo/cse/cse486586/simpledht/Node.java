package edu.buffalo.cse.cse486586.simpledht;

public class Node implements Comparable<Node>{
    String node_id;
    String port_num;
    String emulator_num;
    String Predecessor = null;
    String Successor = null;

    public String getNode_id() {
        return node_id;
    }

    public void setNode_id(String node_id) {
        this.node_id = node_id;
    }

    public String getPort_num() {
        return port_num;
    }

    public void setPort_num(String port_num) {
        this.port_num = port_num;
    }

    public String getEmulator_num() {
        return emulator_num;
    }

    public void setEmulator_num(String emulator_num) {
        this.emulator_num = emulator_num;
    }

    public String getPredecessor() {
        return Predecessor;
    }

    public void setPredecessor(String predecessor) {
        Predecessor = predecessor;
    }

    public String getSuccessor() {
        return Successor;
    }

    public void setSuccessor(String successor) {
        Successor = successor;
    }

    public int compareTo(Node t) {
        return this.node_id.compareTo(t.getNode_id());
    }
}
