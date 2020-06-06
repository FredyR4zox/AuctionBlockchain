package pt.up.fc.dcc.ssd.auctionblockchain.Auction;

import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.BlockchainUtils;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.Transaction;
import pt.up.fc.dcc.ssd.auctionblockchain.Client.Bid;
import pt.up.fc.dcc.ssd.auctionblockchain.Utils;
import pt.up.fc.dcc.ssd.auctionblockchain.Wallet;

import java.util.Date;
import java.util.TreeSet;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

public class AuctionManager implements Runnable{
    private static final Logger logger = Logger.getLogger(AuctionManager.class.getName());

    private Auction auction;
    Wallet seller;
    TreeSet<Bid> bids_status;
    Thread runningAuction;
    String lastHashUpdate;

    public AuctionManager(Auction auction) {
        this.auction = auction;
        this.bids_status= AuctionsState.getAuctionBidsTreeSet(auction.getItemID());
    }

    public AuctionManager(Wallet seller, long minAmount, float minIncrement, long fee, long timeout){
        this.seller = seller;
        String randomString = Utils.randomString(Utils.hashAlgorithmLengthInBytes);
        Auction auction = new Auction(seller, randomString, minAmount, minIncrement, fee, timeout);
        this.auction = auction;
        AuctionsState.addAuction(auction);
        this.bids_status = AuctionsState.getAuctionBidsTreeSet(randomString);
        BlockchainUtils.getKademliaClient().announceNewAuction(auction);
        runningAuction = new Thread(this, "AuctionRunning: " + auction.getItemID());
        runningAuction.start();
    }

    private Transaction createTransaction(Bid winBid) {
        return new Transaction(seller, winBid);
    }


    @Override
    public void run() {
        while(this.bids_status.isEmpty()){
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Bid winBid = this.bids_status.last();
        long timestamp = new Date().getTime();
        while((new Date().getTime())-timestamp<this.auction.getTimeout()){
            if (this.bids_status.last()!=winBid){
                winBid=this.bids_status.last();
                timestamp = new Date().getTime();
            }else{
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        logger.info("Auction has ended");
        Transaction trans = createTransaction(winBid);
        BlockchainUtils.addTransaction(trans);
        BlockchainUtils.getKademliaClient().announceNewTransaction(trans);
    }

    public Auction getAuction() {
        return auction;
    }

    public Thread getRunningAuction() {
        return runningAuction;
    }
}
