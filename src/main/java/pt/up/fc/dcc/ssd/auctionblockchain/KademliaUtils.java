package pt.up.fc.dcc.ssd.auctionblockchain;

import com.google.protobuf.ByteString;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KademliaUtils {
    public static final Charset charset = Charset.forName("ASCII");
    public static final int idSizeInBits = 160;
    public static final int idSizeInBytes = idSizeInBits/8;
    public static final int k = 20;


    public static BlockProto BlockToBlockProto(Block block){
        BlockHeaderProto header = BlockHeaderProto.newBuilder()
                .setPrevBlock(ByteString.copyFrom(block.getPreviousHash(), charset))
                .setMerkleRoot(ByteString.copyFrom(block.getHash(), charset))
                .setTimestamp(block.getTimeStamp())
                .setDifficulty(1234)
                .setNonce((int)block.getNonce())
                .build();

        BlockProto.Builder builder = BlockProto.newBuilder().setBlockHeader(header);

        int size = block.getNrTransactions();
        Transaction[] transactions = block.getData();

        for(int i = 0; i < size; i++)
            builder.addTransactions(TransactionToTransactionProto(transactions[i]));

        return builder.build();
    }

    public static Block BlockProtoToBlock(BlockProto blockProto){
        BlockHeaderProto blockHeader = blockProto.getBlockHeader();

        int size = blockProto.getTransactionsCount();
        Transaction[] transactions = new Transaction[size];

        for (int i = 0; i < size; i++)
            transactions[i] = TransactionProtoToTransaction(blockProto.getTransactions(i));

        Block block = new Block(
                blockHeader.getMerkleRoot().toString(charset),
                blockHeader.getPrevBlock().toString(charset),
                TransactionProtoToTransaction(blockProto.getReward()),
                transactions,
                blockHeader.getDifficulty(),
                blockHeader.getTimestamp(),
                blockHeader.getNonce());

        return block;
    }

    public static TransactionProto TransactionToTransactionProto(Transaction transaction){
        return TransactionProto.newBuilder()
                .setSellerID(ByteString.copyFrom(transaction.getSellerID(), charset))
                .setBuyerID(ByteString.copyFrom(transaction.getBuyerID(), charset))
                .setAmount(transaction.getAmount())
                .setFee(transaction.getTransactionFee())
                .setItemID(transaction.getItemID())
                .setBuyerPublicKey(ByteString.copyFrom(transaction.getBuyerPublicKey().toString(), charset))
                .setSignature(ByteString.copyFrom(transaction.getSignature()))
                .setHash(ByteString.copyFrom(transaction.getHash(), charset))
                .build();
    }

    public static Transaction TransactionProtoToTransaction(TransactionProto transactionProto){
        return new Transaction(
                transactionProto.getBuyerID().toString(charset),
                transactionProto.getSellerID().toString(charset),
                transactionProto.getBuyerPublicKey().toByteArray(),
                transactionProto.getAmount(),
                transactionProto.getFee(),
                transactionProto.getItemID(),
                transactionProto.getSignature().toByteArray());
    }

    public static NodeIDProto NodeIDToNodeIDProto(byte[] nodeID){
        return NodeIDProto.newBuilder()
                .setId(ByteString.copyFrom(nodeID))
                .build();
    }

    public static byte[] NodeIDProtoToNodeID(NodeIDProto nodeIDProto){
        return nodeIDProto.getId().toByteArray();
    }

    public static KademliaNodeProto KademliaNodeToKademliaNodeProto(KademliaNode kademliaNode){
        return KademliaNodeProto.newBuilder()
                .setIpAddress(kademliaNode.getIpAddress())
                .setPort(kademliaNode.getPort())
                .setNodeID(NodeIDToNodeIDProto(kademliaNode.getNodeID()))
                .build();
    }

    public static KademliaNode KademliaNodeProtoToKademliaNode(KademliaNodeProto kademliaNodeProto){
        return new KademliaNode(
                kademliaNodeProto.getIpAddress(),
                kademliaNodeProto.getPort(),
                NodeIDProtoToNodeID(kademliaNodeProto.getNodeID()));
    }

    public static KBucketProto KBucketToKBucketProto(KBucket kBucket){
        KBucketProto.Builder builder = KBucketProto.newBuilder();

        List<KademliaNode> nodes = kBucket.getNodes();

        for(int i = 0; i < nodes.size(); i++)
            builder.addNodes(KademliaNodeToKademliaNodeProto(nodes.get(i)));

        return builder.build();
    }

    public static KBucket KBucketProtoToKBucket(KBucketProto kBucketProto){
        int size = kBucketProto.getNodesCount();

        KademliaNode[] nodes = new KademliaNode[size];

        for(int i = 0; i < size; i++)
            nodes[i] = KademliaNodeProtoToKademliaNode(kBucketProto.getNodes(i));

        return new KBucket(nodes);
    }
}
