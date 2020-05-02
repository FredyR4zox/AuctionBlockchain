package pt.up.fc.dcc.ssd.auctionblockchain;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
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

    public static boolean ping(KademliaNode myNode, KademliaNode node){
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

    public static <T> boolean store(KademliaNode myNode, KademliaNode node, T info) {
        StoreRequest.Builder request = StoreRequest.newBuilder().setNodeID(KademliaUtils.NodeIDToNodeIDProto(myNode.getNodeID()));

        if(info.getClass() == Transaction.class) {
            request.setTransaction(KademliaUtils.TransactionToTransactionProto((Transaction) info));
            request.setKey(ByteString.copyFrom(((Transaction)info).getHash(), KademliaUtils.charset));
        }
        else if(info.getClass() == Block.class) {
            request.setBlock(KademliaUtils.BlockToBlockProto((Block) info));
            request.setKey(ByteString.copyFrom(((Block)info).getHash(), KademliaUtils.charset));
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

    public static List<KademliaNode> findNode(KademliaNode myNode, KademliaNode node, byte[] requestedID) {
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

        return KademliaUtils.KBucketProtoToKademliaNodeList(response.getBucket());
    }

    public static <T> List<T> findValue(KademliaNode myNode, KademliaNode node, byte[] key) {
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
}
