package pt.up.fc.dcc.ssd.auctionblockchain.Auction;

import pt.up.fc.dcc.ssd.auctionblockchain.Client.Bid;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.BlockChain;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.BlockchainUtils;
import pt.up.fc.dcc.ssd.auctionblockchain.Wallet;

import java.util.Date;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

public class AuctionManager implements Runnable{
    private static final Logger logger = Logger.getLogger(AuctionManager.class.getName());

    private Auction auction;
    private Bid winBid;
    private BlockChain curBlockChain;

    public AuctionManager(Auction auction, BlockChain curBlockChain) {
        this.auction = auction;
        this.curBlockChain = BlockchainUtils.getLongestChain();
    }
//    public static AuctionManager createAuction(Wallet seller){
//
//    }

    public Boolean updateBid(Bid bid){
        if(!this.checkBid(bid)){
            return false;
        }
        this.winBid = bid;
        //this.sendBidUpdates();
        return true;
    }

    public Boolean checkBid(Bid bid){
        if(!bid.verifyBid()){
            return false;
        }
        if(!this.verifyBidInAuction(bid)){
            return false;
        }
        if(!this.verifyBidInChain(bid)){
            return false;
        }
        return true;
    }

    private boolean verifyBidInChain(Bid bid) {
        return curBlockChain.checkIfEnoughFunds(bid.getBuyerID(), bid.getAmount());
    }

    private boolean verifyBidInAuction(Bid bid) {
        //check if bid corresponds to this auction
        Boolean output = bid.getItemId().equals(auction.getItemID());
        output &= bid.getSellerID().equals(auction.getSellerID());
        //check if its first bid
        if(this.winBid==null){
            return output;
        }
        //check if minimum increment was followed
        output &= bid.getAmount()-winBid.getAmount() <= auction.getMinIncrement();
        output &= verifyFee(bid.getAmount(), bid.getFee(), auction.getFee());
        return output;
    }

    private Boolean verifyFee(long amount, long fee, long minFee) {
        return true;
    }

    @Override
    public void run() {
        while(this.winBid==null){
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Bid tempBid=this.winBid;
        long timestamp = new Date().getTime();
        while((new Date().getTime())-timestamp<this.auction.getTimeout()){
            if (this.winBid!=tempBid){
                tempBid=this.winBid;
                timestamp = new Date().getTime();
            }else{
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
