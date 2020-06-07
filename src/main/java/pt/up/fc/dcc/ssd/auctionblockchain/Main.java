package pt.up.fc.dcc.ssd.auctionblockchain;

import jdk.jshell.execution.Util;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import pt.up.fc.dcc.ssd.auctionblockchain.Auction.Auction;
import pt.up.fc.dcc.ssd.auctionblockchain.Auction.AuctionManager;
import pt.up.fc.dcc.ssd.auctionblockchain.Auction.AuctionsState;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.*;
import pt.up.fc.dcc.ssd.auctionblockchain.Client.Bid;
import pt.up.fc.dcc.ssd.auctionblockchain.Client.Client;
import pt.up.fc.dcc.ssd.auctionblockchain.Kademlia.*;

import java.io.File;
import java.security.SecureRandom;
import java.security.Security;
import java.util.List;
import java.util.Random;

import static java.lang.Thread.sleep;

public class Main {
    public static void main(String[] args) throws InterruptedException, java.io.IOException {
        System.out.println("Welcome to the AuctionBlockchain! A Public Ledger for Auctions.\n");

        if (args.length < 1) {
            printUsage();
            return;
        }

        Security.addProvider(new BouncyCastleProvider());


        String walletFile = Wallet.defaultWalletFile;
        if (args.length > 1)
            walletFile = args[1];

        File directory = new File(Wallet.WALLETS_PATH);
        if (!directory.exists()){
            if(!directory.mkdir()){
                System.out.println("Error creating directory for wallets: " + Wallet.WALLETS_PATH);
                return;
            }
        }

        Wallet wallet = Wallet.createWalletFromFile(walletFile);
        if (wallet == null) {
            System.out.println("File not found. Generating a new wallet and saving it to file.");

            wallet = new Wallet();
            System.out.println("---Generated wallet---");
            System.out.println("\tAddress: " + wallet.getAddress());
            System.out.println("\tPublic key: " + wallet.getPubKey().toString());

            if (Wallet.createFileWithWallet(walletFile, wallet) == null) {
                System.out.println("Error writing wallet to file " + Wallet.WALLETS_PATH + walletFile);
                return;
            }
        }

        System.out.println("Using wallet file: " + Wallet.WALLETS_PATH + walletFile);


        String myIpAddress = KademliaUtils.defaultIpAddress;
        if (args.length > 2)
            myIpAddress = args[2];

        if (myIpAddress.equals(KademliaUtils.defaultIpAddress)) {
            myIpAddress = KademliaUtils.getMyIpAddress();
            if (myIpAddress == null) {
                System.out.println("Error getting IP address.\n");
                return;
            }
        }
        System.out.println("Using IP address: " + myIpAddress);


        int port = KademliaUtils.defaultPort;
        if (args.length > 3) {
            try {
                port = Integer.parseInt(args[3]);
            }catch(NumberFormatException e){
                System.out.println("Error converting port number " + args[3] + " to int.\n");
                printUsage();
                return;
            }
        }

        System.out.println("Using port: " + port);


        if (args[0].equals("bootstrap")) {
            startBootstrap(wallet, myIpAddress, port);
            return;
        }


        String bootstrapIpAddress = KademliaUtils.bootstrapNodeIP;
        if (args.length > 4)
            bootstrapIpAddress = args[4];

        System.out.println("Using bootstrap node IP address: " + bootstrapIpAddress);


        int bootstrapPort = KademliaUtils.bootstrapNodePort;
        if (args.length > 5){
            try {
                bootstrapPort = Integer.parseInt(args[5]);
            }catch(NumberFormatException e){
                System.out.println("Error converting bootstrap node port number " + args[5] + " to int.\n");
                printUsage();
                return;
            }
        }

        System.out.println("Using bootstrap node port: " + bootstrapPort);


        if (args[0].equals("miner")) {
            startMiner(wallet, myIpAddress, port, bootstrapIpAddress, bootstrapPort);
            return;
        } else if (args[0].equals("auctioneer")) {
            startAuctioneer(wallet, myIpAddress, port, bootstrapIpAddress, bootstrapPort);
            return;
        } else if (args[0].equals("client")) {
            startClient(wallet, myIpAddress, port, bootstrapIpAddress, bootstrapPort);
            return;
        }

    }

