package pt.up.fc.dcc.ssd.auctionblockchain.Kademlia;

import pt.up.fc.dcc.ssd.auctionblockchain.Pair;
import pt.up.fc.dcc.ssd.auctionblockchain.Utils;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.Math;
import com.google.common.math.BigIntegerMath;

public class KBucketManager {
    private static final Logger logger = Logger.getLogger(KBucketManager.class.getName());

    private final KademliaNode myNode;
    private final KBucket[] buckets;


    public KBucketManager(KademliaNode myNode) {
        this.myNode = myNode;

        this.buckets = new KBucket[Utils.hashAlgorithmLengthInBits];
        for(int i = 0; i < Utils.hashAlgorithmLengthInBits; i++)
            buckets[i] = new KBucket();
    }


    public boolean insertNode(KademliaNode node) {
        if (myNode.equals(node)) {
            logger.log(Level.SEVERE, "Error: Trying to insert own node into bucket");
            return false;
        }

//        System.out.println("Inserting Node: ");
//        for(int j=0; j<KademliaUtils.idSizeInBytes; j++)
//            System.out.print(" " + node.getNodeID()[j]);
//        System.out.println("\n\n");

        BigInteger distance = KademliaUtils.distanceTo(myNode, node);

        if(distance.equals(BigInteger.ZERO))
            logger.log(Level.SEVERE, "Error: Distance of 0 and the node is not equal to myNode");

        int i = BigIntegerMath.log2(distance, RoundingMode.FLOOR);

        synchronized (buckets[i]) {
            if(buckets[i].insertNode(node)) {
//                logger.log(Level.INFO, "Added node to bucket " + i);
                return true;
            }

            List<KademliaNode> nodes = buckets[i].getNodes();
            KademliaNode first = nodes.get(0);

            Pair<KademliaNode, Boolean> ret = KademliaClient.ping(myNode, first);
            if (!ret.getSecond().booleanValue()) {
                buckets[i].removeNode(first);

                buckets[i].insertNode(node);

//                logger.log(Level.INFO, "Discarded first node " + first + " and added new node " + node);
                return true;
            } else {
                buckets[i].removeNode(first);

                buckets[i].insertNode(ret.getFirst());

//                logger.log(Level.INFO, "Updated first node " + first);
                return false;
            }
        }
    }

    public KademliaNode getMyNode() {
        return myNode;
    }


    public KBucket[] getBuckets() {
        return buckets;
    }

    public List<KademliaNode> getClosestNodes(byte[] requestorID, byte[] requestedID, int maxNodes) {
        List<KademliaNode> ret = new ArrayList<>();

        BigInteger distance = KademliaUtils.distanceTo(myNode.getNodeID(), requestedID);
        int bucketLocation = BigIntegerMath.log2(distance, RoundingMode.FLOOR);

        List<KademliaNode> nodes = buckets[bucketLocation].getNodes();

        for(KademliaNode node : nodes) {
            if (!Arrays.equals(node.getNodeID(), requestorID) && !Arrays.equals(node.getNodeID(), myNode.getNodeID()))
                ret.add(node);
        }

        for(int i = 1; ret.size() < maxNodes && (bucketLocation + i < Utils.hashAlgorithmLengthInBits || bucketLocation - i >= 0); i++) {
            KBucket bucket;

            if(bucketLocation + i < Utils.hashAlgorithmLengthInBits) {
                bucket = buckets[bucketLocation + i];
                if (bucket != null) {
                    for (KademliaNode node : bucket.getNodes()) {
                        if (!Arrays.equals(node.getNodeID(), requestorID) && !Arrays.equals(node.getNodeID(), myNode.getNodeID()))
                            ret.add(node);
                    }
                }
            }

            if(bucketLocation - i >= 0) {
                bucket = buckets[bucketLocation - i];
                if (bucket != null) {
                    for (KademliaNode node : bucket.getNodes()) {
                        if (!Arrays.equals(node.getNodeID(), requestorID) && !Arrays.equals(node.getNodeID(), myNode.getNodeID()))
                            ret.add(node);
                    }
                }
            }
        }

//        logger.log(Level.INFO, "Aggregated " + ret.size() + " nodes");
        return ret.subList(0, Math.min(ret.size(), maxNodes));
    }
}
