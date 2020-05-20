package pt.up.fc.dcc.ssd.auctionblockchain;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

public class Main {


    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());

        Boolean ableToAdd;
        Block blockAdded;

        Wallet creator= new Wallet();

        BlockChain.createGenesisBlock(creator);
        System.out.println("creator address:" + creator.getAddress());
        BlockChain.printHashMap();
        System.out.println();

        Wallet miner = new Wallet();
        MinerUtils minerAddition = new MinerUtils(miner);

        Wallet wallet1 = new Wallet();
        Wallet wallet2 = new Wallet();
        Wallet alice = new Wallet();
        Wallet bob = new Wallet();

        Transaction trans10 = new Transaction(creator, wallet1.getAddress(), 10, 2, 0);
        Transaction trans11 = new Transaction(creator, wallet2.getAddress(), 20, 1, 10);
        Transaction trans12 = new Transaction(creator, bob.getAddress(), 20, 1, 8);

        minerAddition.addTransactionIfValidToPool(trans12);
        minerAddition.addTransactionIfValidToPool(trans10);

        for(int i=0; i<1;i++) {
            ableToAdd = minerAddition.addTransactionIfValidToPool(trans11);
        }

        minerAddition.createBlock();

        BlockChain.printHashMap();

        Transaction trans21 = new Transaction(miner, alice.getAddress(), 60, 2, 5);
        ableToAdd = minerAddition.addTransactionIfValidToPool(trans21);
        System.out.println("Able to add Transaction: "+ ableToAdd);

        blockAdded = minerAddition.createBlock();
        if(blockAdded != null) {
            System.out.println("Able to add block: " + ableToAdd);
        }
        BlockChain.printHashMap();

        System.out.println();
        BlockChain.isChainValidAndCreateHashMap();
        BlockChain.printHashMap();

        //String BlockChainJson = BlockChain.makeJson();
        //System.out.println(BlockChainJson);

        Auction auction = new Auction(0, 60, creator);
        Boolean output= auction.verifyAuction();
        System.out.println(output);

    }
}

