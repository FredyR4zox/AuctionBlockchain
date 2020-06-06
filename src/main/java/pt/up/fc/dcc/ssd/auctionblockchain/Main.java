package pt.up.fc.dcc.ssd.auctionblockchain;

import pt.up.fc.dcc.ssd.auctionblockchain.Auction.Auction;
import pt.up.fc.dcc.ssd.auctionblockchain.Auction.AuctionsState;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.BlockchainUtils;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.MinerUtils;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.Transaction;
import pt.up.fc.dcc.ssd.auctionblockchain.Client.Bid;
import pt.up.fc.dcc.ssd.auctionblockchain.Kademlia.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;
import java.util.List;
import java.util.Random;


public class Main {
    public static void main(String[] args) throws InterruptedException, java.io.IOException {
        Security.addProvider(new BouncyCastleProvider());

        String myIpAddress = KademliaUtils.getMyIpAddress();
        if(myIpAddress == null){
            System.out.println("Error getting IP address");
            return;
        }
        System.out.println("My IP address is " + myIpAddress);

        byte[] randomNodeID = new byte[Utils.hashAlgorithmLengthInBytes];
        Random random = new SecureRandom();
        random.nextBytes(randomNodeID);

        int port = random.nextInt(65535-1001) + 1001; // random port

        KademliaNode myNode = new KademliaNode(myIpAddress, port, randomNodeID);

        KBucketManager bucketManager = new KBucketManager(myNode);


        KademliaServer server = new KademliaServer(port, bucketManager);
        server.start();


        Wallet alice = Wallet.createWalletFromFile("alice");
        MinerUtils.startMining(alice);

        KademliaClient client = new KademliaClient(bucketManager);
        BlockchainUtils.setKademliaClient(client);

        random.nextBytes(randomNodeID);
        KademliaNode bootstrapNode = new KademliaNode(KademliaUtils.bootstrapNodeIP, KademliaUtils.bootstrapNodePort, KademliaUtils.bootstrapNodeID);

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

