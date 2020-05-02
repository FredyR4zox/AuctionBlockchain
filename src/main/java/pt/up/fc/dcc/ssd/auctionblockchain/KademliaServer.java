package pt.up.fc.dcc.ssd.auctionblockchain;

import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class KademliaServer {
    private static final Logger logger = Logger.getLogger(KademliaServer.class.getName());

    private final int port;
    private final Server server;
//    private final KademliaNode myNode;
//    private final KBucketManager bucketManager;

    public KademliaServer(int port, KademliaNode myNode) throws IOException {
        this(port, myNode, new KBucketManager(myNode));
    }

    public KademliaServer(int port, KademliaNode myNode, KBucketManager bucketManager) throws IOException {
        this(ServerBuilder.forPort(port), port, myNode, bucketManager);
    }

    public KademliaServer(ServerBuilder<?> serverBuilder, int port, KademliaNode myNode, KBucketManager bucketManager) {
        this.port = port;
        this.server = serverBuilder.addService(new AuctionBlockchainService(myNode, bucketManager))
                .build();
//        this.myNode = myNode;
//        this.bucketManager = bucketManager;
    }

    /** Start serving requests. */
    public void start() throws IOException {
        server.start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    KademliaServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
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
        private final KademliaNode myNode;
        private final KBucketManager bucketManager;

        AuctionBlockchainService(KademliaNode myNode, KBucketManager bucketManager) {
            this.myNode = myNode;
            this.bucketManager = bucketManager;
        }

        @Override
        public void ping(NodeIDProto request, StreamObserver<NodeIDProto> responseObserver) {
            responseObserver.onNext(request);
            responseObserver.onCompleted();

            bucketManager.insertNode(new KademliaNode(request.getId().toByteArray()));
        }

        @Override
        public void store(StoreRequest request, StreamObserver<StoreResponse> responseObserver) {
            StoreResponse.Builder response = StoreResponse.newBuilder().setNodeID(request.getNodeID());

//            if(DHT.store(request.getKey(), request.getValue()))
//                response.setSuccess(true);
//            else
//                response.setSuccess(false);

            response.setSuccess(true);

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        }

        @Override
        public void findNode(FindNodeRequest request, StreamObserver<FindNodeResponse> responseObserver) {
            byte[] requestorID = KademliaUtils.NodeIDProtoToNodeID(request.getNodeID());
            byte[] requestedID = KademliaUtils.NodeIDProtoToNodeID(request.getRequestedNodeId());

            List<KademliaNode> nodes = bucketManager.getClosestNodes(requestorID, requestedID, KademliaUtils.k);

            FindNodeResponse response = FindNodeResponse.newBuilder()
                    .setNodeID(request.getNodeID())
                    .setBucket(KademliaUtils.KademliaNodeListToKBucketProto(nodes))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void findValue(FindValueRequest request, StreamObserver<FindValueResponse> responseObserver) {
            byte[] key = request.getKey().toByteArray();

            FindValueResponse.Builder responseBuilder = FindValueResponse.newBuilder().setNodeID(request.getNodeID());

//            byte[] mempoolKey;
//
//            try {
//                MessageDigest messageDigest = MessageDigest.getInstance(KademliaUtils.hashAlgorithm);
//                mempoolKey = messageDigest.digest("mempool".getBytes(KademliaUtils.charset));
//            } catch (NoSuchAlgorithmException e) {
//                logger.warning("Error: Could not find hash algorithm " + KademliaUtils.hashAlgorithm);
//                e.printStackTrace();
//
//                responseObserver.onNext(FindValueResponse.newBuilder().build());
//                responseObserver.onCompleted();
//                return;
//            }
//
//            if(mempoolKey != null && Arrays.equals(key, mempoolKey)) {
//                List<Transaction> transactions = DHT.getMempool();
//                responseBuilder.setTransactions(KademliaUtils.TransactionListToMempoolProto(transactions));
//            }
//            else {
//                if(DHT.contains(key)) {
//                    responseBuilder.setBlock(KademliaUtils.BlockToBlockProto(DHT.get(key)));
//                }
//                else {
//                    List<KademliaNode> nodes = bucketManager.getClosestNodes(request.getNodeID().getId().toByteArray(), key, KademliaUtils.k);
//                    responseBuilder.setBucket(KademliaUtils.KademliaNodeListToKBucketProto(nodes));
//                }
//            }


            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        }
    }
}
