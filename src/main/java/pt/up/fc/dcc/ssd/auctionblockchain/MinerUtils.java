package pt.up.fc.dcc.ssd.auctionblockchain;

import com.sun.source.tree.Tree;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.logging.Logger;

public class MinerUtils implements Runnable {
    private static final Logger logger = Logger.getLogger(MinerUtils.class.getName());
    //class to manage adding blocks and checking transactions by miners
    private TreeSet<Transaction> transPool;
    private Wallet minerWallet;
    private String lastHash;
    private Block newBlock;
    private Boolean isMining;

    public MinerUtils(Wallet minerWallet){
        this.minerWallet = minerWallet;
        this.transPool = new TreeSet<>(new transactionCompare());
        this.lastHash = BlockChain.getLastHash();
        this.isMining = false;
    }

    public MinerUtils() {

    }

    private void createBlock(){
        isMining = true;
        HashSet<String> usedIDs = new HashSet<>();
        newBlock =  new Block(BlockChain.getLastHash(), BlockChain.getSize()+1);
        Iterator<Transaction> transIterator = this.transPool.iterator();
//        if (!transIterator.hasNext()) {
//            logger.warning("No transactions in transaction Pool");
//            return;
//        }
        for(int i=0; i< BlockchainUtils.MAX_NR_TRANSACTIONS && transIterator.hasNext(); i++){
            Transaction trans = transIterator.next();
            //a transaction cant have the same buyerID
            if(usedIDs.contains(trans.getBuyerID())) {
                i--;
                continue;
            }
            //if transaction cant be added it is ignored
            if(!newBlock.addTransactionIfValid(trans)) i--;
            usedIDs.add(trans.getBuyerID());
        }
        checkMineAddBlock(newBlock);
    }

    public void checkBlockChainUpdates(){
        String lastHash = BlockChain.getLastHash();
        if(this.lastHash.equals(lastHash)) {
            return;
        }
        logger.info("Blockchain is not up to date");
        Block block = BlockChain.getBlockWithHash(lastHash);
        while(!this.lastHash.equals(block.getHash())){
            MinerUtils.removeTransactionsFromTransPool(block, this.transPool);
            block = BlockChain.getBlockWithHash(block.getPreviousHash());
        }
        this.lastHash = lastHash;
    }

    public Boolean addTransactionIfValidToPool(Transaction trans) {
        Boolean output = BlockchainUtils.addTransactionIfValidToPool(trans, this.transPool, logger);
        checkBlockChainUpdates();
        if (this.transPool.size()>= BlockchainUtils.MIN_NR_TRANSACTIONS && !isMining){
            this.createBlock();
        }
        return output;
    }

    public static void removeTransactionsFromTransPool(Block block, TreeSet<Transaction> transPool) {
        Transaction[] data = block.getData();
        for (int i =0; i < block.getNrTransactions(); i++){
            Transaction trans = data[i];
            transPool.remove(trans);
        }
    }



    public void checkMineAddBlock(Block newBlock){
        //check if transactions in block are valid
        if(!newBlock.areSignaturesAndHashValid()){
            this.isMining=false;
            return;
        }
        //check block hash
        if(!newBlock.isHashValid()){
            this.isMining=false;
            return;
        }
        //Mine block
        //Add block to blockchain and update Hashmap
        Thread thread = new Thread(this);
        thread.start();
        return;
    }

    @Override
    public void run() {
        logger.info("Trying to mine a block\n");
        if (newBlock.mineBlock(minerWallet)){
            BlockChain.addBlock(newBlock);
            logger.info("Added block: " + newBlock.getHash() + " to blockchain\n");
            removeTransactionsFromTransPool(newBlock, this.getTransPool());
            this.setLastHash(newBlock.getHash());
            this.isMining=false;
        }
        this.isMining=false;
    }

    static class transactionCompare implements Comparator<Transaction>{

        @Override
        public int compare(Transaction transaction, Transaction t1) {
            //check bigger transaction fee
            int result = Long.compare(t1.getTransactionFee(), transaction.getTransactionFee());
            if (result == 0){
                //check timestamp
                return Long.compare(transaction.getTimeStamp(), t1.getTimeStamp());
            }
            else return result;
        }
    }

    public static Logger getLogger() {
        return logger;
    }

    public TreeSet<Transaction> getTransPool() {
        return transPool;
    }

    public Wallet getMinerWallet() {
        return minerWallet;
    }

    public String getLastHash() {
        return lastHash;
    }

    public void setLastHash(String lastHash) {
        this.lastHash = lastHash;
    }
}
