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

    public static Pair<KademliaNode, Boolean> ping(KademliaNode myNode, KademliaNode node) {
        return pingGRPC(myNode, node);
    }

    private static Pair<KademliaNode, Boolean> pingGRPC(KademliaNode myNode, KademliaNode node){
        ManagedChannel channel = ManagedChannelBuilder.forTarget(node.getIpAddress() + ":" + node.getPort()).usePlaintext().build();
        AuctionBlockchainBlockingStub blockingStub = AuctionBlockchainGrpc.newBlockingStub(channel);

        KademliaNodeProto response;

        try {
            response = blockingStub.ping(KademliaUtils.KademliaNodeToKademliaNodeProto(myNode));
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "PING RPC failed for " + node.getIpAddress() + ":" + node.getPort() + " - " + e.getStatus());
            return new Pair(null, false);
        }


        KademliaNode nodeInResponse =  KademliaUtils.KademliaNodeProtoToKademliaNode(response);

        if (!Arrays.equals(node.getNodeID(), nodeInResponse.getNodeID())) {
            logger.log(Level.WARNING, "Response returned with different nodeID");
            return new Pair(nodeInResponse, false);
        }

//        bucketManager.insertNode(nodeInResponse);

        return new Pair(nodeInResponse, true);
    }

    private static <T> Pair<KademliaNode, Boolean> storeGRPC(KademliaNode myNode, KademliaNode node, byte[] key, T info) {
        StoreRequest.Builder request = StoreRequest.newBuilder()
                .setNode(KademliaUtils.KademliaNodeToKademliaNodeProto(myNode))
                .setKey(ByteString.copyFrom(key));


        if(info.getClass() == Transaction.class) {
            request.setTransaction(KademliaUtils.TransactionToTransactionProto((Transaction) info));
        }
        else if(info.getClass() == Block.class) {
            request.setBlock(KademliaUtils.BlockToBlockProto((Block) info));
        }
        else {
            logger.log(Level.SEVERE, "Error: store value type not valid - " + info.getClass());
            return new Pair(null, false);
        }

        ManagedChannel channel = ManagedChannelBuilder.forTarget(node.getIpAddress() + ":" + node.getPort()).usePlaintext().build();
        AuctionBlockchainBlockingStub blockingStub = AuctionBlockchainGrpc.newBlockingStub(channel);

        StoreResponse response;

        try {
            response = blockingStub.store(request .build());
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "STORE RPC failed for " + node.getIpAddress() + ":" + node.getPort() + " - " + e.getStatus());
            return new Pair(null, false);
        }


        KademliaNode nodeInResponse =  KademliaUtils.KademliaNodeProtoToKademliaNode(response.getNode());

        if (!Arrays.equals(node.getNodeID(), nodeInResponse.getNodeID())) {
            logger.log(Level.WARNING, "Response returned with different nodeID");
            return new Pair(nodeInResponse, false);
        }

