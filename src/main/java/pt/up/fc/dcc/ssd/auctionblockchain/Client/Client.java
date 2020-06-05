package pt.up.fc.dcc.ssd.auctionblockchain.Client;

import pt.up.fc.dcc.ssd.auctionblockchain.Auction.Auction;
import pt.up.fc.dcc.ssd.auctionblockchain.Auction.AuctionManager;
import pt.up.fc.dcc.ssd.auctionblockchain.Auction.AuctionsState;
import pt.up.fc.dcc.ssd.auctionblockchain.Wallet;

import java.util.HashMap;
import java.util.TreeSet;
import java.util.logging.Logger;

public class Client implements Runnable{
    private static final Logger logger = Logger.getLogger(Client.class.getName());
    //saves the itemID and the lastest bid received
    private HashMap<Bid, TreeSet<Bid>> bidsParticipated = new HashMap<>();

    public static void bet(Wallet buyer, String itemId, long amount){
        Auction auction = AuctionsState.getAuction(itemId);
        Bid myBid = new Bid(buyer, auction, amount);
        if(!AuctionsState.checkWinningBid(myBid)){
            return;
        }
        AuctionsState.updateBid(myBid);
    }

    @Override
    public void run() {

    }
}