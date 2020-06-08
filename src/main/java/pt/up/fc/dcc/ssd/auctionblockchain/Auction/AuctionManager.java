package pt.up.fc.dcc.ssd.auctionblockchain.Auction;

import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.BlockchainUtils;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.Transaction;
import pt.up.fc.dcc.ssd.auctionblockchain.Client.Bid;
import pt.up.fc.dcc.ssd.auctionblockchain.Utils;
import pt.up.fc.dcc.ssd.auctionblockchain.Wallet;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

public class AuctionManager implements Runnable{
    private static final Logger logger = Logger.getLogger(AuctionManager.class.getName());

    private static final Scanner scanner =new Scanner(System.in);
    private Auction auction;
    private Wallet seller;
    private TreeSet<Bid> bids_status;
    private Thread runningAuction;

    public AuctionManager(Auction auction) {
        this.auction = auction;
        AuctionsState.addAuction(auction);
        this.bids_status= AuctionsState.getAuctionBidsTreeSet(auction.getItemID());
    }

    public AuctionManager(Wallet seller){
        this.seller = seller;
        //this.auction = getAuction(seller);
        this.auction = new Auction(seller, "AAAAAA", 10, 5, 2, 10000);
        AuctionsState.addAuction(auction);
        this.bids_status = AuctionsState.getAuctionBidsTreeSet(auction.getItemID());
        BlockchainUtils.getKademliaClient().announceNewAuction(auction);
        runningAuction = new Thread(this, "AuctionRunning: " + auction.getItemID());
        runningAuction.start();
    }

    private Auction getAuction(Wallet seller) {
        //System.out.println("What do you wish the auctionID to be:");
        byte[] randomID = new byte[8];
        Random random = new SecureRandom();
        random.nextBytes(randomID);

        String name = Utils.bytesToHexString(randomID) ;
        /*while(!AuctionsState.isNameValid(name)){
            System.out.println("Name already used, insert another one");
            name = scanner.nextLine();
        }*/
        System.out.println("Insert the minimun amount for the bid");
        long amountRead = scanner.nextLong();
        System.out.println("Insert the minimun increment percentage");
        float increment = scanner.nextFloat();
        System.out.println("Insert the fee for the auction \nBigger fees equals faster processing");
        long fee = scanner.nextLong();
        System.out.println("Insert the maximum time between bets in seconds");
        long timeout=scanner.nextLong() * 1000;
        Auction auction = new Auction(seller, name, amountRead, increment, fee, timeout);
        return auction;
    }

    private Transaction createTransaction(Bid winBid) {
        return new Transaction(seller, winBid);
    }


    @Override
    public void run() {
        logger.info("started auction");
        while(this.bids_status.isEmpty()){
            this.bids_status = AuctionsState.getAuctionBidsTreeSet(auction.getItemID());
            try {
                sleep(4000);
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
                    Thread.sleep(1000);
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
