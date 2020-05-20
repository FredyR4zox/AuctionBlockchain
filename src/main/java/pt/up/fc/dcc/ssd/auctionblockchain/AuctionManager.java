package pt.up.fc.dcc.ssd.auctionblockchain;

import java.util.Comparator;
import java.util.Date;
import java.util.TreeSet;
import java.util.logging.Logger;

public class AuctionManager {
    private static final Logger logger = Logger.getLogger(AuctionManager.class.getName());

    Auction auction;
    private TreeSet<Transaction> bids;

    public AuctionManager(Auction auction) {
        this.auction = auction;
        this.bids= new TreeSet<>(new AuctionManager.bidsCompare());
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
            return BlockchainUtils.addTransactionIfValidToPool(trans, this.bids, logger);
    }

    public Transaction endBid(){
        long currentTime = new Date().getTime();
        if (currentTime<this.auction.getEndTimeStamp()){
            logger.warning("Biding time hasn't ended");
            return null;
        }
        return this.bids.last();
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
