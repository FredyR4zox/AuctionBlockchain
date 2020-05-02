package pt.up.fc.dcc.ssd.auctionblockchain;

import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

public class KBucket {
    private static final Logger logger = Logger.getLogger(KademliaNode.class.getName());

    private final Set<KademliaNode> nodes;


    public KBucket() {
        this(new ArrayList<>());
    }

    public KBucket(KademliaNode[] nodes){
        this(Arrays.asList(nodes));
    }

    public KBucket(List<KademliaNode> nodes){
        this.nodes = new TreeSet<>(new KademliaNodeCompare());

        for(KademliaNode node : nodes)
            this.insertNode(node);
    }

    public KBucket(KBucket kBucket) {
        this(kBucket.getNodes());
    }


    public boolean contains(KademliaNode node){
        return nodes.contains(node);
    }

    public boolean insertNode(KademliaNode node){
        //TODO: Don't change the whole object, only update the last time seen
        if (nodes.contains(node)) {
            nodes.remove(node);
            nodes.add(node);

            logger.info("Updated node in bucket");
            return true;
        }
        else if (nodes.size() == KademliaUtils.k){
            logger.warning("Error: Bucket full");
            return false;
        }
        else if(nodes.size() > KademliaUtils.k){
            logger.warning("Error: Bucket size (" + nodes.size() + ") bigger than k (" + KademliaUtils.k);
            return false;
        }

        nodes.add(node);
        logger.info("Added node to bucket");
        return true;
    }

    public boolean removeNode(KademliaNode node){
        return nodes.remove(node);
    }

    public int size(){
        return nodes.size();
    }

//    public KademliaNode getNode(int i) {
//        if(i < 0 || i >= nodes.size())
//            return null;
//
//        return nodes. (i);
//    }

    public List<KademliaNode> getNodes(){
        return new ArrayList<>(nodes);
    }
//
//    public Set<byte[]> getNodeIDs(){
//        return new HashSet<>(nodeIDs);
//    }



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
