package pt.up.fc.dcc.ssd.auctionblockchain;

import java.util.Arrays;
import java.util.logging.Logger;
import java.lang.Math;

public class KBucketManager {
    private static final Logger logger = Logger.getLogger(KademliaNode.class.getName());
    private static final int idSizeInBits = 160;

    private KademliaNode myNode;
    private KBucket[] buckets;

    public KBucketManager(KademliaNode node) {
        myNode = node;

        buckets = new KBucket[idSizeInBits];
        for(int i=0 ; i<idSizeInBits; i++)
            buckets[i] = new KBucket(i);
    }

    public boolean insertNode(KademliaNode node){
        int distance = myNode.distanceTo(node);
        int i = (int)log2(distance);

        return buckets[i].insertNode(node);
    }

    public KBucket[] getBuckets() {
        KBucket[] tmpBuckets = new KBucket[160];
        for (int i=0; i<idSizeInBits; i++)
            tmpBuckets[i] = new KBucket(buckets[i].getI(), Arrays.asList(buckets[i].getNodes()));

        return tmpBuckets;
    }

    public KBucket getBucket(int i){
        return new KBucket(buckets[i].getI(), Arrays.asList(buckets[i].getNodes()));
    }

    public static double log2(double d) {
        return Math.log(d)/Math.log(2.0);
    }
}
