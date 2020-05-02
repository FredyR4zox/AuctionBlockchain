package pt.up.fc.dcc.ssd.auctionblockchain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;

public class BlockChain {
    public static ArrayList<Block> blockchain = new ArrayList<>();
    public static HashMap<String, Long> walletsMoney = new HashMap<>();
    private static int size;

    public static int getSize() {
        return size;
    }
    public static double getMinerReward() {
        return BlockchainUtils.minerReward;
    }
    public static Block getXBlock(int blocknr){
        return blockchain.get(blocknr-1);
    }
    public static String getLastHash(){
        return BlockChain.getXBlock(size).getHash();
    }

    public static Boolean checkAddBlock(Block newBlock){
        //check if transactions in block are valid
        if(!newBlock.areSignaturesAndHashValid()) return false;
        if(!areFundsSufficient(newBlock)) return false;
        //check if miner reward with Transaction fees are valid
        if(newBlock.getTransactionFeesTotal() == BlockChain.getMinerReward() - newBlock.getMinersReward().getAmount())return false;
        //Add block to blockchain and update Hashmap
        addBlock(newBlock);
        //Add minersReward to HashMap
        addMinerRewardToHashMap(newBlock.getMinersReward());
        for(int i= 0; i<=newBlock.getNrTransactions(); i++){
            Transaction trans = newBlock.getXData(i);
            //Add Transaction to HashMap
            updateHashMapValues(trans, BlockChain.walletsMoney);
        }

        return true;
    }

    public static void createGenesisBlock(Wallet creator){
        Block genesis= new Block("0");
        //Mine block
        genesis.mineBlock(creator);
        addBlock(genesis);
    }

    public static Boolean areFundsSufficient(Block block){
        HashMap<String, Long> fundsTracking = copyUsedHashMapValues(block);
        for(int i= 0; i<block.getNrTransactions(); i++){
            Transaction trans = block.getXData(i);
            //Check if he has money
            if(!checkIfEnoughFunds(trans, fundsTracking)) return false;
            updateHashMapValues(trans, fundsTracking);
        }
        return true;
    }

    private static HashMap<String, Long> copyUsedHashMapValues(Block block) {
        HashMap<String, Long> fundsTracking = new HashMap<>();

        for(int i=0; i<block.getNrTransactions(); i++) {
            Transaction trans = block.getXData(i);
            if(walletsMoney.containsKey(trans.getBuyerID()) && !fundsTracking.containsKey(trans.getBuyerID())){
                fundsTracking.put(trans.getBuyerID(), walletsMoney.get(trans.getBuyerID()));
            }
            if(walletsMoney.containsKey(trans.getSellerID())&&!fundsTracking.containsKey(trans.getSellerID())){
                fundsTracking.put(trans.getSellerID(),walletsMoney.get(trans.getSellerID()));
            }
        }
        return fundsTracking;
    }

    public static Boolean checkIfEnoughFunds(Transaction trans, HashMap <String, Long> walletsMoney) {
        if(!walletsMoney.containsKey(trans.getBuyerID())){
            System.out.println("This buyer has never received funds");
            return false;
        }
        Long buyerFunds=walletsMoney.get(trans.getBuyerID());
        if(buyerFunds-trans.getAmount()<0) {
            System.out.println("Buyer doesn't have enough funds");
            return false;
        }
        else return true;
    }

    public static void updateHashMapValues(Transaction t, HashMap <String, Long> walletsMoney) {

        if(walletsMoney.putIfAbsent(t.getSellerID(), t.getAmount()- t.getTransactionFee()) != null) {
            long sellerNewValue = walletsMoney.get(t.getSellerID()) + t.getAmount() - t.getTransactionFee();
            walletsMoney.replace(t.getSellerID(), sellerNewValue);
        }
        long buyerNewValue = walletsMoney.get(t.getBuyerID())-t.getAmount();
        if (buyerNewValue==0) {
            walletsMoney.remove(t.getBuyerID());
        }
        else{
            walletsMoney.replace(t.getBuyerID(), buyerNewValue);
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
            //check if miner reward with Transaction fees are valid
            if(!block.areTransactionFeesValid()) return false;
            //Add minersReward to HashMap
            BlockChain.addMinerRewardToHashMap(block.getMinersReward());
            //make transactions checks
            //Do Hash and signature check
            if(!block.areSignaturesAndHashValid()) return false;
            //Check if they have money
            if(!areFundsSufficient(block)) return false;
            for(int i= 0; i<block.getNrTransactions(); i++){
                Transaction trans = block.getXData(i);
                //Add Transaction to HashMap
                updateHashMapValues(trans, BlockChain.walletsMoney);
            }
        }
        return true;
    }

    public static void addMinerRewardToHashMap(Transaction minersReward){
            //in case its a coinbase transfer
                if(BlockChain.walletsMoney.putIfAbsent(minersReward.getSellerID(), minersReward.getAmount())!= null) {
                    Long minerNewValue = BlockChain.walletsMoney.get(minersReward.getSellerID()) + minersReward.getAmount();
                    BlockChain.walletsMoney.replace(minersReward.getSellerID(), minerNewValue);
                }
                return;
            }

    public static Boolean areHashesValid(Block currentBlock, Block previousBlock){
        //compare registered hash and calculated hash:
        if(!currentBlock.isHashValid()){
            System.out.println("Current Hashes not equal");
            return false;
        }
        //compare previous hash and registered previous hash
        if(!previousBlock.getHash().equals(currentBlock.getPreviousHash()) ) {
            System.out.println("Previous Hashes not equal");
            return false;
        }
        return true;
    }

    public static void addBlock(Block newBlock){
        blockchain.add(newBlock);
        size ++;
        //Add minersReward to HashMap
        BlockChain.addMinerRewardToHashMap(newBlock.getMinersReward());
        //updates hashmap
        for(int i= 0; i<newBlock.getNrTransactions(); i++){
            Transaction trans = newBlock.getXData(i);
            //Add Transaction to HashMap
            BlockChain.updateHashMapValues(trans, BlockChain.walletsMoney);
        }
    }

    public static void printHashMap(){
        walletsMoney.forEach((key, value) -> System.out.println(key + " " + value));
    }

    public static String makeJson(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(blockchain);
    }
    public static BlockChain makeFromJson(String blockChainJson){
        return new Gson().fromJson(blockChainJson, BlockChain.class);
    }
}
