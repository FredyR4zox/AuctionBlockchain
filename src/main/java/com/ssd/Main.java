package com.ssd;

public class Main {


    public static void main(String[] args) {
        //addXBlock our blocks to the BlockChain ArrayList:
        transaction trans1 = new transaction(1,2, 5);
        transaction trans12 = new transaction(1,2, 10);
        Block b1 = new Block(trans1, "0");
        b1.addTransaction(trans12);
        BlockChain.addBlock(b1);
        System.out.println("Trying to Mine block 1... ");
        BlockChain.getXBlock(0).mineBlock(BlockChain.getDifficulty());

        transaction trans2 = new transaction(1,2, 20);
        BlockChain.addBlock(new Block(BlockChain.getXBlock(BlockChain.getSize()-1).hash));
        System.out.println("Trying to Mine block 2... ");
        BlockChain.getXBlock(1).mineBlock(BlockChain.getDifficulty());

        transaction trans3 = new transaction(1,2, 2);
        BlockChain.addBlock(new Block(trans3,BlockChain.getXBlock(BlockChain.getSize()-1).hash));
        System.out.println("Trying to Mine block 3... ");

        BlockChain.getXBlock(2).mineBlock(BlockChain.getDifficulty());
        System.out.println("\nBlockchain is Valid: " + BlockChain.isChainValid());
        String transJson = trans1.makeJson();
        System.out.println(transJson);
        String blockJson = b1.makeJson();
        System.out.println(blockJson);

        //String BlockChainJson = BlockChain.makeJson();
        //System.out.println(BlockChainJson);
    }
}
