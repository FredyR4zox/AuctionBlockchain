package pt.up.fc.dcc.ssd.auctionblockchain;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.logging.Logger;

public class MinerUtils {
    private static final Logger logger = Logger.getLogger(MinerUtils.class.getName());
    //class to manage adding blocks and checking transactions by miners
    private TreeSet<Transaction> transPool;
    Wallet minerWallet;

    public MinerUtils(Wallet minerWallet){
        this.minerWallet = minerWallet;
        this.transPool = new TreeSet<>(new transactionCompare());
    }

    public Block createBlock(){

        HashSet<String> usedIDs = new HashSet<>();
        Block newBlock =  new Block(BlockChain.getLastHash());
        Iterator<Transaction> transIterator = this.transPool.iterator();
        if (!transIterator.hasNext()) {
            logger.warning("No transactions in transaction Pool");
            return null;
        }
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
        logger.info("Trying to mine a block\n");
        if (checkMineAddBlock(newBlock)){
            removeTransactionsFromTransPool(newBlock.getData(), newBlock.getNrTransactions());
            return newBlock;
        }
        else return null;

    }

    public Boolean addTransactionIfValidToPool(Transaction trans) {
        return BlockchainUtils.addTransactionIfValidToPool(trans, this.transPool, logger);
    }

    private void removeTransactionsFromTransPool(Transaction[] data, int nrTransactions) {
        for (int i =0; i < nrTransactions; i++){
            Transaction trans = data[i];
            this.transPool.remove(trans);
        }
    }



    public Boolean checkMineAddBlock(Block newBlock){
        //check if transactions in block are valid
        if(!newBlock.areSignaturesAndHashValid()) return false;
        //check block hash
        if(!newBlock.isHashValid()) return false;
        //Mine block
        newBlock.mineBlock(minerWallet);
        //Add block to blockchain and update Hashmap
        BlockChain.addBlock(newBlock);
        logger.info("Added block: " + newBlock.getHash() + " to blockchain\n");
        return true;
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
}
