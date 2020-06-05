package pt.up.fc.dcc.ssd.auctionblockchain;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import pt.up.fc.dcc.ssd.auctionblockchain.Auction.Auction;
import pt.up.fc.dcc.ssd.auctionblockchain.Auction.AuctionsState;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.BlockchainUtils;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.Transaction;
import pt.up.fc.dcc.ssd.auctionblockchain.Client.Bid;
import pt.up.fc.dcc.ssd.auctionblockchain.Kademlia.*;

import java.security.SecureRandom;
import java.security.Security;
import java.util.List;
import java.util.Random;


public class Main {
    public static void main(String[] args) throws InterruptedException, java.io.IOException {
        Security.addProvider(new BouncyCastleProvider());

        byte[] randomNodeID = new byte[Utils.hashAlgorithmLengthInBytes];
        Random random = new SecureRandom();
        random.nextBytes(randomNodeID);

        int port = random.nextInt(65535-1001) + 1001; // random port
        KademliaNode myNode = new KademliaNode("127.0.0.1", port, randomNodeID);

        KBucketManager bucketManager = new KBucketManager(myNode);


        KademliaServer server = new KademliaServer(port, bucketManager);
        server.start();


        KademliaClient client = new KademliaClient(bucketManager);
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
