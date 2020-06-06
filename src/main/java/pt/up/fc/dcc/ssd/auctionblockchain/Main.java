package pt.up.fc.dcc.ssd.auctionblockchain;

import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.Block;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.BlockchainUtils;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.MinerUtils;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.Transaction;
import pt.up.fc.dcc.ssd.auctionblockchain.Client.Bid;
import pt.up.fc.dcc.ssd.auctionblockchain.Kademlia.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.SecureRandom;
import java.security.Security;
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

        KademliaNode myNode = new KademliaNode(myIpAddress, KademliaUtils.bootstrapNodePort, KademliaUtils.bootstrapNodeID);
        KBucketManager manager = new KBucketManager(myNode);

        KademliaServer server = new KademliaServer(KademliaUtils.bootstrapNodePort, manager);
        server.start();

        Wallet creator = new Wallet();
        System.out.println("Creator address: " + creator.getAddress());
        System.out.println("Creator public key: " + creator.getPubKey().toString());
        System.out.println("Creator private key: " + creator.getPrivKey().toString());

        MinerUtils.startMining(creator);

        KademliaClient client = new KademliaClient(manager);
        BlockchainUtils.setKademliaClient(client);

        BlockchainUtils.createGenesisBlock(creator);



        byte[] rand = new byte[Utils.hashAlgorithmLengthInBytes];
        Random random = new SecureRandom();
        random.nextBytes(rand);

        Wallet alice = Wallet.createWalletFromFile("alice");
        Bid bid = new Bid(creator, new String(rand), alice.getAddress(), BlockchainUtils.minerReward/2, 5);
        Transaction transaction = new Transaction(alice, bid);

        BlockchainUtils.addTransaction(transaction);
        client.announceNewTransaction(transaction);


        random.nextBytes(rand);
        Wallet bob = Wallet.createWalletFromFile("bob");
        bid = new Bid(creator, new String(rand), bob.getAddress(), BlockchainUtils.minerReward/2, 5);
        transaction = new Transaction(bob, bid);

        BlockchainUtils.addTransaction(transaction);
        client.announceNewTransaction(transaction);




//        Thread.sleep(10000);

//        System.out.println(BlockchainUtils.getLongestChain().makeJson());

        server.blockUntilShutdown();
    }
}
