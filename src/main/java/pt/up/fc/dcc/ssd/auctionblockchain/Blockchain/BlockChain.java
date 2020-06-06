package pt.up.fc.dcc.ssd.auctionblockchain.Blockchain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import pt.up.fc.dcc.ssd.auctionblockchain.Client.Bid;
import pt.up.fc.dcc.ssd.auctionblockchain.Utils;
import pt.up.fc.dcc.ssd.auctionblockchain.Wallet;

import java.math.BigInteger;
import java.util.*;
import java.util.logging.Logger;

public class BlockChain{
    private static final Logger logger = Logger.getLogger(BlockChain.class.getName());

    private final List<Block> blockchain;
    private ArrayList<BlockChain> unconfirmedBlockchains;
    private HashMap<String, Long> walletsMoney;
    private HashMap<String, Block> blocksPreviousHashes;
    private String lastBlockHash;
    private TreeSet<Transaction> unconfirmedTransaction;
    private HashSet<String> confirmedTransactionHashes;
    private HashSet<String> registeredIDs;
    private int size;
    private Boolean mining;
    private BigInteger work;

    public BlockChain() {
        this.blockchain = new ArrayList<>();
        this.unconfirmedBlockchains = new ArrayList<>();
        this.walletsMoney = new HashMap<>();
        this.blocksPreviousHashes = new HashMap<>();
        this.unconfirmedTransaction = new TreeSet<>(new BlockchainUtils.transactionCompare());
        this.confirmedTransactionHashes = new HashSet<>();
        this.registeredIDs = new HashSet<>();
        size=0;
        this.mining = false;
        this.lastBlockHash= Utils.getStandardString();
        work= BigInteger.valueOf(0);
    }

    private Boolean splitChain(Block newBlock){
        //if not forked create the fork
        //if fork is in a previous block do a normal split
        //get index of block previous to duplicate
        int split= blockchain.indexOf(this.blocksPreviousHashes.get(newBlock.getPreviousHash())) - 1;
        BlockChain partialCur = this.chainClone(split);
        this.undoChanges(split);
        BlockChain newchain = this.chainClone(split);
        if(!newchain.checkAddBlock(newBlock)){
            logger.warning("block from split couldn't be confirmed");
            this.mergeChains(partialCur);
            return false;
        }
        unconfirmedBlockchains.add(partialCur);
        unconfirmedBlockchains.add(newchain);
        return true;
    }

    //create yet another split
     private Boolean furtherSplitChain(Block newBlock){
         BlockChain newchain = this.chainClone(this.size-1);
         if(!newchain.checkAddBlock(newBlock)){
             logger.warning("block from split couldn't be confirmed");
             return false;
         }
         unconfirmedBlockchains.add(newchain);
         return true;
     }

    private BlockChain chainClone(int split){
        //the chain clone clones everything except the chain
        //the chain starts now after the split
        BlockChain clone = new BlockChain();
        clone.work = this.work;
        clone.size = this.size;
        clone.lastBlockHash= this.lastBlockHash;
        clone.unconfirmedTransaction = (TreeSet<Transaction>) this.unconfirmedTransaction.clone();
        clone.walletsMoney = (HashMap<String, Long>) this.walletsMoney.clone();
        clone.blocksPreviousHashes = (HashMap<String, Block>) this.blocksPreviousHashes.clone();
        clone.unconfirmedTransaction = (TreeSet<Transaction>) this.unconfirmedTransaction.clone();
        clone.confirmedTransactionHashes = (HashSet<String>) this.confirmedTransactionHashes.clone();
        clone.registeredIDs = (HashSet<String>) this.registeredIDs.clone();
        if( split == clone.size-1) {
            return clone;
        } else{
            for(int i= split+1;i<this.blockchain.size(); i++){
                clone.blockchain.add(this.blockchain.get(i));
            }
            return clone;
        }
    }

    private void undoChanges(int split) {
        for(int i= this.blockchain.size()-1; i>split; i--){
            this.removeBlock(this.blockchain.get(i));
        }
    }

    private void mergeChains(BlockChain joiner){
        this.blockchain.addAll(joiner.getBlockchain());
        this.unconfirmedBlockchains = joiner.getUnconfirmedBlockChains();
        this.confirmedTransactionHashes = joiner.getConfirmedTransactionHashes();
        this.unconfirmedTransaction = joiner.getUnconfirmedTransaction();
        this.registeredIDs = joiner.getRegisteredIDs();
        this.walletsMoney= joiner.getWalletsMoney();
        this.blocksPreviousHashes = joiner.getBlocksPreviousHashes();
        this.lastBlockHash = joiner.getLastBlockHash();
        this.size = joiner.getSize();
        this.work=joiner.getWork();
    }

