package pt.up.fc.dcc.ssd.auctionblockchain;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import pt.up.fc.dcc.ssd.auctionblockchain.AuctionBlockchainGrpc.AuctionBlockchainBlockingStub;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.Block;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.BlockchainUtils;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.Transaction;

public class KademliaClient {
    private static final Logger logger = Logger.getLogger(KademliaClient.class.getName());

    private final KBucketManager bucketManager;


    public KademliaClient(KBucketManager bucketManager){
        this.bucketManager = bucketManager;
    }

    public static Pair<KademliaNode, Boolean> ping(KademliaNode myNode, KademliaNode node) {
        return pingGRPC(myNode, node);
    }

    public boolean bootstrap(KademliaNode bootstrappingNode){
        Pair<KademliaNode, Boolean> response = KademliaClient.ping(bucketManager.getMyNode(), bootstrappingNode);

        if(!response.getSecond().booleanValue()){
            logger.log(Level.SEVERE, "Bootstrap failed. Could not contact node " + bootstrappingNode + " for bootstrap.");
            return false;
        }

        if(findNode(bucketManager.getMyNode().getNodeID()).isEmpty()){
            logger.log(Level.SEVERE, "Bootstrap failed. Received an empty list while performing a node lookup for own node ID.");
            return false;
        }

        return true;
    }

    public <T> List<Transaction> getMempool(){
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

        Random random = new SecureRandom();
        random.nextBytes(rand);

        List<T> transactions = findValue(rand, mempoolKey);

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

    public <T> Block findBlockWithPreviousHash(String hash){
        byte[] hashBytes = hash.getBytes(KademliaUtils.charset);

        List<T> values = findValue(hashBytes, hashBytes);

        for(T val : values){
            if(val.getClass() == Block.class) {
                Block block = (Block) val;
                if(block.getPreviousHash().equals(hash))
                    return block;
            }
        }

        return null;
    }

    public void getBlockChain(){
        String lastBlockHash = BlockchainUtils.getLongestChain().getLastBlockHash();

        while(true){
            byte[] lastBlockHashBytes = lastBlockHash.getBytes(KademliaUtils.charset);

            List<Block> blocks = findValue(lastBlockHashBytes, lastBlockHashBytes);

            if(blocks.isEmpty())
                break;

            Block block = null;
            for(int i=0; i<blocks.size(); i++) {
                Block blockAux = blocks.get(i);
                if (blockAux.getPreviousHash().equals(lastBlockHash)){
                    block = blockAux;
                    break;
                }
            }

            if(block == null) {
                logger.log(Level.WARNING, "findValue returned wrong block");
                break;
            }

            BlockchainUtils.addBlock(block);

            lastBlockHash = BlockchainUtils.getLongestChain().getLastBlockHash();
        }
    }

    public void announceNewBlock(Block block){
        byte[] rand = new byte[KademliaUtils.idSizeInBytes];

        Random random = new SecureRandom();
        random.nextBytes(rand);

        if(store(rand, block)){
            logger.log(Level.INFO, "Announced new block.");
        }
        else
            logger.log(Level.INFO, "Could not announce new block.");
    }



    /***
     * ##########################################
     * # Private functions (internal functions) #
     * ########################################## */

    private static Pair<KademliaNode, Boolean> pingGRPC(KademliaNode myNode, KademliaNode node){
        ManagedChannel channel = ManagedChannelBuilder.forTarget(node.getIpAddress() + ":" + node.getPort()).usePlaintext().build();
        AuctionBlockchainBlockingStub blockingStub = AuctionBlockchainGrpc.newBlockingStub(channel);

        KademliaNodeProto response;

        try {
            response = blockingStub.ping(KademliaUtils.KademliaNodeToKademliaNodeProto(myNode));
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "PING RPC failed for " + node.getIpAddress() + ":" + node.getPort() + " - " + e.getStatus());
            closeChannel(channel);
            return new Pair(null, false);
        }


        KademliaNode nodeInResponse =  KademliaUtils.KademliaNodeProtoToKademliaNode(response);

        if (!Arrays.equals(node.getNodeID(), nodeInResponse.getNodeID())) {
            logger.log(Level.WARNING, "Response returned with different nodeID");
            closeChannel(channel);
            return new Pair(nodeInResponse, false);
        }

        closeChannel(channel);

        return new Pair(nodeInResponse, true);
    }

