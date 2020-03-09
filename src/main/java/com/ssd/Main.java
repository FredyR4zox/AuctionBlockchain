package com.ssd;

public class Main {


    public static void main(String[] args) {
        //addXBlock our blocks to the BlockChain ArrayList:
        Wallet wallet1 = new Wallet();
        Wallet wallet2 = new Wallet();
        Wallet miner = new Wallet();
        transaction trans1 = new transaction(wallet1.getPubKey(), wallet2.getPubKey(), 5,0,0);
        trans1.signTransaction(wallet1.getPrivKey());
        boolean output = trans1.verifySignature();
        System.out.println("Is trans valid: "+ output);
        transaction trans12 = new transaction(wallet2.getPubKey(),wallet1.getPubKey(), 7,0,10);
        trans12.signTransaction(wallet2.getPrivKey());
        Block b1 = new Block(trans1, "0");
        b1.addTransaction(trans12);
        BlockChain.addBlock(b1);
        System.out.println("Trying to Mine block 1... ");
        BlockChain.getXBlock(0).mineBlock(BlockChain.getDifficulty(), miner);

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
