package pt.up.fc.dcc.ssd.auctionblockchain;

import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.BlockchainUtils;
import pt.up.fc.dcc.ssd.auctionblockchain.Kademlia.KBucketManager;
import pt.up.fc.dcc.ssd.auctionblockchain.Kademlia.KademliaNode;
import pt.up.fc.dcc.ssd.auctionblockchain.Kademlia.KademliaServer;
import pt.up.fc.dcc.ssd.auctionblockchain.Kademlia.KademliaUtils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;


public class Main {
    public static void main(String[] args) throws InterruptedException, java.io.IOException {
        Security.addProvider(new BouncyCastleProvider());

        KademliaNode myNode = new KademliaNode(KademliaUtils.bootstrapNodeIP, KademliaUtils.bootstrapNodePort, KademliaUtils.bootstrapNodeID);
        KBucketManager manager = new KBucketManager(myNode);

        KademliaServer server = new KademliaServer(8080, manager);
        server.start();

        Wallet creator = new Wallet();
        System.out.println("Creator address: " + creator.getAddress());
        System.out.println("Creator public key: " + creator.getPubKey().toString());
        System.out.println("Creator private key: " + creator.getPrivKey().toString());

        BlockchainUtils.createGenesisBlock(creator);

        server.blockUntilShutdown();
    }
}
