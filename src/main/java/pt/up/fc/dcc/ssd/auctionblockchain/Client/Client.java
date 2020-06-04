package pt.up.fc.dcc.ssd.auctionblockchain.Client;

import pt.up.fc.dcc.ssd.auctionblockchain.Auction.Auction;
import pt.up.fc.dcc.ssd.auctionblockchain.Wallet;

import java.util.HashMap;

public class Client {
    //saves the itemID and the lastest bid received
    private static final HashMap<String, AuctionUpdate> subscribedBids = new HashMap<>();

    public static void bet(Wallet buyer, String itemId, long amount){

    }
    public static void addBid(String itemId, Bid bid){
        AuctionUpdate auctionUpdate = subscribedBids.get(itemId);
        auctionUpdate.updateLatestBid(bid);
    }
}
class AuctionUpdate{
    Auction auction;
    Boolean isSubscribed;
    Bid latestBid;

    public void updateLatestBid(Bid bid){
        if (!this.isSubscribed){
            this.isSubscribed = true;
        }
        latestBid = bid;
    }
}