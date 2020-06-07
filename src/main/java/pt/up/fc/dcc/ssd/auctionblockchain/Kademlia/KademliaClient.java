package pt.up.fc.dcc.ssd.auctionblockchain.Kademlia;

import com.google.common.math.BigIntegerMath;
import jdk.jshell.execution.Util;
import pt.up.fc.dcc.ssd.auctionblockchain.*;
import pt.up.fc.dcc.ssd.auctionblockchain.Auction.Auction;
import pt.up.fc.dcc.ssd.auctionblockchain.AuctionBlockchainGrpc.AuctionBlockchainBlockingStub;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.Block;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.BlockchainUtils;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.Transaction;
import pt.up.fc.dcc.ssd.auctionblockchain.Client.Bid;

import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.math.BigInteger;
import java.security.SecureRandom;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

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
        logger.log(Level.INFO, "Bootstrapping kademlia network...");

        Pair<KademliaNode, Boolean> response = KademliaClient.ping(bucketManager.getMyNode(), bootstrappingNode);

        if(!response.getSecond().booleanValue()){
            logger.log(Level.SEVERE, "Bootstrap failed. Could not contact node " + bootstrappingNode + " for bootstrap.");
            return false;
        }

        bucketManager.insertNode(response.getFirst());

        List<KademliaNode> nodes = findNode(bucketManager.getMyNode().getNodeID());

        if(nodes.isEmpty()){
            logger.log(Level.SEVERE, "Bootstrap failed. Received an empty list while performing a node lookup for own node ID.");
            return false;
        }

        for(KademliaNode node : nodes){
            Pair<KademliaNode, Boolean> ret = KademliaClient.ping(bucketManager.getMyNode(), node);
            KademliaNode retNode = ret.getFirst();

            if (retNode != null) {
                bucketManager.insertNode(retNode);
//                logger.log(Level.INFO, "Inserted node " + retNode);
            }
            else
                logger.log(Level.INFO, "Could not PING node " + node);
        }

        byte[] distance = new byte[Utils.hashAlgorithmLengthInBytes];
        Random random = new SecureRandom();
        random.nextBytes(distance);

        Set<KademliaNode> contactedNodes = new HashSet<>();

        for(int i = 0; i < Utils.hashAlgorithmLengthInBytes; i++){

            for(int j = 1; j < 8; j++){
                BitSet bits = new BitSet(8);
                bits.set(0, 8);

                for(int k = 0; k < j; k++){
                    // Shift 1 zero into the start of the value
                    bits.clear(k);
                }

                bits.flip(0, 8);        // Flip the bits since they're in reverse order

                distance[i] = (byte)bits.toByteArray()[0];


                BigInteger distanceXORed = KademliaUtils.distanceTo(bucketManager.getMyNode().getNodeID(), distance);


                nodes = findNode(distanceXORed.toByteArray());

                if(nodes.isEmpty())
                    logger.log(Level.SEVERE, "Bootstrap failed for distance " + i + ". Received an empty list while performing a node lookup.");

                for(KademliaNode node : nodes){
                    if(contactedNodes.contains(node))
                        continue;

                    Pair<KademliaNode, Boolean> ret = KademliaClient.ping(bucketManager.getMyNode(), node);
                    KademliaNode retNode = ret.getFirst();

                    if (retNode != null) {
                        bucketManager.insertNode(retNode);
//                        logger.log(Level.INFO, "Inserted node " + retNode);
                    }
                    else {
                        logger.log(Level.WARNING, "Could not PING node " + node);
                    }

                    contactedNodes.add(node);
                }
            }
        }

        logger.log(Level.INFO, "Bootstrapping kademlia network done.");
        return true;
    }

    public <T> List<Transaction> getMempool(){
        logger.log(Level.INFO, "Getting mempool");

        byte[] mempoolKey = KademliaUtils.mempoolHash();

        if(mempoolKey == null)
            return new ArrayList<>();

        byte[] rand = new byte[Utils.hashAlgorithmLengthInBytes];

        Random random = new SecureRandom();
        random.nextBytes(rand);

        List<T> transactions = findValue(rand, mempoolKey);

        if(transactions.isEmpty())
            logger.log(Level.INFO, "Received empty mempool list. Returning...");

        List<Transaction> retList = new ArrayList<>();
        for(T tmp : transactions){
            if(tmp.getClass() == Transaction.class){
//                logger.log(Level.INFO, "Added a transaction to the list.");
                retList.add((Transaction) tmp);
            }
            else
                logger.log(Level.SEVERE, "Error: Received information different than a transaction.");
        }

        return retList;
    }

    public <T> List<Auction> getAuctions(){
        logger.log(Level.INFO, "Getting Auctions");

        byte[] auctionsKey = KademliaUtils.auctionsHash();

        if(auctionsKey == null)
            return new ArrayList<>();

        byte[] rand = new byte[Utils.hashAlgorithmLengthInBytes];

        Random random = new SecureRandom();
        random.nextBytes(rand);

        List<T> auctions = findValue(rand, auctionsKey);

        if(auctions.isEmpty())
            logger.log(Level.INFO, "Received empty auctions list. Returning...");

        List<Auction> retList = new ArrayList<>();
        for(T tmp : auctions){
            if(tmp.getClass() == Auction.class){
//                logger.log(Level.INFO, "Added a auction to the list.");
                retList.add((Auction) tmp);
            }
            else
                logger.log(Level.SEVERE, "Error: Received information different than a auction.");
        }

        return retList;
    }

    public <T> List<Bid> getBidsFromAuction(Auction auction){
        logger.log(Level.INFO, "Getting bids from auction " + auction.getHash());

        byte[] rand = new byte[Utils.hashAlgorithmLengthInBytes];

        Random random = new SecureRandom();
        random.nextBytes(rand);

        List<T> bids = findValue(rand, Utils.hexStringToBytes(auction.getItemID()));

        List<Bid> retList = new ArrayList<>();
        for(T tmp : bids){
            if(tmp.getClass() == Bid.class){
//                logger.log(Level.INFO, "Added a bid to the list.");
                retList.add((Bid) tmp);
            }
            else
                logger.log(Level.SEVERE, "Error: Received information different than a bid.");
        }

        return retList;
    }

    public <T> Block findBlockWithPreviousHash(String hash){
        logger.log(Level.INFO, "Trying to find block with previous hash " + hash);

        byte[] hashBytes = Utils.hexStringToBytes(hash);

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

    public void bootstrapBlockchain(){
        logger.log(Level.INFO, "Bootstrapping blockchain...");

        String lastBlockHash = BlockchainUtils.getLongestChain().getLastBlockHash();

        while(true){
            byte[] lastBlockHashBytes = Utils.hexStringToBytes(lastBlockHash);

            List<Block> blocks = findValue(lastBlockHashBytes, lastBlockHashBytes);

            if(blocks.isEmpty()) {
                logger.log(Level.INFO, "Received empty block list. Returning...");
                break;
            }

            Block block = null;
            for(int i=0; i<blocks.size(); i++) {
                Block blockAux = blocks.get(i);
                if (blockAux.getPreviousHash().equals(lastBlockHash)){
                    logger.log(Level.INFO, "Received block with hash " + blockAux.getHash());
                    block = blockAux;
                    break;
                }
            }

            if(block == null) {
                logger.log(Level.WARNING, "findValue returned wrong block.");
                break;
            }

            if(!BlockchainUtils.addBlock(block)){
                logger.log(Level.WARNING, "Could not add block to blockchain.");
                break;
            }

            lastBlockHash = BlockchainUtils.getLongestChain().getLastBlockHash();
        }

        logger.log(Level.INFO, "Bootstrapping blockchain done.");
    }

    public boolean announceNewBlock(Block block){
        byte[] rand = new byte[Utils.hashAlgorithmLengthInBytes];

        Random random = new SecureRandom();
        random.nextBytes(rand);

        if(store(rand, block)) {
            logger.log(Level.INFO, "Announced new block with hash " + block.getHash());
            return true;
        }
        else {
            logger.log(Level.INFO, "Could not announce new block with hash " + block.getHash());
            return false;
        }
    }

    public boolean announceNewTransaction(Transaction transaction){
        byte[] rand = new byte[Utils.hashAlgorithmLengthInBytes];

        Random random = new SecureRandom();
        random.nextBytes(rand);

        if(store(rand, transaction)){
            logger.log(Level.INFO, "Announced new transaction with hash " + transaction.getHash());
            return true;
        }
        else{
            logger.log(Level.INFO, "Could not announce new transaction with hash " + transaction.getHash());
            return false;
        }
    }

    public boolean announceNewBid(Bid bid){
        byte[] rand = new byte[Utils.hashAlgorithmLengthInBytes];

        Random random = new SecureRandom();
        random.nextBytes(rand);

        if(store(rand, bid)) {
            logger.log(Level.INFO, "Announced new bid with hash " + bid.getHash());
            return true;
        }
        else {
            logger.log(Level.INFO, "Could not announce new bid with hash " + bid.getHash());
            return false;
        }
    }

    public boolean announceNewAuction(Auction auction){
        byte[] rand = new byte[Utils.hashAlgorithmLengthInBytes];

        Random random = new SecureRandom();
        random.nextBytes(rand);

        if(store(rand, auction)) {
            logger.log(Level.INFO, "Announced new auction with hash " + auction.getHash());
            return true;
        }
        else {
            logger.log(Level.INFO, "Could not announce new auction with hash " + auction.getHash());
            return false;
        }
    }



    /***
     * ##########################################
     * # Private functions (internal functions) #
     * ########################################## */

    private static Pair<KademliaNode, Boolean> pingGRPC(KademliaNode myNode, KademliaNode node){
//        logger.log(Level.INFO, "Sending PING RPC to " + node);

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

        logger.log(Level.INFO, "Successfully sent PING RPC to " + node);
        return new Pair(nodeInResponse, true);
    }

    private <T> Pair<KademliaNode, Boolean> storeGRPC(KademliaNode node, byte[] key, T info) {
//        logger.log(Level.INFO, "Sending STORE RPC to " + node + " for key " + Utils.bytesToHexString(key));

        StoreRequest.Builder request = StoreRequest.newBuilder()
                .setNode(KademliaUtils.KademliaNodeToKademliaNodeProto(bucketManager.getMyNode()))
                .setKey(ByteString.copyFrom(key));


        if(info.getClass() == Transaction.class) {
            request.setTransaction(KademliaUtils.TransactionToTransactionProto((Transaction) info));
        }
        else if(info.getClass() == Block.class) {
            request.setBlock(KademliaUtils.BlockToBlockProto((Block) info));
        }
        else if(info.getClass() == Auction.class) {
            request.setAuction(KademliaUtils.AuctionToAuctionProto((Auction) info));
        }
        else if(info.getClass() == Bid.class) {
            request.setBid(KademliaUtils.BidToBidProto((Bid) info));
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

        logger.log(Level.INFO, "Successfully sent STORE RPC to " + node + " for key " + Utils.bytesToHexString(key));
        return new Pair(nodeInResponse, response.getSuccess());
    }

    private <T> Pair<KademliaNode, List<T>> findNodeGRPC(KademliaNode node, byte[] requestedID) {
//        logger.log(Level.INFO, "Sending FIND_NODE RPC to " + node + " for key " + Utils.bytesToHexString(requestedID));

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

        logger.log(Level.INFO, "Successfully sent FIND_NODE RPC to " + node + " for key " + Utils.bytesToHexString(requestedID));
        return new Pair(nodeInResponse, (List<T>)KademliaUtils.KBucketProtoToKademliaNodeList(response.getBucket()));
    }

    private <T> Pair<KademliaNode, List<T>> findValueGRPC(KademliaNode node, byte[] key) {
//        logger.log(Level.INFO, "Sending FIND_VALUE RPC to " + node + " for key " + Utils.bytesToHexString(key));

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
        if (response.getBlockOrTransactionsOrNodesOrAuctionsOrBidsCase() == FindValueResponse.BlockOrTransactionsOrNodesOrAuctionsOrBidsCase.BLOCK) {
            ret.add((T)KademliaUtils.BlockProtoToBlock(response.getBlock()));
        }
        else if (response.getBlockOrTransactionsOrNodesOrAuctionsOrBidsCase() == FindValueResponse.BlockOrTransactionsOrNodesOrAuctionsOrBidsCase.TRANSACTIONS) {
            ret.addAll((List<T>)KademliaUtils.MempoolProtoToTransactionList(response.getTransactions()));
        }
        else if (response.getBlockOrTransactionsOrNodesOrAuctionsOrBidsCase() == FindValueResponse.BlockOrTransactionsOrNodesOrAuctionsOrBidsCase.BUCKET) {
            ret.addAll((List<T>)KademliaUtils.KBucketProtoToKademliaNodeList(response.getBucket()));
        }
        else if (response.getBlockOrTransactionsOrNodesOrAuctionsOrBidsCase() == FindValueResponse.BlockOrTransactionsOrNodesOrAuctionsOrBidsCase.AUCTIONS) {
            ret.addAll((List<T>)KademliaUtils.AuctionListProtoToAuctionList(response.getAuctions()));
        }
        else if (response.getBlockOrTransactionsOrNodesOrAuctionsOrBidsCase() == FindValueResponse.BlockOrTransactionsOrNodesOrAuctionsOrBidsCase.BIDS) {
            ret.addAll((List<T>)KademliaUtils.BidListProtoToBidList(response.getBids()));
        }
        else
            logger.log(Level.SEVERE, "Error: findValueResponse without BlockOrTransactionsOrNodesOrAuctionsOrBidsCase");

        closeChannel(channel);

        logger.log(Level.INFO, "Successfully sent FIND_VALUE RPC to " + node + " for key " + Utils.bytesToHexString(key));
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

//        logger.log(Level.INFO, "tempAlphaList with " + tempAlphaList.size() + " nodes");

        // Variable to store the returned objects in case the RPC is Find_Value
        List<T> ret = new ArrayList<>();

        // Query selected alpha nodes
        for(KademliaNodeWrapper node : tempAlphaList) {
            boolean received = false;

            Pair<KademliaNode, List<T>> retAux = rpc.apply(node, key);

            if(retAux.getFirst() == null) {
//                logger.log(Level.INFO, "Node " + node + " didn't respond. Removing from shortlist.");
                shortList.remove(node);
            }
            else {
//                logger.log(Level.INFO, "Node " + node + " returned some things.");
                bucketManager.insertNode(retAux.getFirst());
                probedNodes.add(node);
            }


            // Set node timestamp as the distance
            for(T aux : retAux.getSecond()) {
                if (aux.getClass() == KademliaNode.class) {
                    KademliaNode aux2 = (KademliaNode) aux;
//                    bucketManager.insertNode(aux2);

                    KademliaNodeWrapper aux3 = new KademliaNodeWrapper(aux2, KademliaUtils.distanceTo(aux2.getNodeID(), nodeKey));
                    shortList.add(aux3);

//                    logger.log(Level.INFO, "Node " + aux2 + " added.");
                }
                else {
//                    logger.log(Level.INFO, "Added some information.");
                    ret.add(aux);
                    received = true;
                }
            }

            if(received)
                return ret;
        }

        return ret;
    }


    private <T> boolean store(byte[] serviceKey, T value) {
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
//                    logger.log(Level.INFO, "Could not find a better node.");
                    iterationMoreClose = false;

                    contactNNodes(serviceKey, serviceKey, shortList, probedNodes, KademliaUtils.k, this::findNodeGRPC);
                }
            }
            else {
//                logger.log(Level.INFO, "Could not find a better node because the list was empty.");
                iterationMoreClose = false;
            }
        }


        List<KademliaNodeWrapper> closestNodes = new ArrayList<>(probedNodes).subList(0, Math.min(probedNodes.size(), KademliaUtils.k));
        probedNodes.clear();
        probedNodes.addAll(closestNodes);

        boolean success = false;
        for(KademliaNode node : probedNodes) {
            Pair<KademliaNode, Boolean> ret = storeGRPC(node, serviceKey, value);

            if(ret.getSecond().booleanValue()) {
//                logger.log(Level.INFO, "Success in storeGRPC for node " + node);
                success = true;
            }

            if(ret.getFirst() != null)
                bucketManager.insertNode(ret.getFirst());
        }

        return success;
    }

    private <T> List<T> findNode(byte[] key) {
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
//                    logger.log(Level.INFO, "Could not find a better node.");
                    iterationMoreClose = false;

                    contactNNodes(key, key, shortList, probedNodes, KademliaUtils.k, this::findNodeGRPC);
                }
            }
            else {
//                logger.log(Level.INFO, "Could not find a better node because the list was empty.");
                iterationMoreClose = false;
            }
        }

        return (List<T>) new ArrayList<>(probedNodes).subList(0, Math.min(probedNodes.size(), KademliaUtils.k));
    }

    private <T> List<T> findValue(byte[] nodeKey, byte[] key) {
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
//                logger.log(Level.INFO, "Received data.");
                return retAux;
            }

            // Stop iterating if no better node was found
            if(!shortList.isEmpty()) {
                if (closestNode.equals(shortList.first())) {
//                    logger.log(Level.INFO, "Could not find a better node.");
                    iterationMoreClose = false;

                    retAux = contactNNodes(nodeKey, key, shortList, probedNodes, KademliaUtils.k, this::findValueGRPC);
                    if (!retAux.isEmpty()) {
//                        logger.log(Level.INFO, "Received data.");
                        return retAux;
                    }
                }
            }
            else {
//                logger.log(Level.INFO, "Could not find a better node because the list was empty.");
                iterationMoreClose = false;
            }
        }

        logger.log(Level.INFO, "Returning an empty list because the end of the function was reached.");
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


