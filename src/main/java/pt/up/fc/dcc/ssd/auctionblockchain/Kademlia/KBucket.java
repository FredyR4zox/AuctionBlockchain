package pt.up.fc.dcc.ssd.auctionblockchain.Kademlia;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KBucket {
    private static final Logger logger = Logger.getLogger(KBucket.class.getName());

    private final LinkedList<KademliaNode> nodes;
    private final Set<byte[]> nodeIDs;


    public KBucket() {
        this(new ArrayList<>());
    }

    public KBucket(KademliaNode[] nodes){
        this(Arrays.asList(nodes));
    }

    public KBucket(List<KademliaNode> nodes){
        this.nodes = new LinkedList<>();
        this.nodeIDs = new HashSet<>();

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
        // This function inserts the element at the end of the list, assuming the
        // last time seen is the most recent (without checking the lastTimeSeen variable)
        if (nodes.contains(node)) {
            nodes.remove(node);
            nodes.addLast(node);

//            System.out.println("Updated node: ");
//            for(int j=0; j<KademliaUtils.idSizeInBytes; j++)
//                System.out.print(" " + node.getNodeID()[j]);
//            System.out.println("\n\n");

//            logger.log(Level.INFO, "Updated node in bucket");
            return true;
        }
        else if (nodes.size() == KademliaUtils.k){
//            logger.log(Level.INFO, "Bucket full");
            return false;
        }
        else if(nodes.size() > KademliaUtils.k){
            logger.log(Level.SEVERE, "Error: Bucket size (" + nodes.size() + ") bigger than k (" + KademliaUtils.k + ")");
            return false;
        }

        nodes.addLast(node);
//        logger.log(Level.INFO, "Added node to bucket and now has length of " + nodes.size());
        return true;
    }

    public boolean removeNode(KademliaNode node){
        return nodes.remove(node);
    }

    public int size(){
        return nodes.size();
    }

    public List<KademliaNode> getNodes(){
        return new ArrayList<>(nodes);
    }
}
