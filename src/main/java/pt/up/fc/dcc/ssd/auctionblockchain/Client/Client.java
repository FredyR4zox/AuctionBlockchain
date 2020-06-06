package pt.up.fc.dcc.ssd.auctionblockchain.Client;

import pt.up.fc.dcc.ssd.auctionblockchain.Auction.Auction;
import pt.up.fc.dcc.ssd.auctionblockchain.Auction.AuctionManager;
import pt.up.fc.dcc.ssd.auctionblockchain.Auction.AuctionsState;
import pt.up.fc.dcc.ssd.auctionblockchain.Pair;
import pt.up.fc.dcc.ssd.auctionblockchain.Wallet;

import java.util.*;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

public class Client implements Runnable{
    private static final Logger logger = Logger.getLogger(Client.class.getName());

    private static final Scanner scanner =new Scanner(System.in);
    //saves the itemID and the lastest bid received
    private static List<Pair<Bid, TreeSet<Bid>>> bidsParticipated = new LinkedList<>();

    public static void bet(Wallet buyer, String itemId, long amount){
        Auction auction = AuctionsState.getAuction(itemId);
        assert auction != null;
        Bid myBid = new Bid(buyer, auction, amount);
        AuctionsState.updateBid(myBid);
        /*if(interactive){
            bidsParticipated.add(new Pair(myBid, AuctionsState.getAuctionBidsTreeSet(itemId)));
            Thread thread = new Thread(new Client());
            thread.start();
        }*/
    }

    @Override
    public void run() {
        while(true) {
            for (Pair<Bid, TreeSet<Bid>> mybet : bidsParticipated) {
                if (mybet.getFirst() != mybet.getSecond().last()) {
                    System.out.println("Someone outbid u, do u wish to bet");
                    String response = scanner.nextLine();
                    if(response.equals("y")){
                        System.out.println("update");
                    }
                }
            }
            try {
                sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}