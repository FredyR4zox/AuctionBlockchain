package pt.up.fc.dcc.ssd.auctionblockchain;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static final int port = 8080;

    public static void main(String[] args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        byte[] rand = new byte[KademliaUtils.idSizeInBytes];
        Random random = new SecureRandom();
        random.nextBytes(rand);

        KademliaNode myNode = new KademliaNode(rand);

        KBucketManager bucketManager = new KBucketManager(myNode);

        MinerUtils minerUtils = new MinerUtils();
        KademliaServer server;

        try {
            server = new KademliaServer(port, bucketManager, minerUtils);
            server.start();
        } catch (IOException e){
            logger.log(Level.SEVERE, "Error. Could not initialize the Kademlia server on port " + port);
            return;
        }

        KademliaNode bootstrapNode = new KademliaNode(KademliaUtils.bootstrapNodeIP, KademliaUtils.bootstrapNodePort, new byte[KademliaUtils.idSizeInBytes]);
        KademliaClient.bootstrap(bucketManager, bootstrapNode);

        List<Block> blocks;
        do {
            random.nextBytes(rand);
            blocks = KademliaClient.findValue(bucketManager, rand, BlockChain.getLastHash().getBytes());
            for (Block block : blocks) {
                if (BlockChain.getBlockWithHash(block.getHash()) == null) {
                    BlockChain.addBlock(block);
                }
            }
        } while (!blocks.isEmpty());

        server.blockUntilShutdown();
    }
}