    public static void printUsage() {
        System.out.println("Usage: java -jar AuctionBlockchain.jar nodeType [options]");
        System.out.println();
        System.out.println("Node types: bootstrap    (Always on node to help other nodes join the network)");
        System.out.println("            miner        (Node that mines blocks)");
        System.out.println("            auctioneer   (Node that controls an auction)");
        System.out.println("            client       (Node that a client uses to bid on an auction)");
        System.out.println();
        System.out.println("Example for bootstrap:    java -jar AuctionBlockchain.jar bootstrap    [walletFile myIpAddress grpcPort]");
        System.out.println("Example for miner:        java -jar AuctionBlockchain.jar miner        [walletFile myIpAddress grpcPort bootstrapNodeIpAddress bootstrapNodePort]");
        System.out.println("Example for auctioneer:   java -jar AuctionBlockchain.jar auctioneer   [walletFile myIpAddress grpcPort bootstrapNodeIpAddress bootstrapNodePort]");
        System.out.println("Example for client:       java -jar AuctionBlockchain.jar client       [walletFile myIpAddress grpcPort bootstrapNodeIpAddress bootstrapNodePort]");
        System.out.println();
        System.out.println("Default values for:   walletFile               " + Wallet.WALLETS_PATH + Wallet.defaultWalletFile);
        System.out.println("                      myIpAddress              your public IP address. This can be changed to 127.0.0.1 if you don't have port forwarding on your router or you want to test it locally. Can also be set to 0.0.0.0 to automatically set your public address.");
        System.out.println("                      grpcPort                 1337");
        System.out.println("                      bootstrapNodeIpAddress   34.105.188.87");
        System.out.println("                      bootstrapNodePort        1337");
        System.out.println();
        System.out.println("Wallets are saved in the " + Wallet.WALLETS_PATH + " directory.");
    }

    public static void startBootstrap(Wallet wallet, String myIpAddress, int grpcPort) throws InterruptedException, java.io.IOException {
        System.out.println("Using nodeId: " + Utils.bytesToHexString(KademliaUtils.bootstrapNodeID) + "\n");

        KademliaNode myNode = new KademliaNode(myIpAddress, grpcPort, KademliaUtils.bootstrapNodeID);
        KBucketManager manager = new KBucketManager(myNode);

        KademliaServer server = new KademliaServer(grpcPort, manager);
        server.start();

        KademliaClient client = new KademliaClient(manager);
        BlockchainUtils.setKademliaClient(client);

        BlockchainUtils.createGenesisBlock(wallet);


        // Uncomment the following code to automatically send coins from the mined genesis block to alice and bob (50 coins each)

        /*
        System.out.println("---Sending 50 coins to alice---");

        byte[] rand = new byte[Utils.hashAlgorithmLengthInBytes];
        Random random = new SecureRandom();
        random.nextBytes(rand);

        Wallet alice = Wallet.createWalletFromFile("alice.txt");
        Bid bid = new Bid(wallet, Utils.bytesToHexString(rand), alice.getAddress(), BlockchainUtils.minerReward/2, 5);
        Transaction transaction = new Transaction(alice, bid);

        BlockchainUtils.addTransaction(transaction);
        client.announceNewTransaction(transaction);



        System.out.println("---Sending 50 coins to bob---");

        random.nextBytes(rand);

        Wallet bob = Wallet.createWalletFromFile("bob.txt");
        bid = new Bid(wallet, Utils.bytesToHexString(rand), bob.getAddress(), BlockchainUtils.minerReward/2, 5);
        transaction = new Transaction(bob, bid);

        BlockchainUtils.addTransaction(transaction);
        client.announceNewTransaction(transaction);
        */


        server.blockUntilShutdown();
    }

