package pt.up.fc.dcc.ssd.auctionblockchain;

import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

public class KBucket {
    private static final Logger logger = Logger.getLogger(KademliaNode.class.getName());

    private final List<KademliaNode> nodes;
    private final Set<byte[]> nodeIDs;


    public KBucket() {
        this(new ArrayList<KademliaNode>());
    }

    public KBucket(KademliaNode[] nodes){
        this(Arrays.asList(nodes));
    }

    public KBucket(List<KademliaNode> nodes){
        this.nodes = new ArrayList<KademliaNode>();
        this.nodeIDs = new HashSet<>();

        for(KademliaNode node : nodes)
            this.insertNode(node);
    }

    public KBucket(KBucket kBucket) {
        this(kBucket.getNodes());
    }



    public boolean insertNode(KademliaNode node){
        if(nodeIDs.contains(node.getNodeID())){
            synchronized(nodes) {
                List<KademliaNode> tempNodes = new ArrayList<>(nodes);

                for (KademliaNode nodeAux : tempNodes){
                    if(nodeAux.getNodeID() == node.getNodeID()) {
                        nodes.remove(nodeAux);
                        nodeAux.updateLastSeen(node.getLastSeen());
                        nodes.add(nodeAux);

                        logger.info("Updated node");
                        return true;
                    }
                }
            }
        }
        else if (nodes.size() == KademliaUtil.k){
            synchronized(nodes) {
                KademliaNode first = nodes.get(0);
                if(!KademliaClient.ping(first)){
                    nodes.remove(first);
                    nodeIDs.remove(first.getNodeID());

                    nodes.add(node);
                    nodeIDs.add(node.getNodeID());

                    logger.info("Added new node");
                    return true;
                }
                else {
                    nodes.remove(first);
                    first.updateLastSeen(Instant.now().getEpochSecond());
                    nodes.add(first);

                    return false;
                }
            }
        }
        else {
            synchronized(nodes) {
                nodes.add(node);
                nodeIDs.add(node.getNodeID());
            }

            logger.info("Added new node");
            return true;
        }

        logger.info("Could not add new node");
        return false;
    }

    public List<KademliaNode> getNodes(){
        return nodes;
    }


//    class KademliaNodeCompare implements Comparator<KademliaNode>
//    {
//        public int compare(KademliaNode k1, KademliaNode k2)
//        {
////            if (k1.getLastSeen() < k2.getLastSeen()) return -1;
////            if (k1.getLastSeen() > k2.getLastSeen()) return 1;
////            else return 0;
//            if (k1.getLastSeen() < k2.getLastSeen()) return -1;
//            else return 1;
//        }
//    }
}
