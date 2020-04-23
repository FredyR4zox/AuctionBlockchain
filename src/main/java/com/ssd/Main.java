package com.ssd;

public class Main {


    public static void main(String[] args) {
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
        Wallet alice = new Wallet();
        Wallet bob = new Wallet();

        transaction trans1 = new transaction(creator, wallet1.getAddress(), 60,0.1,0);
        ableToAdd = minerAddition.addTransactionIfValid(trans1);
        System.out.println("Able to add transaction: "+ ableToAdd);

        transaction trans12 = new transaction(wallet1,wallet2.getAddress(),10,0.1,10);
        for(int i=0; i<6;i++) {
            ableToAdd = minerAddition.addTransactionIfValid(trans12);
            System.out.println("Able to add transaction: " + ableToAdd);
        }

        System.out.println("Trying to add block 1... ");
        ableToAdd = minerAddition.checkMineAddBlock();
        System.out.println("Able to add block: " + ableToAdd);
        BlockChain.printHashMap();

        System.out.println();
        minerAddition.reset();

        transaction trans21 = new transaction(miner, alice.getAddress(), 60,0.2,5);
        ableToAdd = minerAddition.addTransactionIfValid(trans21);
        System.out.println("Able to add transaction: "+ ableToAdd);

        System.out.println("Trying to add block 2... ");
        ableToAdd = minerAddition.checkMineAddBlock();
        System.out.println("Able to add block: " + ableToAdd);
        BlockChain.printHashMap();

        System.out.println();
        Boolean output=BlockChain.isChainValidAndCreateHashMap();
        System.out.println("Is Chain Valid:" + output);
        BlockChain.printHashMap();

        //String BlockChainJson = BlockChain.makeJson();
        //System.out.println(BlockChainJson);
    }
}
