package pt.up.fc.dcc.ssd.auctionblockchain.Kademlia;

import pt.up.fc.dcc.ssd.auctionblockchain.Utils;

import java.time.Instant;
import java.util.Arrays;
import java.util.logging.Logger;

public class KademliaNode {
//    private static final Logger logger = Logger.getLogger(KademliaNode.class.getName());

    private final String ipAddress;
    private final int port;
    private final byte[] nodeID;
    private long lastSeen;

    public KademliaNode(byte[] nodeID) {
        this("", 0, nodeID, Instant.now().getEpochSecond());
    }

    public KademliaNode(String ipAddress, int port, byte[] nodeID) {
        this(ipAddress, port, nodeID, Instant.now().getEpochSecond());
    }

    public KademliaNode(String ipAddress, int port, byte[] nodeID, long lastSeen) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.nodeID = nodeID;
        this.lastSeen = lastSeen;
    }

    public KademliaNode(KademliaNode node){
        this(node.getIpAddress(), node.getPort(), node.getNodeID(), node.getLastSeen());
    }


    @Override
    public boolean equals(Object obj) {
        // self check
        if (this == obj)
            return true;
        // null check
        if (obj == null)
            return false;
        // type check and cast
        if (getClass() != obj.getClass())
            return false;
        KademliaNode node = (KademliaNode) obj;

//        System.out.println("node1: ");
//        for(int j=0; j<KademliaUtils.idSizeInBytes; j++)
//            System.out.print(" " + nodeID[j]);
//        System.out.println("");
//
//        System.out.println("node2: ");
//        for(int j=0; j<KademliaUtils.idSizeInBytes; j++)
//            System.out.print(" " + node.getNodeID()[j]);
//        System.out.println("\n\n");

        // field comparison
        return Arrays.equals(nodeID, node.getNodeID());
    }


    public void updateLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public byte[] getNodeID() {
        return nodeID;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();

        for(int i = 0; i < Utils.hashAlgorithmLengthInBytes; i++)
            buf.append(String.format("%02x", nodeID[i]));

        buf.append(" " + ipAddress + ":" + port);

        return buf.toString();
    }
}
