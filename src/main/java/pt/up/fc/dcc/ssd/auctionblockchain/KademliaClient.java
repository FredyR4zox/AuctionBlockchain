package pt.up.fc.dcc.ssd.auctionblockchain;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bouncycastle.crypto.prng.DigestRandomGenerator;
import org.bouncycastle.crypto.prng.RandomGenerator;
import pt.up.fc.dcc.ssd.auctionblockchain.AuctionBlockchainGrpc.AuctionBlockchainBlockingStub;
import pt.up.fc.dcc.ssd.auctionblockchain.AuctionBlockchainGrpc.AuctionBlockchainStub;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class KademliaClient {
    private static final Logger logger = Logger.getLogger(KademliaClient.class.getName());

//    private final AuctionBlockchainBlockingStub blockingStub;
//    private final AuctionBlockchainStub asyncStub;

//    public KademliaClient(String ipAddress, int port){
//        ManagedChannel channel = ManagedChannelBuilder.forTarget(ipAddress + ":" + port).usePlaintext().build();
//
//        blockingStub = AuctionBlockchainGrpc.newBlockingStub(channel);
//        asyncStub = AuctionBlockchainGrpc.newStub(channel);
//    }

    public static boolean ping(KademliaNode myNode, KademliaNode node) {
        return pingGRPC(myNode, node);
    }

    public static boolean pingGRPC(KademliaNode myNode, KademliaNode node){
        ManagedChannel channel = ManagedChannelBuilder.forTarget(node.getIpAddress() + ":" + node.getPort()).usePlaintext().build();
        AuctionBlockchainBlockingStub blockingStub = AuctionBlockchainGrpc.newBlockingStub(channel);

        NodeIDProto id;

        try {
            id = blockingStub.ping(KademliaUtils.NodeIDToNodeIDProto(myNode.getNodeID()));
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "PING RPC failed for " + node.getIpAddress() + ":" + node.getPort() + " - " + e.getStatus());
            return false;
        }


        if (!Arrays.equals(myNode.getNodeID(), KademliaUtils.NodeIDProtoToNodeID(id))) {
            logger.log(Level.WARNING, "Response returned with different nodeID");
            return false;
        }

        return true;
    }

    public static <T> boolean storeGRPC(KademliaNode myNode, KademliaNode node, byte[] key, T info) {
        StoreRequest.Builder request = StoreRequest.newBuilder()
                .setNodeID(KademliaUtils.NodeIDToNodeIDProto(myNode.getNodeID()))
                .setKey(ByteString.copyFrom(key));


        if(info.getClass() == Transaction.class) {
            request.setTransaction(KademliaUtils.TransactionToTransactionProto((Transaction) info));
        }
        else if(info.getClass() == Block.class) {
            request.setBlock(KademliaUtils.BlockToBlockProto((Block) info));
        }
        else {
            logger.log(Level.SEVERE, "Error: store value type not valid - " + info.getClass());
            return false;
        }

        ManagedChannel channel = ManagedChannelBuilder.forTarget(node.getIpAddress() + ":" + node.getPort()).usePlaintext().build();
        AuctionBlockchainBlockingStub blockingStub = AuctionBlockchainGrpc.newBlockingStub(channel);

        StoreResponse response;

        try {
            response = blockingStub.store(request .build());
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "STORE RPC failed for " + node.getIpAddress() + ":" + node.getPort() + " - " + e.getStatus());
            return false;
        }

        if (!Arrays.equals(myNode.getNodeID(), KademliaUtils.NodeIDProtoToNodeID(response.getNodeID()))) {
            logger.log(Level.WARNING, "Response returned with different nodeID");
            return false;
        }

        return response.getSuccess();
    }

    public static <T> List<T> findNodeGRPC(KademliaNode myNode, KademliaNode node, byte[] requestedID) {
        FindNodeRequest request = FindNodeRequest.newBuilder()
                .setNodeID(KademliaUtils.NodeIDToNodeIDProto(myNode.getNodeID()))
                .setRequestedNodeId(KademliaUtils.NodeIDToNodeIDProto(requestedID))
                .build();

        ManagedChannel channel = ManagedChannelBuilder.forTarget(node.getIpAddress() + ":" + node.getPort()).usePlaintext().build();
        AuctionBlockchainBlockingStub blockingStub = AuctionBlockchainGrpc.newBlockingStub(channel);

        FindNodeResponse response;

        try {
            response = blockingStub.findNode(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "FIND_NODE RPC failed for " + node.getIpAddress() + ":" + node.getPort() + " - " + e.getStatus());
            return new ArrayList<>();
        }

        if (!Arrays.equals(myNode.getNodeID(), KademliaUtils.NodeIDProtoToNodeID(response.getNodeID()))) {
            logger.log(Level.WARNING, "Response returned with different nodeID");
            return new ArrayList<>();
        }

        return (List<T>)KademliaUtils.KBucketProtoToKademliaNodeList(response.getBucket());
    }

    public static <T> List<T> findValueGRPC(KademliaNode myNode, KademliaNode node, byte[] key) {
        FindValueRequest request = FindValueRequest.newBuilder()
                .setNodeID(KademliaUtils.NodeIDToNodeIDProto(myNode.getNodeID()))
                .setKey(ByteString.copyFrom(key))
                .build();

        ManagedChannel channel = ManagedChannelBuilder.forTarget(node.getIpAddress() + ":" + node.getPort()).usePlaintext().build();
        AuctionBlockchainBlockingStub blockingStub = AuctionBlockchainGrpc.newBlockingStub(channel);

        FindValueResponse response;

        try {
            response = blockingStub.findValue(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "FIND_VALUE RPC failed for " + node.getIpAddress() + ":" + node.getPort() + " - " + e.getStatus());
            return new ArrayList<>();
        }

        if (!Arrays.equals(myNode.getNodeID(), KademliaUtils.NodeIDProtoToNodeID(response.getNodeID()))) {
            logger.log(Level.WARNING, "Response returned with different nodeID");
            return new ArrayList<>();
        }

        List<T> ret = new ArrayList<>();
        if (response.getBlockOrTransactionsOrNodesCase() == FindValueResponse.BlockOrTransactionsOrNodesCase.BLOCK) {
            ret.add((T)KademliaUtils.BlockProtoToBlock(response.getBlock()));
        }
        else if (response.getBlockOrTransactionsOrNodesCase() == FindValueResponse.BlockOrTransactionsOrNodesCase.TRANSACTIONS) {
            ret.addAll((List<T>)KademliaUtils.MempoolProtoToTransactionList(response.getTransactions()));
        }
        else if (response.getBlockOrTransactionsOrNodesCase() == FindValueResponse.BlockOrTransactionsOrNodesCase.BUCKET) {
            ret.addAll((List<T>)KademliaUtils.KBucketProtoToKademliaNodeList(response.getBucket()));
        }
        else
            logger.log(Level.SEVERE, "Error: findValueResponse without BlockOrTransactionsOrNodesCase");

        return ret;
    }


    // nodeKey - nodes to search
    // key - key to search
    // This is done so that if we want mempool from certain nodes
    private static <T> List<T> contactNNodes(KademliaNode myNode, byte[] nodeKey, byte[] key, TreeSet<KademliaNodeWrapper> shortList, TreeSet<KademliaNodeWrapper> probedNodes, int n, ThreeParameterFunction<KademliaNode, KademliaNode, byte[], List<T> > rpc) {
        // Select alpha nodes to query
        List<KademliaNodeWrapper> tempAlphaList = new ArrayList<>();
        Iterator it = shortList.iterator();
        while (tempAlphaList.size() < KademliaUtils.alpha && it.hasNext()) {
            KademliaNodeWrapper node = (KademliaNodeWrapper) it.next();

            if (probedNodes.contains(node))
                continue;

            tempAlphaList.add(node);
        }

        logger.log(Level.INFO, "tempAlphaList with " + tempAlphaList.size() + " nodes");

        // Variable to store the returned objects in case the RPC is Find_Value
        List<T> ret = new ArrayList<>();

        // Query selected alpha nodes
        for(KademliaNodeWrapper node : tempAlphaList) {

            List<T> retAux = rpc.apply(myNode, node, key);

            if(retAux.isEmpty()) {
                logger.log(Level.INFO, "Node " + node + " didn't return anything. Removing from list.");
                shortList.remove(node);
            }
            else {
                logger.log(Level.INFO, "Node " + node + " returned some things.");
                probedNodes.add(node);
            }

            // Set node timestamp as the distance
            for(T aux : retAux) {
                if (aux.getClass() == KademliaNode.class) {
                    KademliaNode aux2 = (KademliaNode) aux;
                    KademliaNodeWrapper aux3 = new KademliaNodeWrapper(aux2, KademliaUtils.distanceTo(aux2.getNodeID(), nodeKey));
                    shortList.add(aux3);
                    logger.log(Level.INFO, "Node " + aux2 + " added.");
                }
                else {
                    logger.log(Level.INFO, "Added some information");
                    ret.add(aux);
                }
            }
        }

        return ret;
    }

    @FunctionalInterface
    public interface ThreeParameterFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }

