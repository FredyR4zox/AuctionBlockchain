package pt.up.fc.dcc.ssd.auctionblockchain;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.logging.Logger;
import java.lang.Math;

public class KBucketManager {
    private static final Logger logger = Logger.getLogger(KademliaNode.class.getName());

    private KademliaNode myNode;
    private KBucket[] buckets;


    public KBucketManager(KademliaNode myNode) {
        this.myNode = myNode;

        this.buckets = new KBucket[KademliaUtil.idSizeInBits];
        for(int i = 0 ; i < KademliaUtil.idSizeInBits; i++)
            buckets[i] = new KBucket();
    }


    public boolean insertNode(KademliaNode node){
        int distance = distanceTo(myNode, node);

        int i = (int)log2(distance);

        return buckets[i].insertNode(node);
    }

    public KBucket[] getBuckets() {
        return buckets;
    }

    public KBucket getBucket(int i){
        return buckets[i];
    }

    public int distanceTo(KademliaNode node1, KademliaNode node2){
        return this.distanceTo(node1.getNodeID(), node2.getNodeID());
    }

    public int distanceTo(byte[] nodeID1, byte[] nodeID2){
        byte[] distance = new byte[KademliaUtil.idSizeInBits];

        for (int i = 0; i < KademliaUtil.idSizeInBits; i++)
            distance[i] = (byte) ((int)nodeID1[i] ^ (int)nodeID2[i]);

        return new BigInteger(distance).intValue();
    }

    public static double log2(double d) {
        return Math.log(d)/Math.log(2.0);
    }
}