    private <T> Pair<KademliaNode, Boolean> storeGRPC(KademliaNode node, byte[] key, T info) {
        StoreRequest.Builder request = StoreRequest.newBuilder()
                .setNode(KademliaUtils.KademliaNodeToKademliaNodeProto(bucketManager.getMyNode()))
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
            closeChannel(channel);
            return new Pair(null, false);
        }


        KademliaNode nodeInResponse =  KademliaUtils.KademliaNodeProtoToKademliaNode(response.getNode());

        if (!Arrays.equals(node.getNodeID(), nodeInResponse.getNodeID())) {
            logger.log(Level.WARNING, "Response returned with different nodeID");
            closeChannel(channel);
            return new Pair(nodeInResponse, false);
        }

        closeChannel(channel);

        return new Pair(nodeInResponse, response.getSuccess());
    }

    private <T> Pair<KademliaNode, List<T>> findNodeGRPC(KademliaNode node, byte[] requestedID) {
        FindNodeRequest request = FindNodeRequest.newBuilder()
                .setNode(KademliaUtils.KademliaNodeToKademliaNodeProto(bucketManager.getMyNode()))
                .setRequestedNodeId(KademliaUtils.NodeIDToNodeIDProto(requestedID))
                .build();

        ManagedChannel channel = ManagedChannelBuilder.forTarget(node.getIpAddress() + ":" + node.getPort()).usePlaintext().build();
        AuctionBlockchainBlockingStub blockingStub = AuctionBlockchainGrpc.newBlockingStub(channel);

        FindNodeResponse response;

        try {
            response = blockingStub.findNode(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "FIND_NODE RPC failed for " + node.getIpAddress() + ":" + node.getPort() + " - " + e.getStatus());
            closeChannel(channel);
            return new Pair(null, new ArrayList<>());
        }


        KademliaNode nodeInResponse =  KademliaUtils.KademliaNodeProtoToKademliaNode(response.getNode());

        if (!Arrays.equals(node.getNodeID(), nodeInResponse.getNodeID())) {
            logger.log(Level.WARNING, "Response returned with different nodeID");
            closeChannel(channel);
            return new Pair(nodeInResponse, new ArrayList<>());
        }

        closeChannel(channel);

        return new Pair(nodeInResponse, (List<T>)KademliaUtils.KBucketProtoToKademliaNodeList(response.getBucket()));
    }

    private <T> Pair<KademliaNode, List<T>> findValueGRPC(KademliaNode node, byte[] key) {
        FindValueRequest request = FindValueRequest.newBuilder()
                .setNode(KademliaUtils.KademliaNodeToKademliaNodeProto(bucketManager.getMyNode()))
                .setKey(ByteString.copyFrom(key))
                .build();

        ManagedChannel channel = ManagedChannelBuilder.forTarget(node.getIpAddress() + ":" + node.getPort()).usePlaintext().build();
        AuctionBlockchainBlockingStub blockingStub = AuctionBlockchainGrpc.newBlockingStub(channel);

        FindValueResponse response;

        try {
            response = blockingStub.findValue(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "FIND_VALUE RPC failed for " + node.getIpAddress() + ":" + node.getPort() + " - " + e.getStatus());
            closeChannel(channel);
            return new Pair(null, new ArrayList<>());
        }


        KademliaNode nodeInResponse =  KademliaUtils.KademliaNodeProtoToKademliaNode(response.getNode());

        if (!Arrays.equals(node.getNodeID(), nodeInResponse.getNodeID())) {
            logger.log(Level.WARNING, "Response returned with different nodeID");
            closeChannel(channel);
            return new Pair(nodeInResponse, new ArrayList<>());
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

        closeChannel(channel);

        return new Pair(nodeInResponse, ret);
    }


    private static void closeChannel(ManagedChannel channel){
        try {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e){
            logger.log(Level.WARNING, "Exception when closing channel: " + e.getMessage());
        }
    }


    // nodeKey - nodes to search
    // key - key to search
    // This is done so that if we want mempool from certain nodes
    private <T> List<T> contactNNodes(byte[] nodeKey, byte[] key, TreeSet<KademliaNodeWrapper> shortList, TreeSet<KademliaNodeWrapper> probedNodes, int n, TwoParameterFunction<KademliaNode, byte[], Pair<KademliaNode, List<T>> > rpc) {
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

            Pair<KademliaNode, List<T>> retAux = rpc.apply(node, key);

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


    private <T> boolean store(byte[] serviceKey, T value) {
        KademliaNode myNode = bucketManager.getMyNode();

        TreeSet<KademliaNodeWrapper> shortList = new TreeSet<>(new KademliaNodeWrapperCompare());
        TreeSet<KademliaNodeWrapper> probedNodes = new TreeSet<>(new KademliaNodeWrapperCompare());

        List<KademliaNode> nodes = bucketManager.getClosestNodes(null, serviceKey, KademliaUtils.alpha);

        if(nodes.isEmpty()){
            logger.log(Level.WARNING, "No nodes found to send STORE RPC");
            return false;
        }

        for(KademliaNode node : nodes)
            shortList.add(new KademliaNodeWrapper(node, KademliaUtils.distanceTo(node.getNodeID(), serviceKey)));

        KademliaNode closestNode;
        boolean iterationMoreClose = true;

        // Stop iterating if no better node was found or we already probed more than k nodes
        while(iterationMoreClose && probedNodes.size() < KademliaUtils.k) {
            closestNode = shortList.first();

            contactNNodes(serviceKey, serviceKey, shortList, probedNodes, KademliaUtils.alpha, this::findNodeGRPC);


            // Stop iterating if no better node was found
            if(!shortList.isEmpty()){
                if(closestNode.equals(shortList.first())) {
                    logger.log(Level.INFO, "Could not find a better node.");
                    iterationMoreClose = false;

                    contactNNodes(serviceKey, serviceKey, shortList, probedNodes, KademliaUtils.k, this::findNodeGRPC);
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
            Pair<KademliaNode, Boolean> ret = storeGRPC(node, serviceKey, value);
            if (ret.getSecond().booleanValue()) {
                logger.log(Level.INFO, "Success in storeGRPC for node " + node);
                bucketManager.insertNode(ret.getFirst());
                success = true;
            }
        }

        return success;
    }

    private <T> List<T> findNode(byte[] key) {
        KademliaNode myNode = bucketManager.getMyNode();

        TreeSet<KademliaNodeWrapper> shortList = new TreeSet<>(new KademliaNodeWrapperCompare());
        TreeSet<KademliaNodeWrapper> probedNodes = new TreeSet<>(new KademliaNodeWrapperCompare());

        List<KademliaNode> nodes = bucketManager.getClosestNodes(null, key, KademliaUtils.alpha);

        if(nodes.isEmpty()){
            logger.log(Level.WARNING, "No nodes found to send FIND_NODE RPC");
            return new ArrayList<>();
        }

        for(KademliaNode node : nodes)
            shortList.add(new KademliaNodeWrapper(node, KademliaUtils.distanceTo(node.getNodeID(), key)));

        KademliaNode closestNode;
        boolean iterationMoreClose = true;

        // Stop iterating if no better node was found or we already probed more than k nodes
        while(iterationMoreClose && probedNodes.size() < KademliaUtils.k) {
            closestNode = shortList.first();

            contactNNodes(key, key, shortList, probedNodes, KademliaUtils.alpha, this::findNodeGRPC);


            // Stop iterating if no better node was found
            if(!shortList.isEmpty()){
                if(closestNode.equals(shortList.first())) {
                    logger.log(Level.INFO, "Could not find a better node.");
                    iterationMoreClose = false;

                    contactNNodes(key, key, shortList, probedNodes, KademliaUtils.k, this::findNodeGRPC);
                }
            }
            else {
                logger.log(Level.INFO, "Could not find a better node because the list was empty.");
                iterationMoreClose = false;
            }
        }

        return (List<T>) new ArrayList<>(probedNodes).subList(0, Math.min(probedNodes.size(), KademliaUtils.k));
    }

    private <T> List<T> findValue(byte[] nodeKey, byte[] key) {
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

        KademliaNode closestNode;
        boolean iterationMoreClose = true;


        // Stop iterating if no better node was found or we already probed more than k nodes
        while(iterationMoreClose && probedNodes.size() < KademliaUtils.k) {
            closestNode = shortList.first();

            List<T> retAux = contactNNodes(nodeKey, key, shortList, probedNodes, KademliaUtils.alpha, this::findValueGRPC);

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

                    retAux = contactNNodes(nodeKey, key, shortList, probedNodes, KademliaUtils.k, this::findValueGRPC);
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


    /***
     * ################################################
     * # Helper functions (internal helper functions) #
     * ################################################ */

    @FunctionalInterface
    private interface TwoParameterFunction<T, U, R> {
        R apply(T t, U u);
    }

    // https://dzone.com/articles/how-to-sort-a-map-by-value-in-java-8
    private static Map<KademliaNode, BigInteger> sortMapByValue(final Map<KademliaNode, BigInteger> nodes) {

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


