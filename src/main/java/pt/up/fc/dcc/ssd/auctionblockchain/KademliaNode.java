package pt.up.fc.dcc.ssd.auctionblockchain;

import java.lang.reflect.Array;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Logger;
import java.math.BigInteger;

public class KademliaNode {
    private static final Logger logger = Logger.getLogger(KademliaNode.class.getName());

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
        // field comparison
        return Objects.equals(nodeID, node.getNodeID());
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
}
