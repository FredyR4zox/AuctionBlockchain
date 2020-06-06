package pt.up.fc.dcc.ssd.auctionblockchain.Kademlia;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import pt.up.fc.dcc.ssd.auctionblockchain.*;
import pt.up.fc.dcc.ssd.auctionblockchain.Auction.Auction;
import pt.up.fc.dcc.ssd.auctionblockchain.Auction.AuctionsState;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.Block;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.BlockchainUtils;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.Transaction;
import pt.up.fc.dcc.ssd.auctionblockchain.Client.Bid;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KademliaServer {
    private static final Logger logger = Logger.getLogger(KademliaServer.class.getName());

    private final int port;
    private final Server server;
    private final KBucketManager bucketManager;


    public KademliaServer(int port, KBucketManager bucketManager) {
        this(ServerBuilder.forPort(port), port, bucketManager);
    }

    public KademliaServer(ServerBuilder<?> serverBuilder, int port, KBucketManager bucketManager) {
        this.port = port;
        this.server = serverBuilder.addService(new AuctionBlockchainService(bucketManager))
                .build();
        this.bucketManager = bucketManager;
    }

    /** Start serving requests. */
    public void start() throws IOException {
        server.start();
        logger.log(Level.INFO, "Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                logger.log(Level.INFO, "*** shutting down gRPC server since JVM is shutting down");
                try {
                    KademliaServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                logger.log(Level.INFO, "*** server shut down");
            }
        });
    }

    /** Stop serving requests and shutdown resources. */
    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }



    private static class AuctionBlockchainService extends AuctionBlockchainGrpc.AuctionBlockchainImplBase {
        private final KBucketManager bucketManager;
        private final KademliaClient kademliaClient;

        AuctionBlockchainService(KBucketManager bucketManager) {
            this.bucketManager = bucketManager;
            this.kademliaClient = new KademliaClient(bucketManager);
        }

        @Override
        public void ping(KademliaNodeProto request, StreamObserver<KademliaNodeProto> responseObserver) {
            responseObserver.onNext(KademliaUtils.KademliaNodeToKademliaNodeProto(bucketManager.getMyNode()));
            responseObserver.onCompleted();

            KademliaNode node =  KademliaUtils.KademliaNodeProtoToKademliaNode(request);
            bucketManager.insertNode(node);

            logger.log(Level.INFO, "Processed PING RPC from " + node);
        }

        @Override
        public void store(StoreRequest request, StreamObserver<StoreResponse> responseObserver) {
            logger.log(Level.INFO, "Received a STORE RPC");

            StoreResponse.Builder response = StoreResponse.newBuilder().setNode(KademliaUtils.KademliaNodeToKademliaNodeProto(bucketManager.getMyNode()));

            StoreRequest.BlockOrTransactionOrAuctionOrBidCase type = request.getBlockOrTransactionOrAuctionOrBidCase();
            boolean result = false;

            if(type == StoreRequest.BlockOrTransactionOrAuctionOrBidCase.TRANSACTION){
                Transaction transaction = KademliaUtils.TransactionProtoToTransaction(request.getTransaction());

                result = BlockchainUtils.addTransaction(transaction);

                if(result)
                    logger.log(Level.INFO, "Added transaction with hash " + transaction.getHash());
                else
                    logger.log(Level.WARNING, "Could not add transaction with hash " + transaction.getHash());
            }
            else if(type == StoreRequest.BlockOrTransactionOrAuctionOrBidCase.BLOCK){
                Block block = KademliaUtils.BlockProtoToBlock(request.getBlock());

                result = BlockchainUtils.addBlock(block);

                if(result)
                    logger.log(Level.INFO, "Added block with hash " + block.getHash());
                else
                    logger.log(Level.WARNING, "Could not add block with hash " + block.getHash());
            }
            else if(type == StoreRequest.BlockOrTransactionOrAuctionOrBidCase.AUCTION){
                Auction auction = KademliaUtils.AuctionProtoToAuction(request.getAuction());

                result = AuctionsState.addAuction(auction);

                if(result)
                    logger.log(Level.INFO, "Added auction with hash " + auction.getHash());
                else
                    logger.log(Level.WARNING, "Could not add auction with hash " + auction.getHash());

            }
            else if(type == StoreRequest.BlockOrTransactionOrAuctionOrBidCase.BID){
                Bid bid = KademliaUtils.BidProtoToBid(request.getBid());

                result = AuctionsState.updateBid(bid);

                if(result)
                    logger.log(Level.INFO, "Added bid with hash " + bid.getHash());
                else
                    logger.log(Level.WARNING, "Could not add bid with hash " + bid.getHash());
            }
            else
                logger.log(Level.SEVERE, "Error: Type of store request not known");


            response.setSuccess(result);

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();

            KademliaNode node =  KademliaUtils.KademliaNodeProtoToKademliaNode(request.getNode());
            bucketManager.insertNode(node);

            if(result) {
                if (type == StoreRequest.BlockOrTransactionOrAuctionOrBidCase.TRANSACTION) {
                    Transaction transaction = KademliaUtils.TransactionProtoToTransaction(request.getTransaction());

                    kademliaClient.announceNewTransaction(transaction);
                }
                else if (type == StoreRequest.BlockOrTransactionOrAuctionOrBidCase.BLOCK) {
                    Block block = KademliaUtils.BlockProtoToBlock(request.getBlock());

                    kademliaClient.announceNewBlock(block);
                }
                else if (type == StoreRequest.BlockOrTransactionOrAuctionOrBidCase.AUCTION) {
                    Auction auction = KademliaUtils.AuctionProtoToAuction(request.getAuction());

                    kademliaClient.announceNewAuction(auction);
                }
                else if (type == StoreRequest.BlockOrTransactionOrAuctionOrBidCase.BID) {
                    Bid bid = KademliaUtils.BidProtoToBid(request.getBid());

                    kademliaClient.announceNewBid(bid);
                }
                else
                    logger.log(Level.SEVERE, "Error: Type of store request not known");
            }


            logger.log(Level.INFO, "Processed STORE RPC from " + node);
        }

        @Override
        public void findNode(FindNodeRequest request, StreamObserver<FindNodeResponse> responseObserver) {
            logger.log(Level.INFO, "Received a FIND_NODE RPC");

            byte[] requestorID = KademliaUtils.NodeIDProtoToNodeID(request.getNode().getNodeID());
            byte[] requestedID = KademliaUtils.NodeIDProtoToNodeID(request.getRequestedNodeId());

            logger.log(Level.INFO, "Processing a FIND_NODE RPC for key " + Utils.bytesToHexString(requestedID));

            List<KademliaNode> nodes = bucketManager.getClosestNodes(requestorID, requestedID, KademliaUtils.k);

            FindNodeResponse response = FindNodeResponse.newBuilder()
                    .setNode(KademliaUtils.KademliaNodeToKademliaNodeProto(bucketManager.getMyNode()))
                    .setBucket(KademliaUtils.KademliaNodeListToKBucketProto(nodes))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            KademliaNode node =  KademliaUtils.KademliaNodeProtoToKademliaNode(request.getNode());
            bucketManager.insertNode(node);

            logger.log(Level.INFO, "Processed FIND_NODE RPC from " + node);
        }

        @Override
        public void findValue(FindValueRequest request, StreamObserver<FindValueResponse> responseObserver) {
            logger.log(Level.INFO, "Received a FIND_VALUE RPC");

            byte[] key = request.getKey().toByteArray();

            logger.log(Level.INFO, "Processing a FIND_VALUE RPC for key " + Utils.bytesToHexString(key));

            FindValueResponse.Builder responseBuilder = FindValueResponse.newBuilder().setNode(KademliaUtils.KademliaNodeToKademliaNodeProto(bucketManager.getMyNode()));

            byte[] mempoolKey = KademliaUtils.mempoolHash();
            byte[] auctionsKey = KademliaUtils.auctionsHash();


            KademliaNode node =  KademliaUtils.KademliaNodeProtoToKademliaNode(request.getNode());

            if(mempoolKey != null && Arrays.equals(key, mempoolKey)) {
                logger.log(Level.INFO, "Sending mempool.");
                List<Transaction> transactions = new ArrayList<>(BlockchainUtils.getLongestChain().getUnconfirmedTransaction());
                responseBuilder.setTransactions(KademliaUtils.TransactionListToMempoolProto(transactions));
            }
            else if(auctionsKey != null && Arrays.equals(key, auctionsKey)) {
                logger.log(Level.INFO, "Sending all auctions.");
                List<Auction> auctions = new ArrayList<>(AuctionsState.getAuctions());
                responseBuilder.setAuctions(KademliaUtils.AuctionListToAuctionListProto(auctions));
            }
            // Its not a special key so it must be a block or an auction (bids of that auction)
            else {
                String keyString = Utils.bytesToHexString(key);

                Block block = BlockchainUtils.getBlockWithPreviousHash(keyString);
                Auction auction = AuctionsState.getAuction(keyString);

                if(block != null) {
                    logger.log(Level.INFO, "Sending block with previous hash " + keyString);
                    responseBuilder.setBlock(KademliaUtils.BlockToBlockProto(block));
                }
                else if(auction != null) {
                    logger.log(Level.INFO, "Sending auction bids with hash " + keyString);
                    List<Bid> bids = new ArrayList<>(AuctionsState.getAuctionBidsTreeSet(keyString));
                    responseBuilder.setBids(KademliaUtils.BidListToBidListProto(bids));
                }
                else {
                    logger.log(Level.INFO, "Sending kbuckets because no value was found to hash " + keyString);
                    List<KademliaNode> nodes = bucketManager.getClosestNodes(node.getNodeID(), key, KademliaUtils.k);
                    responseBuilder.setBucket(KademliaUtils.KademliaNodeListToKBucketProto(nodes));
                }
            }


            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

            bucketManager.insertNode(node);

            logger.log(Level.INFO, "Processed FIND_VALUE RPC from " + node);
        }
    }
}
