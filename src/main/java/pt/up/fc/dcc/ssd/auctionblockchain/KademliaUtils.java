package pt.up.fc.dcc.ssd.auctionblockchain;

import com.google.protobuf.ByteString;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KademliaUtils {
    public static final Charset charset = Charset.forName("ASCII");
    public static final int alpha = 3;
    public static final int idSizeInBits = 160;
    public static final int idSizeInBytes = idSizeInBits/8;
    public static final int k = 20;
    public static final String hashAlgorithm = "SHA-1";



    public static int distanceTo(KademliaNode node1, KademliaNode node2){
        return distanceTo(node1.getNodeID(), node2.getNodeID());
    }

    public static int distanceTo(byte[] nodeID1, byte[] nodeID2){
        byte[] distance = new byte[KademliaUtils.idSizeInBytes];

        for (int i = 0; i < KademliaUtils.idSizeInBytes; i++) {
            distance[i] = (byte) ((int) nodeID1[i] ^ (int) nodeID2[i]);
            if (distance[i] < 0)
                distance[i] += 256; // Two's complement stuff
        }

        return new BigInteger(distance).intValue();
    }

    public static double log2(double d) {
        return Math.log(d)/Math.log(2.0);
    }


    public static BlockProto BlockToBlockProto(Block block){
        BlockHeaderProto header = BlockHeaderProto.newBuilder()
                .setPrevBlock(ByteString.copyFrom(block.getPreviousHash(), charset))
                .setMerkleRoot(ByteString.copyFrom(block.getHash(), charset))
                .setTimestamp(block.getTimeStamp())
                .setDifficulty(block.getDifficulty())
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

    public static List<Transaction> MempoolProtoToTransactionList(MempoolProto mempool) {
        List<Transaction> ret = new ArrayList<>();

        for(TransactionProto trans : mempool.getTransactionsList())
            ret.add(TransactionProtoToTransaction(trans));

        return ret;
    }

    public static MempoolProto TransactionListToMempoolProto(List<Transaction> transactions) {
        MempoolProto.Builder builder = MempoolProto.newBuilder();

        for(Transaction trans : transactions)
            builder.addTransactions(TransactionToTransactionProto(trans));

        return builder.build();
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

    public static KBucketProto KademliaNodeListToKBucketProto(List<KademliaNode> nodeList){
        KBucketProto.Builder builder = KBucketProto.newBuilder();

        for(KademliaNode node : nodeList)
            builder.addNodes(KademliaNodeToKademliaNodeProto(node));

        return builder.build();
    }

    public static List<KademliaNode> KBucketProtoToKademliaNodeList(KBucketProto kBucketProto){
        int size = kBucketProto.getNodesCount();

        List<KademliaNode> nodes = new ArrayList<>();

        for (KademliaNodeProto node : kBucketProto.getNodesList())
            nodes.add(KademliaNodeProtoToKademliaNode(node));

        return nodes;
    }
}
