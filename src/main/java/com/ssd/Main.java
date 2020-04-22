package com.ssd;

public class Main {


    public static void main(String[] args) {
        //addXBlock our blocks to the BlockChain ArrayList:
        Boolean ableToAdd;

        Wallet creator= new Wallet();
        BlockChain.createGenesisBlock(creator);
        System.out.println("creator address:" + creator.getAddress());
        BlockChain.printHashMap();
        System.out.println();

        Wallet miner = new Wallet();
        addBlock minerAddition = new addBlock(miner);

        Wallet wallet1 = new Wallet();
        Wallet wallet2 = new Wallet();


        transaction trans1 = new transaction(creator.getAddress(), wallet1.getAddress(),creator.getPubKey(), 40,0,0);
        trans1.signTransaction(creator.getPrivKey());
        ableToAdd = minerAddition.addTransactionIfValid(trans1);
        System.out.println("Able to add transaction: "+ ableToAdd);

        transaction trans12 = new transaction(wallet1.getAddress(),wallet2.getAddress(),wallet1.getPubKey(), 30,0,10);
        trans12.signTransaction(wallet1.getPrivKey());
        ableToAdd = minerAddition.addTransactionIfValid(trans12);
        System.out.println("Able to add transaction: "+ ableToAdd);

        System.out.println("Trying to add block 1... ");
        ableToAdd = minerAddition.checkMineAddBlock();
        System.out.println("Able to add block: " + ableToAdd);
        BlockChain.printHashMap();

        Boolean output=BlockChain.isChainValidAndCreateHashMap();
        System.out.println("Is Chain Valid:" + output);
        BlockChain.printHashMap();
        //System.out.println(b1.makeJson());
    /*
        transaction trans2 = new transaction(1,2, 20);
        BlockChain.addBlock(new Block(BlockChain.getXBlock(BlockChain.getSize()-1).hash));
        System.out.println("Trying to Mine block 2... ");
        BlockChain.getXBlock(1).mineBlock(BlockChain.getDifficulty());

        transaction trans3 = new transaction(1,2, 2);
        BlockChain.addBlock(new Block(trans3,BlockChain.getXBlock(BlockChain.getSize()-1).hash));
        System.out.println("Trying to Mine block 3... ");

        BlockChain.getXBlock(2).mineBlock(BlockChain.getDifficulty());
        System.out.println("\nBlockchain is Valid: " + BlockChain.isChainValid());
        */

        //String BlockChainJson = BlockChain.makeJson();
        //System.out.println(BlockChainJson);
    }
}