//    private static KademliaNode closestNode(List<KademliaNode> nodeList, byte[] key) {
//        if (nodeList.isEmpty())
//            return null;
//
//        // "KademliaNode bestNode = nodeList.get(0)" so that IDE stops complaining, otherwise "KademliaNode bestNode;"
//        KademliaNode bestNode = nodeList.get(0);
//        int bestDistance = Integer.MAX_VALUE;
//
//        for(KademliaNode node : nodeList) {
//            int distance = KademliaUtils.distanceTo(node.getNodeID(), key);
//            if (distance < bestDistance){
//                bestNode = node;
//                bestDistance = distance;
//            }
//        }
//
//        return bestNode;
//    }





    public static <T> boolean store(KBucketManager bucketManager, byte[] serviceKey, T value) {
        KademliaNode myNode = bucketManager.getMyNode();

        TreeSet<KademliaNodeWrapper> shortList = new TreeSet<>(new KademliaNodeWrapperCompare());
        TreeSet<KademliaNodeWrapper> probedNodes = new TreeSet<>(new KademliaNodeWrapperCompare());

        //Hack: Use the comparator that compares timestamps to compare distances by putting the distance as the timestamp
        List<KademliaNode> nodes = bucketManager.getClosestNodes(null, serviceKey, KademliaUtils.alpha);

        if(nodes.isEmpty()){
            logger.log(Level.WARNING, "No nodes found to send STORE RPC");
            return false;
        }

        for(KademliaNode node : nodes)
            shortList.add(new KademliaNodeWrapper(node, KademliaUtils.distanceTo(node.getNodeID(), serviceKey)));
//            node.updateLastSeen(KademliaUtils.distanceTo(node.getNodeID(), serviceKey));
//
//        shortList.addAll(hackedNodes);

        KademliaNode closestNode;
        boolean iterationMoreClose = true;

        // Stop iterating if no better node was found or we already probed more than k nodes
        while(iterationMoreClose && probedNodes.size() < KademliaUtils.k) {
            closestNode = shortList.first();

            contactNNodes(myNode, serviceKey, serviceKey, shortList, probedNodes, KademliaUtils.alpha, KademliaClient::findNodeGRPC);


            // Stop iterating if no better node was found
            if(!shortList.isEmpty()){
                if(closestNode.equals(shortList.first())) {
                    logger.log(Level.INFO, "Could not find a better node.");
                    iterationMoreClose = false;

                    contactNNodes(myNode, serviceKey, serviceKey, shortList, probedNodes, KademliaUtils.k, KademliaClient::findNodeGRPC);
                }
            }
            else {
                logger.log(Level.INFO, "Could not find a better node because the list was empty.");
                iterationMoreClose = false;
            }
        }


        List<KademliaNodeWrapper> closestNodes = new ArrayList<>(probedNodes).subList(0, Math.min(probedNodes.size(), KademliaUtils.k));
        probedNodes.clear();
        probedNodes.addAll(closestNodes);

        boolean success = false;
        for(KademliaNode node : probedNodes) {
            if (storeGRPC(myNode, node, serviceKey, value)) {
                logger.log(Level.INFO, "Success in storeGRPC for node " + node);
                success = true;
            }
        }

        return success;
    }

    public static <T> List<T> findNode(KBucketManager bucketManager, byte[] key) {
        KademliaNode myNode = bucketManager.getMyNode();

        TreeSet<KademliaNodeWrapper> shortList = new TreeSet<>(new KademliaNodeWrapperCompare());
        TreeSet<KademliaNodeWrapper> probedNodes = new TreeSet<>(new KademliaNodeWrapperCompare());

        //Hack: Use the comparator that compares timestamps to compare distances by putting the distance as the timestamp
        List<KademliaNode> nodes = bucketManager.getClosestNodes(null, key, KademliaUtils.alpha);

        if(nodes.isEmpty()){
            logger.log(Level.WARNING, "No nodes found to send FIND_NODE RPC");
            return new ArrayList<>();
        }

        for(KademliaNode node : nodes)
            shortList.add(new KademliaNodeWrapper(node, KademliaUtils.distanceTo(node.getNodeID(), key)));
//            node.updateLastSeen(KademliaUtils.distanceTo(node.getNodeID(), key));
//
//        shortList.addAll(hackedNodes);

        KademliaNode closestNode;
        boolean iterationMoreClose = true;

        // Stop iterating if no better node was found or we already probed more than k nodes
        while(iterationMoreClose && probedNodes.size() < KademliaUtils.k) {
            closestNode = shortList.first();

            contactNNodes(myNode, key, key, shortList, probedNodes, KademliaUtils.alpha, KademliaClient::findNodeGRPC);


            // Stop iterating if no better node was found
            if(!shortList.isEmpty()){
                if(closestNode.equals(shortList.first())) {
                    logger.log(Level.INFO, "Could not find a better node.");
                    iterationMoreClose = false;

                    contactNNodes(myNode, key, key, shortList, probedNodes, KademliaUtils.k, KademliaClient::findNodeGRPC);
                }
            }
            else {
                logger.log(Level.INFO, "Could not find a better node because the list was empty.");
                iterationMoreClose = false;
            }
        }

        return (List<T>) new ArrayList<>(probedNodes).subList(0, Math.min(probedNodes.size(), KademliaUtils.k));
    }

    public static <T> List<T> findValue(KBucketManager bucketManager, byte[] nodeKey, byte[] key) {
        KademliaNode myNode = bucketManager.getMyNode();

        TreeSet<KademliaNodeWrapper> shortList = new TreeSet<>(new KademliaNodeWrapperCompare());
        TreeSet<KademliaNodeWrapper> probedNodes = new TreeSet<>(new KademliaNodeWrapperCompare());

        List<KademliaNode> nodes = bucketManager.getClosestNodes(null, nodeKey, KademliaUtils.alpha);

        if(nodes.isEmpty()){
            logger.log(Level.WARNING, "No nodes found to send RPC");
            return new ArrayList<>();
        }

        for(KademliaNode node : nodes)
            shortList.add(new KademliaNodeWrapper(node, KademliaUtils.distanceTo(node.getNodeID(), nodeKey)));
//            node.updateLastSeen(KademliaUtils.distanceTo(node.getNodeID(), nodeKey));
//
//        shortList.addAll(nodes);

        KademliaNode closestNode;
        boolean iterationMoreClose = true;


        // Stop iterating if no better node was found or we already probed more than k nodes
        while(iterationMoreClose && probedNodes.size() < KademliaUtils.k) {
            closestNode = shortList.first();

            List<T> retAux = contactNNodes(myNode, nodeKey, key, shortList, probedNodes, KademliaUtils.alpha, KademliaClient::findValueGRPC);

            // If we received anything from the nodes
            if(!retAux.isEmpty()) {
                logger.log(Level.INFO, "Received data.");
                return retAux;
            }

            // Stop iterating if no better node was found
            if(!shortList.isEmpty()) {
                if (closestNode.equals(shortList.first())) {
                    logger.log(Level.INFO, "Could not find a better node.");
                    iterationMoreClose = false;

                    retAux = contactNNodes(myNode, nodeKey, key, shortList, probedNodes, KademliaUtils.k, KademliaClient::findValueGRPC);
                    if (!retAux.isEmpty()) {
                        logger.log(Level.INFO, "Received data.");
                        return retAux;
                    }
                }
            }
            else {
                logger.log(Level.INFO, "Could not find a better node because the list was empty.");
                iterationMoreClose = false;
            }
        }

        logger.log(Level.INFO, "Returning an empty list.");
        return new ArrayList<>();
    }

    // https://dzone.com/articles/how-to-sort-a-map-by-value-in-java-8
    public static Map<KademliaNode, BigInteger> sortMapByValue(final Map<KademliaNode, BigInteger> nodes) {

        return nodes.entrySet()
                .stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private static class KademliaNodeWrapper extends KademliaNode{
        private final BigInteger distance;

        public KademliaNodeWrapper(KademliaNode node, BigInteger distance) {
            super(node);
            this.distance = distance;
        }

        public BigInteger getDistance() {
            return distance;
        }
    }

    static class KademliaNodeWrapperCompare implements Comparator<KademliaNodeWrapper>
    {
        public int compare(KademliaNodeWrapper k1, KademliaNodeWrapper k2)
        {
            return k1.getDistance().compareTo(k2.getDistance());
        }
    }
}


