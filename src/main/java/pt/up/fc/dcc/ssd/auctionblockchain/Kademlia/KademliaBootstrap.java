package pt.up.fc.dcc.ssd.auctionblockchain.Kademlia;


import pt.up.fc.dcc.ssd.auctionblockchain.Pair;
import pt.up.fc.dcc.ssd.auctionblockchain.Utils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KademliaBootstrap implements Runnable {
    private static final Logger logger = Logger.getLogger(KademliaBootstrap.class.getName());

    private Thread t;
    private final KBucketManager bucketManager;
    private final KademliaClient client;
    private final KademliaNode bootstrapNode;

    KademliaBootstrap(KBucketManager bucketManager, KademliaClient client, KademliaNode bootstrapNode) {
        this.client = client;
        this.bootstrapNode = bootstrapNode;
        this.bucketManager = bucketManager;
    }

    public void run() {
        Pair<KademliaNode, Boolean> response = KademliaClient.ping(bucketManager.getMyNode(), bootstrapNode);

        if(!response.getSecond().booleanValue()){
            logger.log(Level.SEVERE, "Bootstrap failed. Could not contact node " + bootstrapNode + " for bootstrap.");
//            return false;
            return;
        }

        bucketManager.insertNode(response.getFirst());

        List<KademliaNode> nodes = client.findNode(bucketManager.getMyNode().getNodeID());

        if(nodes.isEmpty()){
            logger.log(Level.SEVERE, "Bootstrap failed. Received an empty list while performing a node lookup for own node ID.");
//            return false;
            return;
        }

        for(KademliaNode node : nodes){
            Pair<KademliaNode, Boolean> ret = KademliaClient.ping(bucketManager.getMyNode(), node);
            KademliaNode retNode = ret.getFirst();

            if (retNode != null) {
                bucketManager.insertNode(retNode);
//                logger.log(Level.INFO, "Inserted node " + retNode);
            }
            else {
                logger.log(Level.INFO, "Could not PING node " + node);
                bucketManager.removeNode(node);
            }
        }

        byte[] distance = new byte[Utils.hashAlgorithmLengthInBytes];
        Random random = new SecureRandom();
        random.nextBytes(distance);

        HashMap<String, KademliaNode> contactedNodes = new HashMap<>();

        for(int i = 0; i < Utils.hashAlgorithmLengthInBytes; i++){

            for(int j = 1; j < 8; j++){
//                logger.log(Level.INFO, "Bootstrapping kbucket " + String.valueOf(i*8+j) + "...");

                BitSet bits = new BitSet(8);
                bits.set(0, 8);

                for(int k = 0; k < j; k++){
                    // Shift 1 zero into the start of the value
                    bits.clear(k);
                }

                bits.flip(0, 8);        // Flip the bits since they're in reverse order

                distance[i] = (byte)bits.toByteArray()[0];


                BigInteger distanceXORed = KademliaUtils.distanceTo(bucketManager.getMyNode().getNodeID(), distance);


                nodes = client.findNode(distanceXORed.toByteArray());

                if(nodes.isEmpty())
                    logger.log(Level.SEVERE, "Bootstrap failed for distance " + i + ". Received an empty list while performing a node lookup.");

                for(KademliaNode node : nodes){
                    if(contactedNodes.containsKey(Utils.bytesToHexString(node.getNodeID())))
                        continue;

                    Pair<KademliaNode, Boolean> ret = KademliaClient.ping(bucketManager.getMyNode(), node);
                    KademliaNode retNode = ret.getFirst();

                    if (retNode != null) {
                        bucketManager.insertNode(retNode);
//                        logger.log(Level.INFO, "Inserted node " + retNode);
                    }
                    else {
                        bucketManager.removeNode(node);
                        logger.log(Level.WARNING, "Could not PING node " + node);
                    }

                    contactedNodes.put(Utils.bytesToHexString(retNode.getNodeID()), retNode);
                }
            }
        }

        logger.log(Level.INFO, "Bootstrapping kademlia network done.");
//        return true;
        return;
    }

    public void start () {
        if(t == null) {
            t = new Thread(this, "kademliaBootstrapThread");
            t.start();
        }
    }
}
