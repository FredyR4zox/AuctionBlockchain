package pt.up.fc.dcc.ssd.auctionblockchain;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static java.lang.Math.abs;

public class Main {


    public static void main(String[] args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        byte[] rand = new byte[KademliaUtils.idSizeInBytes];
        for(int i = 0; i < KademliaUtils.idSizeInBytes; i++)
            rand[i] = 0;
        rand[KademliaUtils.idSizeInBytes-1] = 1;

        KademliaNode myNode = new KademliaNode("127.0.0.1", 8080, rand);
        KBucketManager manager = new KBucketManager(myNode);

        KademliaServer server = new KademliaServer(8080, manager);
        server.start();

        Random random = new SecureRandom();
        rand = new byte[KademliaUtils.idSizeInBytes];
        random.nextBytes(rand);

        KademliaNode node = new KademliaNode("127.0.0.1", 8081, rand);
        manager.insertNode(node);

        KBucketManager manager2 = new KBucketManager(new KademliaNode(node));
        manager2.insertNode(new KademliaNode(myNode));
        KademliaServer server2 = new KademliaServer(8081, manager2);
        server2.start();

        rand = new byte[KademliaUtils.idSizeInBytes];
        random.nextBytes(rand);
        KademliaNode node3 = new KademliaNode("127.0.0.1", 8081, rand);
        manager2.insertNode(node3);

        Block block = new Block("12345", "12345", new Transaction("SellerID", 50), new Transaction[0], 5, 5, 5);

        System.out.println(KademliaClient.findNode(manager, "12345".getBytes()));
    }
}

