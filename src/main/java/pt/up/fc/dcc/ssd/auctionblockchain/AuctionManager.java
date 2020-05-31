package pt.up.fc.dcc.ssd.auctionblockchain;

import java.util.Comparator;
import java.util.Date;
import java.util.TreeSet;
import java.util.logging.Logger;

public class AuctionManager {
    private static final Logger logger = Logger.getLogger(AuctionManager.class.getName());

    private Auction auction;
    private TreeSet<Transaction> bids;
    private BlockChain curBlockChain;

    public AuctionManager(Auction auction, BlockChain curBlockChain) {
        this.auction = auction;
        this.bids= new TreeSet<>(new AuctionManager.bidsCompare());
        this.curBlockChain = BlockchainUtils.getLongestChain();
    }

    public Boolean addBidIfValid(Transaction trans){
        if(!trans.getSellerID().equals(this.auction.getSellerID()) && trans.getItemID()!=this.auction.getItemID()){
            return false;
        }
        //check if auction has ended
        else if(trans.getTimeStamp()>this.auction.getEndTimeStamp()){
            return false;
        }
        else
            return this.addTransactionIfValidToPool(trans, this.bids, curBlockChain, logger);
    }

    public Transaction endBid(){
        long currentTime = new Date().getTime();
        if (currentTime<this.auction.getEndTimeStamp()){
            logger.warning("Biding time hasn't ended");
            return null;
        }
        return this.bids.last();
    }

    public Boolean addTransactionIfValidToPool(Transaction trans, TreeSet<Transaction> transPool, BlockChain blockchain ,Logger logger){
        //Do hashes check && Do signature checks
        if(!trans.verifyTransaction()){
            logger.warning("There was an attempt to add an invalid transaction to pool");
            return false;
        }
        if(!blockchain.checkIfEnoughFunds(trans.getBuyerID(), trans.getAmount())) {
            logger.warning("There was an attempt to add a transaction with insufficient funds to pool");
            return false;
        }
        if(!transPool.add(trans)){
            logger.warning("Transaction already exists in transaction pool");
            return false;
        }
        return true;
    }

    static class bidsCompare implements Comparator<Transaction> {

        @Override
        public int compare(Transaction transaction, Transaction t1) {
            //check bigger transaction fee
            int result = Long.compare(t1.getAmount(), transaction.getAmount());
            if (result == 0){
                //check timestamp
                return Long.compare(t1.getTimeStamp(), transaction.getTimeStamp());
            }
            else return result;
        }
    }
}
