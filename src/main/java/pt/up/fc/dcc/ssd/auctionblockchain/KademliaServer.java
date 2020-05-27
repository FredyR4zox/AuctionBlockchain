package pt.up.fc.dcc.ssd.auctionblockchain;

import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KademliaServer {
    private static final Logger logger = Logger.getLogger(KademliaServer.class.getName());

    private final int port;
    private final Server server;
//    private final KademliaNode myNode;
    private final KBucketManager bucketManager;

//    public KademliaServer(int port) throws IOException {
//        byte[] rand = new byte[KademliaUtils.idSizeInBytes];
//        Random random = new Random();
//        random.nextBytes(rand);
//        this(port, new KBucketManager(new KademliaNode(rand)));
//    }

    public KademliaServer(int port, KBucketManager bucketManager, MinerUtils minerUtils) throws IOException {
        this(ServerBuilder.forPort(port), port, bucketManager, minerUtils);
    }

    public KademliaServer(ServerBuilder<?> serverBuilder, int port, KBucketManager bucketManager, MinerUtils minerUtils) {
        this.port = port;
        this.server = serverBuilder.addService(new AuctionBlockchainService(bucketManager, minerUtils))
                .build();
//        this.myNode = myNode;
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


    /**
     * Main method.  This comment makes the linter happy.
     */
//    public static void main(String[] args) throws Exception {
//        byte[] myNodeID = {00, 00, 00, 00, 01, 00, 00, 00, 00, 01, 00, 00, 00, 00, 01, 00, 00, 00, 00, 01};
//
//        KademliaNode myNode = new KademliaNode("127.0.0.1", 1337, myNodeID);
//        KBucketManager bucketManager = new KBucketManager(myNode);
//
//        KademliaServer server = new KademliaServer(8980, myNode, bucketManager);
//
//        server.start();
//        server.blockUntilShutdown();
//    }


    private static class AuctionBlockchainService extends AuctionBlockchainGrpc.AuctionBlockchainImplBase {
        private final KBucketManager bucketManager;
        private final MinerUtils minerUtils;
//        private final KademliaNode myNode;

        AuctionBlockchainService(KBucketManager bucketManager, MinerUtils minerUtils) {
            this.bucketManager = bucketManager;
            this.minerUtils = minerUtils;
//            this.myNode = myNode;
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

            StoreRequest.BlockOrTransactionCase type = request.getBlockOrTransactionCase();
            boolean result = false;

            if(type == StoreRequest.BlockOrTransactionCase.TRANSACTION){
                Transaction transaction = KademliaUtils.TransactionProtoToTransaction(request.getTransaction());

                result = minerUtils.addTransactionIfValidToPool(transaction);
            }
            else if(type == StoreRequest.BlockOrTransactionCase.BLOCK){
                Block block = KademliaUtils.BlockProtoToBlock(request.getBlock());

                result = BlockChain.checkAddBlock(block);
            }
            else
                logger.log(Level.SEVERE, "Error: Type of store request not known");


            response.setSuccess(result);

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();

            KademliaNode node =  KademliaUtils.KademliaNodeProtoToKademliaNode(request.getNode());
            bucketManager.insertNode(node);

            logger.log(Level.INFO, "Processed STORE RPC from " + node);
        }

        @Override
        public void findNode(FindNodeRequest request, StreamObserver<FindNodeResponse> responseObserver) {
            logger.log(Level.INFO, "Received a FIND_NODE RPC");

            byte[] requestorID = KademliaUtils.NodeIDProtoToNodeID(request.getNode().getNodeID());
            byte[] requestedID = KademliaUtils.NodeIDProtoToNodeID(request.getRequestedNodeId());

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

            FindValueResponse.Builder responseBuilder = FindValueResponse.newBuilder().setNode(KademliaUtils.KademliaNodeToKademliaNodeProto(bucketManager.getMyNode()));

            byte[] mempoolKey;
//            byte[] lastBlocksKey;

            try {
                MessageDigest messageDigest = MessageDigest.getInstance(KademliaUtils.hashAlgorithm);
                mempoolKey = messageDigest.digest("mempool".getBytes(KademliaUtils.charset));
            } catch (NoSuchAlgorithmException e) {
                logger.log(Level.SEVERE, "Error: Could not find hash algorithm " + KademliaUtils.hashAlgorithm);
                e.printStackTrace();

                responseObserver.onNext(responseBuilder.build());
                responseObserver.onCompleted();
                return;
            }

//            try {
//                MessageDigest messageDigest = MessageDigest.getInstance(KademliaUtils.hashAlgorithm);
//                lastBlocksKey = messageDigest.digest("lastBlock".getBytes(KademliaUtils.charset));
//            } catch (NoSuchAlgorithmException e) {
//                logger.log(Level.SEVERE, "Error: Could not find hash algorithm " + KademliaUtils.hashAlgorithm);
//                e.printStackTrace();
//
//                responseObserver.onNext(responseBuilder.build());
//                responseObserver.onCompleted();
//                return;
//            }


            KademliaNode node =  KademliaUtils.KademliaNodeProtoToKademliaNode(request.getNode());

            if(mempoolKey != null && Arrays.equals(key, mempoolKey)) {
                List<Transaction> transactions = new ArrayList<>(minerUtils.getTransPool());
                responseBuilder.setTransactions(KademliaUtils.TransactionListToMempoolProto(transactions));
            }
//            else if(lastBlocksKey != null && Arrays.equals(key, lastBlocksKey)) {
//                Block lastBlock = BlockChain.getBlockWithHash(BlockChain.getLastHash());
//
//                if(lastBlock != null)
//                    responseBuilder.setBlock(KademliaUtils.BlockToBlockProto(lastBlock));
//            }
            else {
                Block block = BlockChain.getBlockWithHash(new String(key));
                logger.log(Level.INFO, "Could not get block with hash " + new String(key));

                if(block == null){
                    List<KademliaNode> nodes = bucketManager.getClosestNodes(node.getNodeID(), key, KademliaUtils.k);
                    responseBuilder.setBucket(KademliaUtils.KademliaNodeListToKBucketProto(nodes));
                }
                else {
                    responseBuilder.setBlock(KademliaUtils.BlockToBlockProto(block));
                }
            }


            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

            bucketManager.insertNode(node);

            logger.log(Level.INFO, "Processed FIND_NODE RPC from " + node);
        }
    }
}
