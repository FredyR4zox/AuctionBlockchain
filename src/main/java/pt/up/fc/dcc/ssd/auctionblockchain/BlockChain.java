package pt.up.fc.dcc.ssd.auctionblockchain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

public class BlockChain {
    private static final Logger logger = Logger.getLogger(BlockChain.class.getName());

    public static ArrayList<Block> blockchain = new ArrayList<>();
    public static HashMap<String, Long> walletsMoney = new HashMap<>();
    public static HashMap<String, Block> blocksHashes = new HashMap<>();
    private static int size;

    public static int getSize() {
        return size;
    }
    public static long getMinerReward() {
        return BlockchainUtils.minerReward;
    }
    public static Block getXBlock(int blocknr){
        return blockchain.get(blocknr-1);
    }
    public static String getLastHash(){
        return BlockChain.getXBlock(size).getHash();
    }

    public static Block getBlockWithHash(String hash){
        return blocksHashes.get(hash);
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
        HashSet<String> usedIDs = new HashSet<>();
        for(int i= 0; i<block.getNrTransactions(); i++){
            Transaction trans = block.getXData(i);
            if(usedIDs.contains(trans.getBuyerID())) {
                logger.warning("Buyer: " + trans.getBuyerID() + "appears twice in the same block");
                return false;
            }
            //Check if he has money
            if(!checkIfEnoughFunds(trans, BlockChain.walletsMoney)) return false;
            usedIDs.add(trans.getBuyerID());
        }
        return true;
    }

    public static Boolean checkIfEnoughFunds(Transaction trans, HashMap <String, Long> walletsMoney) {
        if(!walletsMoney.containsKey(trans.getBuyerID())){
            logger.warning("Buyer" + trans.getBuyerID()+ " has never received funds");
            return false;
        }
        Long buyerFunds=walletsMoney.get(trans.getBuyerID());
        if(buyerFunds-trans.getAmount()<0) {
            logger.warning("Buyer " + trans.getBuyerID() + " doesn't have enough funds");
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
        blocksHashes.clear();

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
            //make transactions checks
            //Do Hash and signature check
            if(!block.areSignaturesAndHashValid()) return false;
            //Check if they have money and for duplicated buyerIDs
            if(!areFundsSufficient(block)) return false;
            //add transaction block hashes list
            blocksHashes.put(block.getHash(), block);
            for(int i= 0; i<block.getNrTransactions(); i++){
                Transaction trans = block.getXData(i);
                //Add Transaction to HashMap
                updateHashMapValues(trans, BlockChain.walletsMoney);
            }
            //Add minersReward to HashMap
            BlockChain.addMinerRewardToHashMap(block.getMinersReward());
        }
        logger.info("Chain was validated and the hashmap with transactions has been created\n");
        return true;
    }

    public static void addMinerRewardToHashMap(Transaction minersReward){
            //in case its a coinbase transfer
                if(BlockChain.walletsMoney.putIfAbsent(minersReward.getSellerID(), minersReward.getAmount())!= null) {
                    Long minerNewValue = BlockChain.walletsMoney.get(minersReward.getSellerID()) + minersReward.getAmount();
                    BlockChain.walletsMoney.replace(minersReward.getSellerID(), minerNewValue);
                }
    }

    public static Boolean areHashesValid(Block currentBlock, Block previousBlock){
        //compare registered hash and calculated hash:
        if(!currentBlock.isHashValid()){
            logger.warning("Current Hashes not equal");
            return false;
        }
        //compare previous hash and registered previous hash
        if(!previousBlock.getHash().equals(currentBlock.getPreviousHash()) ) {
            logger.warning("Previous Hashes not equal");
            return false;
        }
        return true;
    }

    public static void addBlock(Block newBlock){
        blockchain.add(newBlock);
        size ++;
        //add the block to block hashes
        blocksHashes.put(newBlock.getHash(), newBlock);
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
        String logging = "HashMap Values:\n";
        for (Map.Entry<String, Long> entry : walletsMoney.entrySet()) {
            String key = entry.getKey();
            Long value = entry.getValue();
            logging = logging.concat(key + " " + value + "\n");
        }
        logger.info(logging);
    }

    public static String makeJson(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(blockchain);
    }
    public static BlockChain makeFromJson(String blockChainJson){
        return new Gson().fromJson(blockChainJson, BlockChain.class);
    }
}
