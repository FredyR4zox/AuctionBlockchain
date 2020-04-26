package pt.up.fc.dcc.ssd.auctionblockchain;

import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class KademliaServer {
    private static final Logger logger = Logger.getLogger(KademliaServer.class.getName());

    private final int port;
    private final Server server;

    public KademliaServer(int port, KademliaNode myNode) throws IOException {
        this(port, myNode, new KBucketManager(myNode));
    }

    /** Create a RouteGuide server listening on {@code port} using {@code featureFile} database. */
    public KademliaServer(int port, KademliaNode myNode, KBucketManager bucketManager) throws IOException {
        this(ServerBuilder.forPort(port), port, myNode, bucketManager);
    }

    /** Create a RouteGuide server using serverBuilder as a base and features as data. */
    public KademliaServer(ServerBuilder<?> serverBuilder, int port, KademliaNode myNode, KBucketManager kBucketManager) {
        this.port = port;
        server = serverBuilder.addService(new AuctionBlockchainService(myNode, kBucketManager))
                .build();
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
        public void ping(NodeID request, StreamObserver<NodeID> responseObserver) {
            responseObserver.onNext(request);
            responseObserver.onCompleted();
        }

        @Override
        public void store(StoreRequest request, StreamObserver<StoreResponse> responseObserver) {
            StoreResponse response = StoreResponse.newBuilder().setNodeID(request.getNodeID()).setSuccess(true).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void findNode(FindNodeRequest request, StreamObserver<FindNodeResponse> responseObserver) {
            NodeID requestedID = request.getRequestedNodeId();
            KademliaNodeProto node = KademliaNodeProto.newBuilder().setNodeID(NodeID.newBuilder().setId(ByteString.copyFrom(myNode.getNodeID())).build()).setIpAddress(myNode.getIpAddress()).setPort(myNode.getPort()).build();

            KBucketProto bucket = KBucketProto.newBuilder().addNodes(node).build();

            responseObserver.onNext(FindNodeResponse.newBuilder().setNodeID(requestedID).setBucket(bucket).build());
            responseObserver.onCompleted();
        }

        @Override
        public void findValue(FindValueRequest request, StreamObserver<FindValueResponse> responseObserver) {
            KademliaNodeProto node = KademliaNodeProto.newBuilder().setNodeID(NodeID.newBuilder().setId(ByteString.copyFrom(myNode.getNodeID())).build()).setIpAddress(myNode.getIpAddress()).setPort(myNode.getPort()).build();

            KBucketProto bucket = KBucketProto.newBuilder().addNodes(node).build();

            responseObserver.onNext(FindValueResponse.newBuilder().setNodeID(NodeID.newBuilder().setId(ByteString.copyFrom(myNode.getNodeID())).build()).setBucket(bucket).build());
            responseObserver.onCompleted();
        }
    }
}
