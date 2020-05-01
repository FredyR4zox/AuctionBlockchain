package pt.up.fc.dcc.ssd.auctionblockchain;

import java.util.logging.Logger;
import pt.up.fc.dcc.ssd.auctionblockchain.AuctionBlockchainGrpc.AuctionBlockchainBlockingStub;
import pt.up.fc.dcc.ssd.auctionblockchain.AuctionBlockchainGrpc.AuctionBlockchainStub;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class KademliaClient {
    private static final Logger logger = Logger.getLogger(KademliaClient.class.getName());

    private final AuctionBlockchainBlockingStub blockingStub;
    private final AuctionBlockchainStub asyncStub;

    public KademliaClient(String ipAddress, int port){
        ManagedChannel channel = ManagedChannelBuilder.forTarget(ipAddress + ":" + port).usePlaintext().build();

        blockingStub = AuctionBlockchainGrpc.newBlockingStub(channel);
        asyncStub = AuctionBlockchainGrpc.newStub(channel);
    }

    public static boolean ping(KademliaNode node){
        ManagedChannel channel = ManagedChannelBuilder.forTarget(node.getIpAddress() + ":" + node.getPort()).usePlaintext().build();
        AuctionBlockchainBlockingStub blockingStub = AuctionBlockchainGrpc.newBlockingStub(channel);

        try {
            NodeIDProto id = blockingStub.ping(NodeIDProto.newBuilder().setId(ByteString.copyFrom(node.getNodeID())).build());
            if (id.getId().equals(ByteString.copyFrom(node.getNodeID())))
                return true;
        } catch (StatusRuntimeException e) {
            logger.warning("RPC failed: " + e.getStatus());
        }

        return false;
    }

    public static boolean store(KademliaNode node, BlockProto block) {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(node.getIpAddress() + ":" + String.valueOf(node.getPort())).usePlaintext().build();
        AuctionBlockchainBlockingStub blockingStub = AuctionBlockchainGrpc.newBlockingStub(channel);

        String key = "ABCD";
        try {
            StoreResponse response = blockingStub.store(StoreRequest.newBuilder()
                    .setNodeID(NodeIDProto.newBuilder().setId(ByteString.copyFrom(node.getNodeID())).build())
                    .setKey(key)
                    .setBlock(block)
                    .build());
            return response.getSuccess();
        } catch (StatusRuntimeException e) {
            logger.warning("RPC failed: " + e.getStatus());
        }

        return false;
    }
}