    public void tryResolveForks(){
        if(!this.isForked()){
            return;
        }
        BigInteger[] chainsWork = new BigInteger[unconfirmedBlockchains.size()];
        for(int i=0; i< unconfirmedBlockchains.size(); i++){
            chainsWork[i] = unconfirmedBlockchains.get(i).getBiggestChainWork();
        }
        //answer format is { largestA, posA, largestB, posB }
        BigInteger[] res = Utils.twoLargest(chainsWork);
        if((res[0].subtract(res[2])).compareTo(BlockchainUtils.WORK_RESOLVE_SPLIT)>=0){
            this.mergeChains(unconfirmedBlockchains.get(res[1].intValue()));
            this.tryResolveForks();
        }
    }

    //get current work or highest value of work in forks
    public BigInteger getBiggestChainWork(){
        if(!this.isForked()){
            return this.work;
        }
        BigInteger[] chainsWork = new BigInteger[unconfirmedBlockchains.size()];
        for(int i=0; i< unconfirmedBlockchains.size(); i++){
            chainsWork[i] = unconfirmedBlockchains.get(i).getBiggestChainWork();
        }
        return Utils.largest(chainsWork);
    }

    public BlockChain getLongestChain(){
        if(!this.isForked()){
            return this;
        }
        BlockChain[] chains = new BlockChain[unconfirmedBlockchains.size()];
        for(int i=0; i< unconfirmedBlockchains.size(); i++){
            chains[i] = unconfirmedBlockchains.get(i).getLongestChain();
        }
        return BlockchainUtils.largestChain(chains);
    }

    public Boolean addBlockToCorrectChain(Block newBlock) {
        //if there are chains to be merged do it
        this.tryResolveForks();
        //if its just a simple chain add it
        if (this.lastBlockHash.equals(newBlock.getPreviousHash()) && !this.isForked()) {
            return this.checkAddBlock(newBlock);
        }
        //if its the last hash and the chain is forked split this chain more
        else if (this.lastBlockHash.equals(newBlock.getPreviousHash()) && this.isForked()) {
            //check if next chain beginning is equal
            for (BlockChain unconfirmedBlockchain : unconfirmedBlockchains) {
                if(unconfirmedBlockchain.lastBlockHash.equals(newBlock.getHash())){
                    logger.info("Duplicated Block");
                    return false;
                }
            }
            return this.furtherSplitChain(newBlock);
        }
        //if this previous hash already exists fork chain
        else if (this.blocksPreviousHashes.get(newBlock.getPreviousHash()) != null) {
            //check if its duplicated
            if(this.blocksPreviousHashes.get(newBlock.getPreviousHash()).getHash().equals(newBlock.getHash())){
                logger.info("Duplicated Block");
                return false;
            }
            if(this.getWork().subtract(newBlock.getPreviousWork()).compareTo(BlockchainUtils.WORK_RESOLVE_SPLIT)>0){
                logger.info("Trying to add a block that is too far down the chain");
                return false;
            }
            return this.splitChain(newBlock);
        } else {
            boolean output = false;
            for (BlockChain unconfirmedBlockchain : unconfirmedBlockchains) {
                output = output || unconfirmedBlockchain.addBlockToCorrectChain(newBlock);
            }
            return output;
        }
    }

    private Boolean areFundsSufficient(Transaction trans, HashMap<String, Long> usedIDs){

        Long buyerAmount = usedIDs.get(trans.getBid().getBuyerID());
        if(buyerAmount==null) {
            if(!this.checkIfEnoughFunds(trans.getBid().getBuyerID(), trans.getBid().getAmount())){
                return false;
            }else {
                usedIDs.put(trans.getBid().getBuyerID(), trans.getBid().getAmount());
            }
        }
        else {
            buyerAmount += trans.getBid().getAmount();
            if (!this.checkIfEnoughFunds(trans.getBid().getBuyerID(), buyerAmount)) {
                return false;
            } else {
                usedIDs.replace(trans.getBid().getBuyerID(), buyerAmount);
            }
        }
        return true;
    }

    public Boolean checkIfEnoughFunds(String buyerID, long amount) {
        if(!walletsMoney.containsKey(buyerID)){
            logger.warning("Buyer " + buyerID + " has never received funds");
            return false;
        }
        Long buyerFunds=walletsMoney.get(buyerID);
        if(buyerFunds-amount<0) {
            logger.warning("Buyer " + buyerID + " doesn't have enough funds");
            return false;
        }
        else return true;
    }

