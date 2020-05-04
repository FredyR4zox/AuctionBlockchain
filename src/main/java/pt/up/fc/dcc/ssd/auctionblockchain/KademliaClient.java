package pt.up.fc.dcc.ssd.auctionblockchain;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.logging.Logger;

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
            logger.warning("PING RPC failed for " + node.getIpAddress() + ":" + node.getPort() + " - " + e.getStatus());
            return false;
        }

        if (!myNode.getNodeID().equals(id.getId().toByteArray()))
            return false;

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
            logger.warning("Error: store value type not valid - " + info.getClass());
            return false;
        }

        ManagedChannel channel = ManagedChannelBuilder.forTarget(node.getIpAddress() + ":" + node.getPort()).usePlaintext().build();
        AuctionBlockchainBlockingStub blockingStub = AuctionBlockchainGrpc.newBlockingStub(channel);

        StoreResponse response;

        try {
            response = blockingStub.store(request .build());
        } catch (StatusRuntimeException e) {
            logger.warning("STORE RPC failed for " + node.getIpAddress() + ":" + node.getPort() + " - " + e.getStatus());
            return false;
        }

        if (!myNode.getNodeID().equals(response.getNodeID().getId().toByteArray()))
            return false;

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
            logger.warning("FIND_NODE RPC failed for " + node.getIpAddress() + ":" + node.getPort() + " - " + e.getStatus());
            return new ArrayList<>();
        }

        if (!myNode.getNodeID().equals(response.getNodeID().getId().toByteArray()))
            return new ArrayList<>();

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
            logger.warning("FIND_VALUE RPC failed for " + node.getIpAddress() + ":" + node.getPort() + " - " + e.getStatus());
            return new ArrayList<>();
        }

        if (!myNode.getNodeID().equals(response.getNodeID().getId().toByteArray()))
            return new ArrayList<>();

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
            logger.warning("Error: findValueResponse without BlockOrTransactionsOrNodesCase");

        return ret;
    }


    // nodeKey - nodes to search
    // key - key to search
    // This is done so that if we want mempool from certain nodes
    private static <T> List<T> contactNNodes(KademliaNode myNode, byte[] nodeKey, byte[] key, TreeSet<KademliaNode> shortList, TreeSet<KademliaNode> probedNodes, int n, ThreeParameterFunction<KademliaNode, KademliaNode, byte[], List<T> > rpc) {
        // Select alpha nodes to query
        List<KademliaNode> tempAlphaList = new ArrayList<>();
        Iterator it = shortList.iterator();
        while (tempAlphaList.size() < KademliaUtils.alpha && it.hasNext()) {
            KademliaNode node = (KademliaNode) it.next();

            if (probedNodes.contains(node))
                continue;

            tempAlphaList.add(node);
        }

        // Variable to store the returned objects in case the RPC is Find_Value
        List<T> ret = new ArrayList<>();

        // Query selected alpha nodes
        for(KademliaNode node : tempAlphaList) {

            List<T> retAux = rpc.apply(myNode, node, key);

            if(retAux.isEmpty())
                shortList.remove(node);
            else
                probedNodes.add(node);

            // Set node timestamp as the distance
            for(T aux : retAux) {
                if (aux.getClass() == KademliaNode.class) {
                    KademliaNode aux2 = (KademliaNode) aux;
                    aux2.updateLastSeen(KademliaUtils.distanceTo(aux2.getNodeID(), nodeKey));
                    shortList.add(aux2);
                }
                else
                    ret.add(aux);
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

        TreeSet<KademliaNode> shortList = new TreeSet<>(new KademliaUtils.KademliaNodeCompare());
        TreeSet<KademliaNode> probedNodes = new TreeSet<>(new KademliaUtils.KademliaNodeCompare());

        //Hack: Use the comparator that compares timestamps to compare distances by putting the distance as the timestamp
        List<KademliaNode> hackedNodes = bucketManager.getClosestNodes(null, serviceKey, KademliaUtils.alpha);
        for(KademliaNode node : hackedNodes)
            node.updateLastSeen(KademliaUtils.distanceTo(node.getNodeID(), serviceKey));

        shortList.addAll(hackedNodes);

        KademliaNode closestNode;
        boolean iterationMoreClose = true;

        // Stop iterating if no better node was found or we already probed more than k nodes
        while(iterationMoreClose && probedNodes.size() < KademliaUtils.k) {
            closestNode = shortList.first();

            contactNNodes(myNode, serviceKey, serviceKey, shortList, probedNodes, KademliaUtils.alpha, KademliaClient::findNodeGRPC);


            // Stop iterating if no better node was found
            if(closestNode.equals(shortList.first())) {
                iterationMoreClose = false;

                contactNNodes(myNode, serviceKey, serviceKey, shortList, probedNodes, KademliaUtils.k, KademliaClient::findNodeGRPC);
            }
        }



        List<KademliaNode> closestNodes = new ArrayList<>(probedNodes).subList(0, Math.min(probedNodes.size(), KademliaUtils.k));
        probedNodes.clear();
        probedNodes.addAll(closestNodes);


        for(KademliaNode node : probedNodes) {
            if (storeGRPC(myNode, node, serviceKey, value)) {
                return true;
            }
        }

        return false;
    }

    public static <T> List<T> findNode(KBucketManager bucketManager, byte[] key) {
        KademliaNode myNode = bucketManager.getMyNode();

        TreeSet<KademliaNode> shortList = new TreeSet<>(new KademliaUtils.KademliaNodeCompare());
        TreeSet<KademliaNode> probedNodes = new TreeSet<>(new KademliaUtils.KademliaNodeCompare());

        //Hack: Use the comparator that compares timestamps to compare distances by putting the distance as the timestamp
        List<KademliaNode> hackedNodes = bucketManager.getClosestNodes(null, key, KademliaUtils.alpha);
        for(KademliaNode node : hackedNodes)
            node.updateLastSeen(KademliaUtils.distanceTo(node.getNodeID(), key));

        shortList.addAll(hackedNodes);

        KademliaNode closestNode;
        boolean iterationMoreClose = true;

        // Stop iterating if no better node was found or we already probed more than k nodes
        while(iterationMoreClose && probedNodes.size() < KademliaUtils.k) {
            closestNode = shortList.first();

            contactNNodes(myNode, key, key, shortList, probedNodes, KademliaUtils.alpha, KademliaClient::findValueGRPC);


            // Stop iterating if no better node was found
            if(closestNode.equals(shortList.first())) {
                iterationMoreClose = false;

                contactNNodes(myNode, key, key, shortList, probedNodes, KademliaUtils.k, KademliaClient::findValueGRPC);
            }
        }

        return (List<T>) new ArrayList<>(probedNodes).subList(0, Math.min(probedNodes.size(), KademliaUtils.k));
    }

    public static <T> List<T> findValue(KBucketManager bucketManager, byte[] nodeKey, byte[] key) {
        KademliaNode myNode = bucketManager.getMyNode();

        TreeSet<KademliaNode> shortList = new TreeSet<>(new KademliaUtils.KademliaNodeCompare());
        TreeSet<KademliaNode> probedNodes = new TreeSet<>(new KademliaUtils.KademliaNodeCompare());

        // Hack: Use the comparator that compares timestamps to compare distances by putting the distance as the timestamp
        List<KademliaNode> hackedNodes;
        hackedNodes = bucketManager.getClosestNodes(null, nodeKey, KademliaUtils.alpha);

        for(KademliaNode node : hackedNodes)
            node.updateLastSeen(KademliaUtils.distanceTo(node.getNodeID(), nodeKey));

        shortList.addAll(hackedNodes);

        KademliaNode closestNode;
        boolean iterationMoreClose = true;


        // Stop iterating if no better node was found or we already probed more than k nodes
        while(iterationMoreClose && probedNodes.size() < KademliaUtils.k) {
            closestNode = shortList.first();

            List<T> retAux = contactNNodes(myNode, nodeKey, key, shortList, probedNodes, KademliaUtils.alpha, KademliaClient::findValueGRPC);

            // If we received anything from the nodes
            if(!retAux.isEmpty()) {
                return retAux;
            }

            // Stop iterating if no better node was found
            if(closestNode.equals(shortList.first())) {
                iterationMoreClose = false;

                retAux = contactNNodes(myNode, nodeKey, key, shortList, probedNodes, KademliaUtils.k, KademliaClient::findValueGRPC);
                if(!retAux.isEmpty()) {
                    return retAux;
                }
            }
        }

        return new ArrayList<>();
    }
}


