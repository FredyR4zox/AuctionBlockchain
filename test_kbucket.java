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

        byte[] rand = new byte[pt.up.fc.dcc.ssd.auctionblockchain.Utils.hashAlgorithmLengthInBytes];
        for(int i = 0; i < pt.up.fc.dcc.ssd.auctionblockchain.Utils.hashAlgorithmLengthInBytes; i++)
            rand[i] = 0;
        rand[pt.up.fc.dcc.ssd.auctionblockchain.Utils.hashAlgorithmLengthInBytes-1] = 1;

        KademliaNode myNode = new KademliaNode("127.0.0.1", 8080, rand);
        KBucketManager manager = new KBucketManager(myNode);

        KademliaServer server = new KademliaServer(8080, manager);
        server.start();

        KBucketManager managers[] = new KBucketManager[100];
//        managers[0] = manager;
        Random random = new SecureRandom();

        for(int i = 0; i < 100; i++){
            rand = new byte[pt.up.fc.dcc.ssd.auctionblockchain.Utils.hashAlgorithmLengthInBytes];
            random.nextBytes(rand);
            for(int j = 0; j < 10; j++)
                rand[j] = 0;
            for(int j = 0; j < 10; j++)
                rand[abs(random.nextInt()) % pt.up.fc.dcc.ssd.auctionblockchain.Utils.hashAlgorithmLengthInBytes] = 0;

            KademliaNode node = new KademliaNode("127.0.0.1", 8080+i+1, rand);

//            System.out.println("Node: ");
//            for(int j=0; j<KademliaUtils.idSizeInBytes; j++)
//                System.out.print(" " + rand[j]);
//            System.out.println();

            manager.insertNode(node);
            managers[i] = new KBucketManager(new KademliaNode("127.0.0.1", 8080+i+1, Arrays.copyOf(rand, pt.up.fc.dcc.ssd.auctionblockchain.Utils.hashAlgorithmLengthInBytes)));

            KademliaServer server2 = new KademliaServer(8080+i+1, managers[i]);
            server2.start();
        }

        KBucket[] buckets = manager.getBuckets();

        int count = 0;
        for(int j = 0; j < KademliaUtils.idSizeInBits; j++) {
            count += buckets[j].getNodes().size();
            System.out.println("Bucket " + j + ": " + buckets[j].getNodes().size());
        }
        System.out.println("Total: " + count);

        for(int i = 0; i < 100; i++){
            for(int j = 0; j < KademliaUtils.idSizeInBits; j++){
                List<KademliaNode> nodes = buckets[j].getNodes();
                for(KademliaNode node : nodes)
                    managers[i].insertNode(new KademliaNode("127.0.0.1", 8080, Arrays.copyOf(node.getNodeID(), pt.up.fc.dcc.ssd.auctionblockchain.Utils.hashAlgorithmLengthInBytes)));
            }
        }

        System.out.println("Exit");
    }
}

