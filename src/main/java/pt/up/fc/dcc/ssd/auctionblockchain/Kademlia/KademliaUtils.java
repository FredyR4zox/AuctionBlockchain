package pt.up.fc.dcc.ssd.auctionblockchain.Kademlia;

import com.google.protobuf.ByteString;
import pt.up.fc.dcc.ssd.auctionblockchain.*;
import pt.up.fc.dcc.ssd.auctionblockchain.Auction.Auction;
import pt.up.fc.dcc.ssd.auctionblockchain.Client.Bid;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.Block;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.Transaction;

import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

public class KademliaUtils {
//    private static final Logger logger = Logger.getLogger(KademliaUtils.class.getName());

    public static final int alpha = 3;
    public static final int k = 20;
    public static final int networkTimeoutMsecs = 5*1000;
    public static final int badNodeTimeoutSecs = 120;
    public static final String mempoolText = "mempool";
    public static final String auctionsText = "auctions";

    public static final byte[] bootstrapNodeID = new byte[Utils.hashAlgorithmLengthInBytes];
    public static final String bootstrapNodeIP = "34.105.188.87";
    public static final int bootstrapNodePort = 1337;

    public static final String defaultIpAddress = "0.0.0.0";
    public static final int defaultPort = 1337;



    public static BigInteger distanceTo(KademliaNode node1, KademliaNode node2){
        return distanceTo(node1.getNodeID(), node2.getNodeID());
    }

    public static BigInteger distanceTo(byte[] nodeID1, byte[] nodeID2){
        BigInteger distance = new BigInteger(nodeID1);
        distance = distance.xor(new BigInteger(nodeID2));
        distance = distance.abs();

        if(distance.equals(BigInteger.ZERO))
            return BigInteger.ONE;

        return distance;
    }

