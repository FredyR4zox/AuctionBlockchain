package pt.up.fc.dcc.ssd.auctionblockchain;

import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

public class KBucket {
    private static final Logger logger = Logger.getLogger(KademliaNode.class.getName());

    private static final int k = 20;
    private final int i;
    private SortedSet<KademliaNode> nodes;
    private Set<byte[]> nodeIDs;

    public KBucket(int i) {
        this(i, new TreeSet<KademliaNode>());
    }

    public KBucket(int i, Collection<KademliaNode> nodes){
        this.i = i;
        this.nodes = Collections.synchronizedSortedSet(new TreeSet<KademliaNode>(new KademliaNodeCompare()));

        for(KademliaNode node : nodes)
            this.nodeIDs.add(node.getNodeID());
    }

    public int getI() {
        return i;
    }

    public boolean insertNode(KademliaNode node){
        if(nodeIDs.contains(node.getNodeID())){
            synchronized(nodes) {
                SortedSet<KademliaNode> nodesAux = new TreeSet<KademliaNode>(new KademliaNodeCompare());
                if(!nodesAux.addAll(nodes)) {
                    logger.warning("Could not addAll nodes to new set.");
                    return false;
                }

                for (KademliaNode nodeAux : nodesAux){
                    if(nodeAux.getNodeID() == node.getNodeID()) {
                        nodes.remove(nodeAux);
                        nodeAux.updateLastSeen(node.getLastSeen());
                        nodes.add(nodeAux);

                        logger.info("Updated node of KBucket " + i);
                        return true;
                    }
                }
            }
        }
        else if (nodes.size() == k){
            synchronized(nodes) {
                KademliaNode first = nodes.first();
                if(!KademliaClient.ping(first)){
                    nodes.remove(first);
                    nodeIDs.remove(first.getNodeID());

                    nodes.add(node);
                    nodeIDs.add(node.getNodeID());

                    logger.info("Added new node to KBucket " + i);
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

            logger.info("Added new node to KBucket " + i);
            return true;
        }

        logger.info("Could not add new node to KBucket " + i);
        return false;
    }

    public KademliaNode[] getNodes(){
        KademliaNode[] tempNodes = new KademliaNode[nodes.size()];

        synchronized(nodes) {
            SortedSet<KademliaNode> nodesAux = new TreeSet<KademliaNode>(new KademliaNodeCompare());
            if (!nodesAux.addAll(nodes)) {
                logger.warning("Could not addAll nodes to new set.");
                return tempNodes;
            }

            int i = 0;
            for (KademliaNode nodeAux : nodesAux) {
                tempNodes[i] = new KademliaNode(nodeAux);
            }
        }
        return tempNodes;
    }


    class KademliaNodeCompare implements Comparator<KademliaNode>
    {
        public int compare(KademliaNode k1, KademliaNode k2)
        {
//            if (k1.getLastSeen() < k2.getLastSeen()) return -1;
//            if (k1.getLastSeen() > k2.getLastSeen()) return 1;
//            else return 0;
            if (k1.getLastSeen() < k2.getLastSeen()) return -1;
            else return 1;
        }
    }
}