    public Boolean checkAddBlock(Block newBlock){
        //make block checks
        if(!newBlock.checkBlock()) return false;
        //make block checks in relation to chain
        if(!checkBlockInChain(newBlock)) return false;
        //Add block to blockchain and update Hashmaps
        this.addBlock(newBlock);
        return true;
    }

    private Boolean checkBlockInChain(Block block){
        HashSet<String> tempRegisteredIDs = (HashSet<String>) this.registeredIDs.clone();
        HashSet<String> tempConfirmedTransactionHashes = (HashSet<String>) this.confirmedTransactionHashes.clone();
        HashMap<String, Long> accumulativeSpends = new HashMap<>();
        for(int i= 0; i<block.getNrTransactions(); i++) {
            Transaction trans = block.getXData(i);
            if(!this.areFundsSufficient(trans, accumulativeSpends)) return false;
            if(tempConfirmedTransactionHashes.contains(trans.getHash())){
                logger.warning("Detected duplicated transaction");
                return false;
            }
            if(tempRegisteredIDs.contains(trans.getBid().getItemId())){
                logger.warning("already usedID");
                return false;
            }
            tempRegisteredIDs.add(trans.getBid().getItemId());
            tempConfirmedTransactionHashes.add(trans.getHash());
        }
        return true;
    }

    public void addBlock(Block newBlock){
        if(this.mining){
            this.mining=false;
        }
        this.blockchain.add(newBlock);
        this.lastBlockHash=newBlock.getHash();
        this.size ++;
        this.work = this.work.add(newBlock.getBigDifficulty());
        //add the block to block hashes
        this.blocksPreviousHashes.put(newBlock.getPreviousHash(), newBlock);
        //Add minersReward to HashMap
        this.addMinerRewardToHashMap(newBlock.getMinersReward().getBid());
        //updates hashmap
        for(int i= 0; i<newBlock.getNrTransactions(); i++){
            Transaction trans = newBlock.getXData(i);
            this.unconfirmedTransaction.remove(trans);
            this.confirmedTransactionHashes.add(trans.getHash());
            //register the ID as used
            this.registeredIDs.add(trans.getBid().getItemId());
            //Add Transaction to HashMap
            this.updateHashMapValues(trans.getBid());
        }
    }

    private void updateHashMapValues(Bid b) {

        if(this.walletsMoney.putIfAbsent(b.getSellerID(), b.getAmount()- b.getFee()) != null) {
            long sellerNewValue = this.walletsMoney.get(b.getSellerID()) + b.getAmount() - b.getFee();
            this.walletsMoney.replace(b.getSellerID(), sellerNewValue);
        }
        long buyerNewValue = this.walletsMoney.get(b.getBuyerID())-b.getAmount();
        this.walletsMoney.replace(b.getBuyerID(), buyerNewValue);
    }

    private void addMinerRewardToHashMap(Bid minersReward){
        //in case its a coinbase transfer
        if(this.walletsMoney.putIfAbsent(minersReward.getSellerID(), minersReward.getAmount() + minersReward.getFee())!= null) {
            Long minerNewValue = this.walletsMoney.get(minersReward.getSellerID()) + minersReward.getAmount() + minersReward.getFee();
            this.walletsMoney.replace(minersReward.getSellerID(), minerNewValue);
        }
    }

    private void removeBlock(Block block) {
        this.blockchain.remove(block);
        this.lastBlockHash= block.getPreviousHash();
        this.size--;
        this.work = this.work.subtract(block.getBigDifficulty());
        this.blocksPreviousHashes.remove(block.getPreviousHash());
        this.removeMinerRewardFromHashMap(block.getMinersReward().getBid());
        for(int i= 0; i<block.getNrTransactions(); i++){
            Transaction trans = block.getXData(i);
            this.unconfirmedTransaction.add(trans);
            this.confirmedTransactionHashes.remove(trans.getHash());
            //remove the registeredID
            this.registeredIDs.remove(trans.getBid().getItemId());
            //Add Transaction to HashMap
            this.deUpdateHashMapValues(trans.getBid());
        }
    }

    private void deUpdateHashMapValues(Bid b) {
        long buyerNewValue = this.walletsMoney.get(b.getBuyerID())+b.getAmount();
        this.walletsMoney.replace(b.getBuyerID(), buyerNewValue);
    }


    private void removeMinerRewardFromHashMap(Bid minersReward) {
        Long minerNewValue = this.walletsMoney.get(minersReward.getSellerID()) - minersReward.getAmount();
        this.walletsMoney.replace(minersReward.getSellerID(), minerNewValue);
    }



    /*public Boolean isChainValidAndCreateHashMap(){
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
                updateHashMapValues(trans, this.walletsMoney);
            }
            //Add minersReward to HashMap
            this.addMinerRewardToHashMap(block.getMinersReward());
        }
        logger.info("Chain was validated and the hashmap with transactions has been created\n");
        return true;
    }*/

