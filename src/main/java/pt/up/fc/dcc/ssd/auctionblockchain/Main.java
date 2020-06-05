package pt.up.fc.dcc.ssd.auctionblockchain;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import pt.up.fc.dcc.ssd.auctionblockchain.Auction.AuctionManager;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.Block;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.BlockChain;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.BlockchainUtils;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.Transaction;
import pt.up.fc.dcc.ssd.auctionblockchain.Client.Client;
import pt.up.fc.dcc.ssd.auctionblockchain.Kademlia.*;

import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;
import java.util.Random;

import static java.lang.Thread.sleep;

public class Main {
    public static void main(String[] args) throws InterruptedException, java.io.IOException {
        Security.addProvider(new BouncyCastleProvider());

        KademliaNode myNode = new KademliaNode(KademliaUtils.bootstrapNodeIP, KademliaUtils.bootstrapNodePort, KademliaUtils.bootstrapNodeID);
        KBucketManager manager = new KBucketManager(myNode);

        KademliaServer server = new KademliaServer(8080, manager);
        server.start();

        byte[] previousHash = new byte[Utils.hashAlgorithmLengthInBytes];
        Arrays.fill(previousHash, (byte)0);

        Wallet creator = new Wallet();
        System.out.println("Creator address: " + creator.getAddress());
        System.out.println("Creator public key: " + creator.getPubKey().toString());
        System.out.println("Creator private key: " + creator.getPrivKey().toString());

        BlockchainUtils.createGenesisBlock(creator);

        server.blockUntilShutdown();
    }
}
