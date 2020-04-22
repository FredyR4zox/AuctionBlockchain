package com.ssd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BlockChain {
    public static ArrayList<Block> blockchain = new ArrayList<>();
    public static HashMap<String, Double> walletsMoney = new HashMap<String, Double>();
    private static final int difficulty = 4;
    private static int size;
    private static final Double minerReward = 100.0;

    public static int getDifficulty() {
        return difficulty;
    }
    public static int getSize() {
        return size;
    }
    public static double getMinerReward() {
        return BlockChain.minerReward;
    }
    public static Block getXBlock(int blocknr){
        return blockchain.get(blocknr-1);
    }
    public static String getLastHash(){
        return BlockChain.getXBlock(size).hash;
    }
    public static Boolean checkAddBlock(Block newBlock){
        //check if transactions in block are valid
        if(!newBlock.areSignaturesAndHashValid()) return false;
        if(!areFundsSufficient(newBlock)) return false;

        //Add block to blockchain and update Hashmap
        addBlock(newBlock);
        //Add minersReward to HashMap
        addMinerRewardToHashMap(newBlock.minersReward);
        for(int i= 0; i<=newBlock.nrTransactions; i++){
            transaction trans = newBlock.data[i];
            //Add transaction to HashMap
            updateHashMapValues(trans, BlockChain.walletsMoney);
        }

        return true;
    }
    public static void createGenesisBlock(Wallet creator){
        Block genesis= new Block("0");
        //Mine block
        genesis.mineBlock(creator);
        //Add minersReward to HashMap
        addMinerRewardToHashMap(genesis.minersReward);
        addBlock(genesis);
    }

    public static Boolean areFundsSufficient(Block block){
        HashMap<String, Double> fundsTracking = copyUsedHashMapValues(block);
        for(int i= 0; i<=block.nrTransactions; i++){
            transaction trans = block.data[i];
            //Check if he has money
            if(!checkIfEnoughFunds(trans, fundsTracking)) return false;
            updateHashMapValues(trans, fundsTracking);
        }
        return true;
    }

    private static HashMap<String, Double> copyUsedHashMapValues(Block block) {
        HashMap<String, Double> fundsTracking = new HashMap<String, Double>();
        for(int i= 0; i<=block.nrTransactions; i++) {
            transaction trans = block.data[i];
            if(walletsMoney.containsKey(trans.buyerID)&&!fundsTracking.containsKey(trans.buyerID)){
                fundsTracking.put(trans.buyerID, walletsMoney.get(trans.buyerID));
            }
            if(walletsMoney.containsKey(trans.sellerID)&&!fundsTracking.containsKey(trans.sellerID)){
                fundsTracking.put(trans.sellerID,walletsMoney.get(trans.sellerID));
            }
        }
        return fundsTracking;
    }

    public static Boolean checkIfEnoughFunds(transaction trans, HashMap <String, Double> walletsMoney) {
        if(!walletsMoney.containsKey(trans.buyerID)){
            System.out.println("This buyer has never received funds");
            return false;
        }
        Double buyerFunds=walletsMoney.get(trans.buyerID);
        if(buyerFunds-trans.amount<0) {
            System.out.println("Buyer doesn't have enough funds");
            return false;
        }
        else return true;
    }

    public static void updateHashMapValues(transaction t, HashMap <String, Double> walletsMoney) {
        if(walletsMoney.putIfAbsent(t.sellerID, t.amount)!=null) {
            double sellerNewValue = walletsMoney.get(t.sellerID) + t.amount;
            walletsMoney.replace(t.sellerID, sellerNewValue);
        }
        double buyerNewValue = walletsMoney.get(t.buyerID)-t.amount;
        if (buyerNewValue==0) {
            walletsMoney.remove(t.buyerID);
        }
        else{
            walletsMoney.replace(t.buyerID, buyerNewValue);
        }
    }

    public static Boolean isChainValidAndCreateHashMap(){
        //clear HashMap
        walletsMoney.clear();

        Block currentBlock = null;
        Block previousBlock;

        //loop through blockchain to check hashes:
        for(Block block: blockchain) {
            previousBlock=currentBlock;
            currentBlock=block;
            //Do Hashes check
            if (previousBlock==null){
                if(!currentBlock.isHashValid()) return false;
            }
            else{
                if (!areHashesValid(currentBlock, previousBlock)) return false;
            }
            //Add minersReward to HashMap
            addMinerRewardToHashMap(block.minersReward);
            //make transactions checks
            //Do Hash and signature check
            if(!block.areSignaturesAndHashValid()) return false;
            //Check if they have money
            if(!areFundsSufficient(block)) return false;
            for(int i= 0; i<=block.nrTransactions; i++){
                transaction trans = block.data[i];
                //Add transaction to HashMap
                updateHashMapValues(trans, BlockChain.walletsMoney);
            }
        }
        return true;
    }

    public static void addMinerRewardToHashMap(transaction minersReward){
        walletsMoney.put(minersReward.sellerID, minersReward.amount);
    }

    public static Boolean areHashesValid(Block currentBlock, Block previousBlock){
        //compare registered hash and calculated hash:
        if(!currentBlock.isHashValid()){
            System.out.println("Current Hashes not equal");
            return false;
        }
        //compare previous hash and registered previous hash
        if(!previousBlock.hash.equals(currentBlock.previousHash) ) {
            System.out.println("Previous Hashes not equal");
            return false;
        }
        return true;
    }

    public static void addBlock(Block newBlock){
        blockchain.add(newBlock);
        size ++;
    }

    public static void printHashMap(){
        for (Map.Entry m:walletsMoney.entrySet()) {
            System.out.println(m.getKey()+" "+m.getValue());
        }
    }

    public static String makeJson(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(blockchain);
    }
    public static BlockChain makeFromJson(String blockChainJson){
        return new Gson().fromJson(blockChainJson, BlockChain.class);
    }
}