//        bucketManager.insertNode(nodeInResponse);

        return new Pair(nodeInResponse, response.getSuccess());
    }

    private static <T> Pair<KademliaNode, List<T>> findNodeGRPC(KademliaNode myNode, KademliaNode node, byte[] requestedID) {
        FindNodeRequest request = FindNodeRequest.newBuilder()
                .setNode(KademliaUtils.KademliaNodeToKademliaNodeProto(myNode))
                .setRequestedNodeId(KademliaUtils.NodeIDToNodeIDProto(requestedID))
                .build();

        ManagedChannel channel = ManagedChannelBuilder.forTarget(node.getIpAddress() + ":" + node.getPort()).usePlaintext().build();
        AuctionBlockchainBlockingStub blockingStub = AuctionBlockchainGrpc.newBlockingStub(channel);

        FindNodeResponse response;

        try {
            response = blockingStub.findNode(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "FIND_NODE RPC failed for " + node.getIpAddress() + ":" + node.getPort() + " - " + e.getStatus());
            return new Pair(null, new ArrayList<>());
        }


        KademliaNode nodeInResponse =  KademliaUtils.KademliaNodeProtoToKademliaNode(response.getNode());

        if (!Arrays.equals(node.getNodeID(), nodeInResponse.getNodeID())) {
            logger.log(Level.WARNING, "Response returned with different nodeID");
            return new Pair(nodeInResponse, new ArrayList<>());
        }

//        bucketManager.insertNode(nodeInResponse);

        return new Pair(nodeInResponse, (List<T>)KademliaUtils.KBucketProtoToKademliaNodeList(response.getBucket()));
    }

    private static <T> Pair<KademliaNode, List<T>> findValueGRPC(KademliaNode myNode, KademliaNode node, byte[] key) {
        FindValueRequest request = FindValueRequest.newBuilder()
                .setNode(KademliaUtils.KademliaNodeToKademliaNodeProto(myNode))
                .setKey(ByteString.copyFrom(key))
                .build();

        ManagedChannel channel = ManagedChannelBuilder.forTarget(node.getIpAddress() + ":" + node.getPort()).usePlaintext().build();
        AuctionBlockchainBlockingStub blockingStub = AuctionBlockchainGrpc.newBlockingStub(channel);

        FindValueResponse response;

        try {
            response = blockingStub.findValue(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "FIND_VALUE RPC failed for " + node.getIpAddress() + ":" + node.getPort() + " - " + e.getStatus());
            return new Pair(null, new ArrayList<>());
        }


        KademliaNode nodeInResponse =  KademliaUtils.KademliaNodeProtoToKademliaNode(response.getNode());

        if (!Arrays.equals(node.getNodeID(), nodeInResponse.getNodeID())) {
            logger.log(Level.WARNING, "Response returned with different nodeID");
            return new Pair(nodeInResponse, new ArrayList<>());
        }

//        bucketManager.insertNode(nodeInResponse);

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

        return new Pair(nodeInResponse, ret);
    }


    // nodeKey - nodes to search
    // key - key to search
    // This is done so that if we want mempool from certain nodes
    private static <T> List<T> contactNNodes(KBucketManager bucketManager, KademliaNode myNode, byte[] nodeKey, byte[] key, TreeSet<KademliaNodeWrapper> shortList, TreeSet<KademliaNodeWrapper> probedNodes, int n, ThreeParameterFunction<KademliaNode, KademliaNode, byte[], Pair<KademliaNode, List<T>> > rpc) {
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

            Pair<KademliaNode, List<T>> retAux = rpc.apply(myNode, node, key);

            if(retAux.getSecond().isEmpty()) {
                logger.log(Level.INFO, "Node " + node + " didn't return anything. Removing from list.");
                shortList.remove(node);
            }
            else {
                logger.log(Level.INFO, "Node " + node + " returned some things.");
                bucketManager.insertNode(retAux.getFirst());
                probedNodes.add(node);
            }

            // Set node timestamp as the distance
            for(T aux : retAux.getSecond()) {
                if (aux.getClass() == KademliaNode.class) {
                    KademliaNode aux2 = (KademliaNode) aux;
                    bucketManager.insertNode(aux2);

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

            contactNNodes(bucketManager, myNode, serviceKey, serviceKey, shortList, probedNodes, KademliaUtils.alpha, KademliaClient::findNodeGRPC);


            // Stop iterating if no better node was found
            if(!shortList.isEmpty()){
                if(closestNode.equals(shortList.first())) {
                    logger.log(Level.INFO, "Could not find a better node.");
                    iterationMoreClose = false;

                    contactNNodes(bucketManager, myNode, serviceKey, serviceKey, shortList, probedNodes, KademliaUtils.k, KademliaClient::findNodeGRPC);
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
            Pair<KademliaNode, Boolean> ret = storeGRPC(myNode, node, serviceKey, value);
            if (ret.getSecond().booleanValue()) {
                logger.log(Level.INFO, "Success in storeGRPC for node " + node);
                bucketManager.insertNode(ret.getFirst());
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

            contactNNodes(bucketManager, myNode, key, key, shortList, probedNodes, KademliaUtils.alpha, KademliaClient::findNodeGRPC);


            // Stop iterating if no better node was found
            if(!shortList.isEmpty()){
                if(closestNode.equals(shortList.first())) {
                    logger.log(Level.INFO, "Could not find a better node.");
                    iterationMoreClose = false;

                    contactNNodes(bucketManager, myNode, key, key, shortList, probedNodes, KademliaUtils.k, KademliaClient::findNodeGRPC);
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

            List<T> retAux = contactNNodes(bucketManager, myNode, nodeKey, key, shortList, probedNodes, KademliaUtils.alpha, KademliaClient::findValueGRPC);

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

                    retAux = contactNNodes(bucketManager, myNode, nodeKey, key, shortList, probedNodes, KademliaUtils.k, KademliaClient::findValueGRPC);
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

    public static boolean bootstrap(KBucketManager bucketManager, KademliaNode bootstrappingNode){
        Pair<KademliaNode, Boolean> response = KademliaClient.ping(bucketManager.getMyNode(), bootstrappingNode);

        if(!response.getSecond().booleanValue()){
            logger.log(Level.SEVERE, "Bootstrap failed. Could not contact node " + bootstrappingNode + " for bootstrap.");
            return false;
        }

        if(KademliaClient.findNode(bucketManager, bucketManager.getMyNode().getNodeID()).isEmpty()){
            logger.log(Level.SEVERE, "Bootstrap failed. Received an empty list while performing a node lookup for own node ID.");
            return false;
        }

        return true;
    }

    public static <T> List<Transaction> getMempool(KBucketManager bucketManager){
        byte[] mempoolKey;

        try {
            MessageDigest messageDigest = MessageDigest.getInstance(KademliaUtils.hashAlgorithm);
            mempoolKey = messageDigest.digest("mempool".getBytes(KademliaUtils.charset));
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "Error: Could not find hash algorithm " + KademliaUtils.hashAlgorithm);
            e.printStackTrace();

            return new ArrayList<>();
        }

        byte[] rand = new byte[KademliaUtils.idSizeInBytes];

        List<T> transactions = KademliaClient.findValue(bucketManager, rand, mempoolKey);

        List<Transaction> retList = new ArrayList<>();
        for(T tmp : transactions){
            if(tmp.getClass() == Transaction.class){
                logger.log(Level.INFO, "Added a transaction to the list.");
                retList.add((Transaction) tmp);
            }
            else
                logger.log(Level.SEVERE, "Error: Received information different than a transaction.");
        }

        return retList;
    }

    public static <T> List<Block> getLastKnownBlock(KBucketManager bucketManager){
        byte[] lastBlocksKey;

        try {
            MessageDigest messageDigest = MessageDigest.getInstance(KademliaUtils.hashAlgorithm);
            lastBlocksKey = messageDigest.digest("lastBlock".getBytes(KademliaUtils.charset));
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "Error: Could not find hash algorithm " + KademliaUtils.hashAlgorithm);
            e.printStackTrace();

            return new ArrayList<>();
        }

        byte[] rand = new byte[KademliaUtils.idSizeInBytes];

        List<T> blocks = KademliaClient.findValue(bucketManager, rand, lastBlocksKey);

        List<Block> retList = new ArrayList<>();
        for(T tmp : blocks){
            if(tmp.getClass() == Block.class){
                logger.log(Level.INFO, "Added a block to the list.");
                retList.add((Block) tmp);
            }
            else
                logger.log(Level.SEVERE, "Error: Received information different than a block.");
        }

        return retList;
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


