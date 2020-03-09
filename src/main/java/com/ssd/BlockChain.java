package com.ssd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;

public class BlockChain {
    public static ArrayList<Block> blockchain = new ArrayList<>();
    private static int difficulty = 7;
    private static int size;
    private static long minerReward;

    public static int getDifficulty() {
        return difficulty;
    }
    public static int getSize() {
        return size;
    }

    public static void addBlock(Block newBlock){
        blockchain.add(newBlock);
        size ++;
    }

    public static Block getXBlock(int blocknr){
        return blockchain.get(blocknr);
    }

    public static String makeJson(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(blockchain);
    }

    public static BlockChain makeFromJson(String blockChainJson){
        return new Gson().fromJson(blockChainJson, BlockChain.class);
    }
    public static Boolean isChainValid(){
        Block currentBlock;
        Block previousBlock;

        //loop through blockchain to check hashes:
        for(int i=1; i < blockchain.size(); i++) {
            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i-1);
            //compare registered hash and calculated hash:
            if(!currentBlock.hash.equals(currentBlock.calculateHash()) ){
                System.out.println("Current Hashes not equal");
                return false;
            }
            //compare previous hash and registered previous hash
            if(!previousBlock.hash.equals(currentBlock.previousHash) ) {
                System.out.println("Previous Hashes not equal");
                return false;
            }
        }
        return true;
    }

    public static long getMinerReward() {
        return BlockChain.minerReward;
    }
}
