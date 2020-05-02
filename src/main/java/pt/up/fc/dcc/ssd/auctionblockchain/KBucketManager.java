package pt.up.fc.dcc.ssd.auctionblockchain;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.lang.Math;

public class KBucketManager {
    private static final Logger logger = Logger.getLogger(KademliaNode.class.getName());

    private KademliaNode myNode;
    private KBucket[] buckets;


    public KBucketManager(KademliaNode myNode) {
        this.myNode = myNode;

        this.buckets = new KBucket[KademliaUtils.idSizeInBits];
        for(int i = 0; i < KademliaUtils.idSizeInBits; i++)
            buckets[i] = new KBucket();
    }


    public boolean insertNode(KademliaNode node) {
        if (Arrays.equals(node.getNodeID(), myNode.getNodeID()))
            return false;

        int distance = KademliaUtils.distanceTo(myNode, node);

        int i = (int) KademliaUtils.log2(distance);

        synchronized (buckets[i]) {
            if(buckets[i].insertNode(node)) {
                logger.info("Inserted node " + node.getNodeID());
                return true;
            }

            List<KademliaNode> nodes = buckets[i].getNodes();
            KademliaNode first = nodes.get(0);
            if (!KademliaClient.ping(myNode, first)) {
                buckets[i].removeNode(first);

                buckets[i].insertNode(node);

                logger.info("Discarded first node " + first.getNodeID() + "and added new node " + node.getNodeID());
                return true;
            } else {
                buckets[i].removeNode(first);
                first.updateLastSeen(Instant.now().getEpochSecond());
                buckets[i].insertNode(first);

                logger.info("Updated first node " + first.getNodeID());
                return false;
            }
        }
    }


    public KBucket[] getBuckets() {
        return buckets;
    }

    public KBucket getBucket(int i){
        if(i < 0 || i >= KademliaUtils.idSizeInBits)
            return null;

        return buckets[i];
    }
}