    public static String getMyIpAddress(){
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://ifconfig.me/all.json"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            JSONObject obj = new JSONObject(response.body());
            return obj.getString("ip_addr");
        }
        catch (IOException e){
            return null;
        }
        catch (InterruptedException e){
            return null;
        }
    }


    static class KademliaNodeCompare implements Comparator<KademliaNode>
    {
        public int compare(KademliaNode k1, KademliaNode k2)
        {
            if (k1.getLastSeen() < k2.getLastSeen()) return -1;
            if (k1.getLastSeen() > k2.getLastSeen()) return 1;
            else return 0;
        }
    }

    public static byte[] mempoolHash() {
        return Utils.hexStringToBytes(Utils.getHash(mempoolText));
    }

    public static byte[] auctionsHash() {
        return Utils.hexStringToBytes(Utils.getHash(auctionsText));
    }


    public static BlockProto BlockToBlockProto(Block block){
        BlockHeaderProto header = BlockHeaderProto.newBuilder()
                .setPrevBlock(ByteString.copyFrom(Utils.hexStringToBytes(block.getPreviousHash())))
                .setMerkleRoot(ByteString.copyFrom(Utils.hexStringToBytes(block.getHash())))
                .setTimestamp(block.getTimeStamp())
                .setDifficulty(block.getDifficulty())
                .setNonce((int)block.getNonce())
                .setPreviousWork(ByteString.copyFrom(block.getPreviousWork().toByteArray()))
                .build();

        BlockProto.Builder builder = BlockProto.newBuilder()
                .setBlockHeader(header)
                .setReward(MinersRewardToTransactionProto(block.getMinersReward()));

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
                Utils.bytesToHexString(blockHeader.getMerkleRoot().toByteArray()),
                Utils.bytesToHexString(blockHeader.getPrevBlock().toByteArray()),
                TransactionProtoToMinersReward(blockProto.getReward()),
                transactions,
                blockHeader.getDifficulty(),
                blockHeader.getTimestamp(),
                blockHeader.getNonce(),
                new BigInteger(blockHeader.getPreviousWork().toByteArray()));

        return block;
    }

    public static AuctionProto AuctionToAuctionProto(Auction auction){
        return AuctionProto.newBuilder()
                .setItemId(ByteString.copyFrom(Utils.hexStringToBytes(auction.getItemID())))
                .setSellerId(ByteString.copyFrom(Utils.hexStringToBytes(auction.getSellerID())))
                .setMinAmount(auction.getMinAmount())
                .setMinIncrement(auction.getMinIncrement())
                .setFee(auction.getFee())
                .setTimeout(auction.getTimeout())
                .setSellerPublicKey(ByteString.copyFrom(auction.getSellerPublicKey().getEncoded()))
                .setHash(ByteString.copyFrom(Utils.hexStringToBytes(auction.getHash())))
                .setSignature(ByteString.copyFrom(auction.getSignature()))
                .build();
    }

    public static Auction AuctionProtoToAuction(AuctionProto auctionProto){
        return new Auction(
                Utils.bytesToHexString(auctionProto.getItemId().toByteArray()),
                Utils.bytesToHexString(auctionProto.getItemId().toByteArray()),
                auctionProto.getMinAmount(),
                auctionProto.getMinIncrement(),
                auctionProto.getFee(),
                auctionProto.getTimeout(),
                Wallet.getPublicKeyFromBytes(auctionProto.getSellerPublicKey().toByteArray()),
                Utils.bytesToHexString(auctionProto.getHash().toByteArray()),
                auctionProto.getSignature().toByteArray());
    }

    public static List<Auction> AuctionListProtoToAuctionList(AuctionListProto auctionListProto){
        List<Auction> ret = new ArrayList<>();

        for(AuctionProto auction : auctionListProto.getAuctionsList())
            ret.add(AuctionProtoToAuction(auction));

        return ret;
    }

    public static AuctionListProto AuctionListToAuctionListProto(List<Auction> auctions){
        AuctionListProto.Builder builder = AuctionListProto.newBuilder();

        for(Auction auction : auctions)
            builder.addAuctions(AuctionToAuctionProto(auction));

        return builder.build();
    }

    public static BidProto BidToBidProto(Bid bid){
        return BidProto.newBuilder()
                .setItemId(ByteString.copyFrom(Utils.hexStringToBytes(bid.getItemId())))
                .setSellerID(ByteString.copyFrom(Utils.hexStringToBytes(bid.getSellerID())))
                .setBuyerID(ByteString.copyFrom(Utils.hexStringToBytes(bid.getBuyerID())))
                .setAmount(bid.getAmount())
                .setFee(bid.getFee())
                .setBuyerPublicKey(ByteString.copyFrom(bid.getBuyerPublicKey().getEncoded()))
                .setHash(ByteString.copyFrom(Utils.hexStringToBytes(bid.getHash())))
                .setSignature(ByteString.copyFrom(bid.getSignature()))
                .build();
    }

    public static Bid BidProtoToBid(BidProto bidProto){
        return new Bid(
                Utils.bytesToHexString(bidProto.getItemId().toByteArray()),
                Utils.bytesToHexString(bidProto.getSellerID().toByteArray()),
                Utils.bytesToHexString(bidProto.getBuyerID().toByteArray()),
                bidProto.getAmount(),
                bidProto.getFee(),
                bidProto.getBuyerPublicKey().toByteArray(),
                Utils.bytesToHexString(bidProto.getHash().toByteArray()),
                bidProto.getSignature().toByteArray());
    }

    public static BidProto BidMinersRewardToBidProto(Bid bid){
        return BidProto.newBuilder()
                .setSellerID(ByteString.copyFrom(Utils.hexStringToBytes(bid.getSellerID())))
                .setAmount(bid.getAmount())
                .setHash(ByteString.copyFrom(Utils.hexStringToBytes(bid.getHash())))
                .build();
    }

    public static Bid BidProtoToBidMinersReward(BidProto bidProto){
        return new Bid(
                Utils.bytesToHexString(bidProto.getSellerID().toByteArray()),
                bidProto.getAmount(),
                Utils.bytesToHexString(bidProto.getHash().toByteArray()));
    }

    public static List<Bid> BidListProtoToBidList(BidListProto bidListProto){
        List<Bid> ret = new ArrayList<>();

        for(BidProto bid : bidListProto.getBidsList())
            ret.add(BidProtoToBid(bid));

        return ret;
    }

    public static BidListProto BidListToBidListProto(List<Bid> bids){
        BidListProto.Builder builder = BidListProto.newBuilder();

        for(Bid bid : bids)
            builder.addBids(BidToBidProto(bid));

        return builder.build();
    }

    public static TransactionProto TransactionToTransactionProto(Transaction transaction){
        return TransactionProto.newBuilder()
                .setBid(BidToBidProto(transaction.getBid()))
                .setSellerPublicKey(ByteString.copyFrom(transaction.getSellerPublicKey().getEncoded()))
                .setTimestamp(transaction.getTimeStamp())
                .setHash(ByteString.copyFrom(Utils.hexStringToBytes(transaction.getHash())))
                .setSignature(ByteString.copyFrom(transaction.getSignature()))
                .build();
    }

    public static Transaction TransactionProtoToTransaction(TransactionProto transactionProto){
        return new Transaction(
                BidProtoToBid(transactionProto.getBid()),
                transactionProto.getSellerPublicKey().toByteArray(),
                transactionProto.getTimestamp(),
                Utils.bytesToHexString(transactionProto.getHash().toByteArray()),
                transactionProto.getSignature().toByteArray());
    }

    public static TransactionProto MinersRewardToTransactionProto(Transaction transaction){

        return TransactionProto.newBuilder()
                .setBid(BidMinersRewardToBidProto(transaction.getBid()))
                .setSellerPublicKey(ByteString.copyFrom(transaction.getSellerPublicKey().getEncoded()))
                .setTimestamp(transaction.getTimeStamp())
                .setHash(ByteString.copyFrom(Utils.hexStringToBytes(transaction.getHash())))
                .setSignature(ByteString.copyFrom(transaction.getSignature()))
                .build();
    }

    public static Transaction TransactionProtoToMinersReward(TransactionProto transactionProto){
        return new Transaction(
                BidProtoToBidMinersReward(transactionProto.getBid()),
                transactionProto.getSellerPublicKey().toByteArray(),
                transactionProto.getTimestamp(),
                Utils.bytesToHexString(transactionProto.getHash().toByteArray()),
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