    private Boolean areHashesValid(Block currentBlock, Block previousBlock){
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

    public Block createBlock(Wallet minerWallet){
        HashMap<String, Long> accumulativeSpends = new HashMap<>();
        HashSet<String> tempRegisteredIDs = (HashSet<String>) this.registeredIDs.clone();
        Block newBlock =  new Block(this.getLastBlockHash(), this.work);
        //theoretically no duplicated transaction or used transactions ever reached this point
        Iterator<Transaction> transIterator = this.unconfirmedTransaction.iterator();
//        if (!transIterator.hasNext()) {
//            logger.warning("No transactions in transaction Pool");
//            return;
//        }
        List<Transaction> delTrans = new LinkedList<>();
        for(int i=0; i< BlockchainUtils.MAX_NR_TRANSACTIONS && transIterator.hasNext(); i++){
            Transaction trans = transIterator.next();
            //check if ID exists, if it does, delete the trans
            if(tempRegisteredIDs.contains(trans.getBid().getItemId())){
                logger.warning("message with already used ID");
                delTrans.add(trans);
                i--;
                continue;
            }
            //if transaction cant be verified remove it
            if(!trans.verifyTransaction()){
                delTrans.add(trans);
                i--;
                continue;
            }
            //check if wallet has enough funds having into account transactions already in the block
            if(!this.areFundsSufficient(trans, accumulativeSpends)){
                i--;
                continue;
            }
            //if transaction cant be added it is ignored
            newBlock.addTransaction(trans);
            tempRegisteredIDs.add(trans.getBid().getItemId());
        }
        //remove transactions with already used itemIDs
        //remove invalid transactions
        for(Transaction trans : delTrans){
            this.unconfirmedTransaction.remove(trans);
        }
        if(newBlock.getNrTransactions()==0) return null;
        long transactionFeesTotal = newBlock.getTransactionFeesTotal();
        newBlock.setMinersReward(new Transaction(minerWallet, transactionFeesTotal));
        Random random = new Random();
        newBlock.setNonce(random.nextInt());
        return newBlock;
    }

    public Boolean addTransactionToCorrectChains(Transaction trans){
        if(!this.isForked()){
            if(this.confirmedTransactionHashes.contains(trans.getHash())){
                return false;
            }
            this.unconfirmedTransaction.add(trans);
            return true;
        }else{
            boolean output = false;
            for (BlockChain unconfirmedBlockchain : this.unconfirmedBlockchains) {
                output= output || unconfirmedBlockchain.addTransactionToCorrectChains(trans);
            }
            return output;
        }
    }

    public void removeTransactionsFromTransPool(Block block) {
        Transaction[] data = block.getData();
        for (int i =0; i < block.getNrTransactions(); i++){
            Transaction trans = data[i];
            this.unconfirmedTransaction.remove(trans);
            this.confirmedTransactionHashes.add(trans.getHash());
        }
    }

    public void printHashMap(){
        String logging = "HashMap Values:\n";
        for (Map.Entry<String, Long> entry : walletsMoney.entrySet()) {
            String key = entry.getKey();
            Long value = entry.getValue();
            logging = logging.concat(key + " " + value + "\n");
        }
        logger.info(logging);
    }

    public String makeJson(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(blockchain);
    }
    public BlockChain makeFromJson(String blockChainJson){
        return new Gson().fromJson(blockChainJson, BlockChain.class);
    }

    public ArrayList<BlockChain> getUnconfirmedBlockChains() {
        return unconfirmedBlockchains;
    }

    public List<Block> getBlockchain() {
        return blockchain;
    }

    public String getLastBlockHash() {
        return lastBlockHash;
    }

    public HashMap<String, Block> getBlocksPreviousHashes() {
        return blocksPreviousHashes;
    }

    public TreeSet<Transaction> getUnconfirmedTransaction() {
        return unconfirmedTransaction;
    }

    public HashSet<String> getConfirmedTransactionHashes() {
        return confirmedTransactionHashes;
    }

    public HashMap<String, Long> getWalletsMoney() {
        return walletsMoney;
    }

    public int getSize() {
        return size;
    }
    public Block getXBlock(int blocknr){
        return blockchain.get(blocknr-1);
    }

    public BigInteger getWork(){
        return this.work;
    }

    public Boolean isForked(){
        return !unconfirmedBlockchains.isEmpty();
    }

    public Boolean isMining() {
        return mining;
    }

    public void setMining(Boolean mining) {
        this.mining = mining;
    }

    public HashSet<String> getRegisteredIDs() {
        return registeredIDs;
    }
}
