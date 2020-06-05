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

        BlockchainUtils.createGenesisBlock(creator);

        server.blockUntilShutdown();
    }
}
