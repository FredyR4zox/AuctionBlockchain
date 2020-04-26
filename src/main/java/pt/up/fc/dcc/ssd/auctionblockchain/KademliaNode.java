package pt.up.fc.dcc.ssd.auctionblockchain;

import java.time.Instant;
import java.util.logging.Logger;
import java.math.BigInteger;

public class KademliaNode {
    private static final Logger logger = Logger.getLogger(KademliaNode.class.getName());
    private static int idSizeInBytes = 20;

    private final String ipAddress;
    private final int port;
    private final byte[] nodeID;
    private long lastSeen;

    public KademliaNode(String ipAddress, int port, byte[] nodeID) {
        this(ipAddress, port, nodeID, Instant.now().getEpochSecond());
    }

    public KademliaNode(String ipAddress, int port, byte[] nodeID, long lastSeen) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.nodeID = new byte[idSizeInBytes];
        for (int i = 0; i< idSizeInBytes; i++)
            this.nodeID[i] = nodeID[i];
        this.lastSeen = lastSeen;
    }

    public KademliaNode(KademliaNode node){
        this(node.getIpAddress(), node.getPort(), node.getNodeID(), node.getLastSeen());
    }

    public int distanceTo(KademliaNode node){
        return this.distanceTo(node.getNodeID());
    }

    public int distanceTo(byte[] nodeID){
        byte[] distance = new byte[idSizeInBytes];
        for (int i = 0; i< idSizeInBytes; i++)
            distance[i] = (byte) ((int)this.nodeID[i] ^ (int)nodeID[i]);

        return new BigInteger(distance).intValue();
    }

    public void updateLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public boolean ping() {
        return true;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public byte[] getNodeID() {
        byte[] ret = new byte[idSizeInBytes];
        for (int i = 0; i< idSizeInBytes; i++)
            ret[i] = nodeID[i];

        return ret;
    }

    public long getLastSeen() {
        return lastSeen;
    }
}
