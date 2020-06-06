package pt.up.fc.dcc.ssd.auctionblockchain;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import pt.up.fc.dcc.ssd.auctionblockchain.Auction.Auction;
import pt.up.fc.dcc.ssd.auctionblockchain.Auction.AuctionManager;
import pt.up.fc.dcc.ssd.auctionblockchain.Auction.AuctionsState;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.*;
import pt.up.fc.dcc.ssd.auctionblockchain.Client.Bid;
import pt.up.fc.dcc.ssd.auctionblockchain.Client.Client;
import pt.up.fc.dcc.ssd.auctionblockchain.Kademlia.*;

import java.security.SecureRandom;
import java.security.Security;
import java.util.List;
import java.util.Random;

import static java.lang.Thread.sleep;

public class Main {
    public static void main(String[] args) throws InterruptedException, java.io.IOException {
        if(args.length < 1){
            printUsage();
            return;
        }

        Security.addProvider(new BouncyCastleProvider());

        String myIpAddress = KademliaUtils.getMyIpAddress();
        if(myIpAddress == null){
            System.out.println("Error getting IP address");
            return;
        }
        System.out.println("My IP address is " + myIpAddress);

        if(args[0].equals("bootstrap")){
            int port;
            if(args.length > 1)
                port = Integer.parseInt(args[1]);
            else
                port = KademliaUtils.bootstrapNodePort;

            startBootstrap(myIpAddress, port);
        }

        else if(args[0].equals("listener")){
            if(args.length < 2){
                printUsage();
                return;
            }

            int port = Integer.parseInt(args[1]);

            String defaultBootstrapNodeIP = KademliaUtils.bootstrapNodeIP;
            if(args.length >= 3)
                defaultBootstrapNodeIP = args[2];

            int defaultBootstrapNodePort = KademliaUtils.bootstrapNodePort;
            if(args.length >= 4)
                defaultBootstrapNodePort = Integer.parseInt(args[3]);

            startListener(myIpAddress, port, defaultBootstrapNodeIP, defaultBootstrapNodePort);
        }

        else if(args[0].equals("miner")){
            if(args.length < 2){
                printUsage();
                return;
            }

            int port = Integer.parseInt(args[1]);

            String defaultBootstrapNodeIP = KademliaUtils.bootstrapNodeIP;
            if(args.length >= 3)
                defaultBootstrapNodeIP = args[2];

            int defaultBootstrapNodePort = KademliaUtils.bootstrapNodePort;
            if(args.length >= 4)
                defaultBootstrapNodePort = Integer.parseInt(args[3]);

            startMiner(myIpAddress, port, defaultBootstrapNodeIP, defaultBootstrapNodePort);
        }
    }

    public static void printUsage(){
        System.out.println("Usage: java -jar AuctionBlockchain.jar nodeType [options]");

        System.out.println("Node types: bootstrap");
        System.out.println("            listener");
        System.out.println("            miner");
        System.out.println("            auctioneer");

        System.out.println("Example for bootstrap:    java -jar AuctionBlockchain.jar bootstrap    [grpcPort]");
        System.out.println("Example for listener:     java -jar AuctionBlockchain.jar listener     grpcPort [bootstrapNodeIpAddress bootstrapNodePort]");
        System.out.println("Example for miner:        java -jar AuctionBlockchain.jar miner        grpcPort [bootstrapNodeIpAddress bootstrapNodePort]");
        System.out.println("Example for auctioneer:   java -jar AuctionBlockchain.jar auctioneer   grpcPort [bootstrapNodeIpAddress bootstrapNodePort]");

        System.out.println("Default values for: bootstrapNodeIpAddress              34.105.188.87");
        System.out.println("                    bootstrapNodePort                   1337");
        System.out.println("                    grpcPort option in bootstrap type   1337");
    }