    public static void startMiner(Wallet wallet, String myIpAddress, int grpcPort, String defaultBootstrapNodeIP, int defaultBootstrapNodePort) throws InterruptedException, java.io.IOException {
        byte[] randomNodeID = new byte[Utils.hashAlgorithmLengthInBytes];
        Random random = new SecureRandom();
        random.nextBytes(randomNodeID);

        System.out.println("Using nodeId: " + Utils.bytesToHexString(randomNodeID) + "\n");

        KademliaNode myNode = new KademliaNode(myIpAddress, grpcPort, randomNodeID);
        KBucketManager bucketManager = new KBucketManager(myNode);

        KademliaServer server = new KademliaServer(grpcPort, bucketManager);
        server.start();

        MinerUtils.startMining(wallet);

        KademliaClient client = new KademliaClient(bucketManager);
        BlockchainUtils.setKademliaClient(client);

        KademliaNode bootstrapNode = new KademliaNode(defaultBootstrapNodeIP, defaultBootstrapNodePort, KademliaUtils.bootstrapNodeID);

        client.bootstrap(bootstrapNode);
        client.bootstrapBlockchain();


        List<Transaction> mempool = client.getMempool();
        for (Transaction trans : mempool)
            BlockchainUtils.addTransaction(trans);


        List<Auction> auctions = client.getAuctions();
        for (Auction auction : auctions) {
            AuctionsState.addAuction(auction);

            List<Bid> bids = client.getBidsFromAuction(auction);
            for (Bid bid : bids)
                AuctionsState.updateBid(bid);
        }


        server.blockUntilShutdown();
    }

    public static void startClient(Wallet wallet, String myIpAddress, int grpcPort, String defaultBootstrapNodeIP, int defaultBootstrapNodePort) throws InterruptedException, java.io.IOException {
        byte[] randomNodeID = new byte[Utils.hashAlgorithmLengthInBytes];
        Random random = new SecureRandom();
        random.nextBytes(randomNodeID);

        System.out.println("Using nodeId: " + Utils.bytesToHexString(randomNodeID) + "\n");

        KademliaNode myNode = new KademliaNode(myIpAddress, grpcPort, randomNodeID);
        KBucketManager bucketManager = new KBucketManager(myNode);

        KademliaServer server = new KademliaServer(grpcPort, bucketManager);
        server.start();

        KademliaClient client = new KademliaClient(bucketManager);
        BlockchainUtils.setKademliaClient(client);

        KademliaNode bootstrapNode = new KademliaNode(defaultBootstrapNodeIP, defaultBootstrapNodePort, KademliaUtils.bootstrapNodeID);

        client.bootstrap(bootstrapNode);
        client.bootstrapBlockchain();


        List<Transaction> mempool = client.getMempool();
        for (Transaction trans : mempool)
            BlockchainUtils.addTransaction(trans);

        List<Auction> auctions = client.getAuctions();
        for (Auction auction : auctions) {
            AuctionsState.addAuction(auction);

            List<Bid> bids = client.getBidsFromAuction(auction);
            for (Bid bid : bids)
                AuctionsState.updateBid(bid);
        }


        Client.setClientWallet(wallet);
        Runnable r = new Client();
        Thread clientThread = new Thread(r, "clientThread");
        clientThread.start();


        server.blockUntilShutdown();
    }

    public static void startAuctioneer(Wallet wallet, String myIpAddress, int grpcPort, String defaultBootstrapNodeIP, int defaultBootstrapNodePort) throws InterruptedException, java.io.IOException {
        byte[] randomNodeID = new byte[Utils.hashAlgorithmLengthInBytes];
        Random random = new SecureRandom();
        random.nextBytes(randomNodeID);

        System.out.println("Using nodeId: " + Utils.bytesToHexString(randomNodeID) + "\n");

        KademliaNode myNode = new KademliaNode(myIpAddress, grpcPort, randomNodeID);
        KBucketManager bucketManager = new KBucketManager(myNode);

        KademliaServer server = new KademliaServer(grpcPort, bucketManager);
        server.start();

        KademliaClient client = new KademliaClient(bucketManager);
        BlockchainUtils.setKademliaClient(client);

        KademliaNode bootstrapNode = new KademliaNode(defaultBootstrapNodeIP, defaultBootstrapNodePort, KademliaUtils.bootstrapNodeID);

        client.bootstrap(bootstrapNode);
        client.bootstrapBlockchain();


        List<Transaction> mempool = client.getMempool();
        for (Transaction trans : mempool)
            BlockchainUtils.addTransaction(trans);

        List<Auction> auctions = client.getAuctions();
        for (Auction auction : auctions) {
            AuctionsState.addAuction(auction);

            List<Bid> bids = client.getBidsFromAuction(auction);
            for (Bid bid : bids)
                AuctionsState.updateBid(bid);
        }


        AuctionManager newAuction = new  AuctionManager(wallet);

        server.blockUntilShutdown();
    }
}