    public static void startBootstrap(String myIpAddress, int grpcPort) throws InterruptedException, java.io.IOException {
        KademliaNode myNode = new KademliaNode(myIpAddress, grpcPort, KademliaUtils.bootstrapNodeID);
        KBucketManager manager = new KBucketManager(myNode);

        KademliaServer server = new KademliaServer(grpcPort, manager);
        server.start();

        Wallet creator = new Wallet();
        System.out.println("Creator address: " + creator.getAddress());
        System.out.println("Creator public key: " + creator.getPubKey().toString());
        System.out.println("Creator private key: " + creator.getPrivKey().toString());


        KademliaClient client = new KademliaClient(manager);
        BlockchainUtils.setKademliaClient(client);

        BlockchainUtils.createGenesisBlock(creator);



        byte[] rand = new byte[Utils.hashAlgorithmLengthInBytes];
        Random random = new SecureRandom();
        random.nextBytes(rand);

        Wallet alice = Wallet.createWalletFromFile("alice");
        Bid bid = new Bid(creator, Utils.bytesToHexString(rand), alice.getAddress(), BlockchainUtils.minerReward/2, 5);
        Transaction transaction = new Transaction(alice, bid);

        BlockchainUtils.addTransaction(transaction);
        client.announceNewTransaction(transaction);


        random.nextBytes(rand);
        Wallet bob = Wallet.createWalletFromFile("bob");
        bid = new Bid(creator, Utils.bytesToHexString(rand), bob.getAddress(), BlockchainUtils.minerReward/2, 5);
        transaction = new Transaction(bob, bid);

        BlockchainUtils.addTransaction(transaction);
        client.announceNewTransaction(transaction);

        server.blockUntilShutdown();
    }

    public static void startListener(String myIpAddress, int grpcPort, String defaultBootstrapNodeIP, int defaultBootstrapNodePort) throws InterruptedException, java.io.IOException {
        byte[] randomNodeID = new byte[Utils.hashAlgorithmLengthInBytes];
        Random random = new SecureRandom();
        random.nextBytes(randomNodeID);

        KademliaNode myNode = new KademliaNode(myIpAddress, grpcPort, randomNodeID);

        KBucketManager bucketManager = new KBucketManager(myNode);


        KademliaServer server = new KademliaServer(grpcPort, bucketManager);
        server.start();


//            Wallet alice = Wallet.createWalletFromFile("alice");
//            MinerUtils.startMining(alice);

        KademliaClient client = new KademliaClient(bucketManager);
//            BlockchainUtils.setKademliaClient(client);

        random.nextBytes(randomNodeID);
        KademliaNode bootstrapNode = new KademliaNode(defaultBootstrapNodeIP, defaultBootstrapNodePort, KademliaUtils.bootstrapNodeID);

        client.bootstrap(bootstrapNode);

        client.bootstrapBlockchain();

        List<Transaction> mempool = client.getMempool();
        for(Transaction trans : mempool)
            BlockchainUtils.addTransaction(trans);

        List<Auction> auctions = client.getAuctions();
        for(Auction auction : auctions) {
            AuctionsState.addAuction(auction);

            List<Bid> bids = client.getBidsFromAuction(auction);
            for(Bid bid : bids)
                AuctionsState.updateBid(bid);
        }

        server.blockUntilShutdown();
    }

    public static void startMiner(String myIpAddress, int grpcPort, String defaultBootstrapNodeIP, int defaultBootstrapNodePort) throws InterruptedException, java.io.IOException {
        byte[] randomNodeID = new byte[Utils.hashAlgorithmLengthInBytes];
        Random random = new SecureRandom();
        random.nextBytes(randomNodeID);

        KademliaNode myNode = new KademliaNode(myIpAddress, grpcPort, randomNodeID);

        KBucketManager bucketManager = new KBucketManager(myNode);


        KademliaServer server = new KademliaServer(grpcPort, bucketManager);
        server.start();


        Wallet alice = Wallet.createWalletFromFile("alice");
        MinerUtils.startMining(alice);

        KademliaClient client = new KademliaClient(bucketManager);
        BlockchainUtils.setKademliaClient(client);

        random.nextBytes(randomNodeID);
        KademliaNode bootstrapNode = new KademliaNode(defaultBootstrapNodeIP, defaultBootstrapNodePort, KademliaUtils.bootstrapNodeID);

        client.bootstrap(bootstrapNode);

        client.bootstrapBlockchain();

        List<Transaction> mempool = client.getMempool();
        for(Transaction trans : mempool)
            BlockchainUtils.addTransaction(trans);

        List<Auction> auctions = client.getAuctions();
        for(Auction auction : auctions) {
            AuctionsState.addAuction(auction);

            List<Bid> bids = client.getBidsFromAuction(auction);
            for(Bid bid : bids)
                AuctionsState.updateBid(bid);
        }

        server.blockUntilShutdown();
    }
}
