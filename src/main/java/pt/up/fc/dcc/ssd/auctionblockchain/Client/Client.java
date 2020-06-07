package pt.up.fc.dcc.ssd.auctionblockchain.Client;

import pt.up.fc.dcc.ssd.auctionblockchain.Auction.Auction;
import pt.up.fc.dcc.ssd.auctionblockchain.Auction.AuctionsState;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.BlockchainUtils;
import pt.up.fc.dcc.ssd.auctionblockchain.Pair;
import pt.up.fc.dcc.ssd.auctionblockchain.Wallet;

import java.util.*;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

public class Client implements Runnable{
    private static final Logger logger = Logger.getLogger(Client.class.getName());

    static Wallet clientWallet;
    private static final Scanner scanner =new Scanner(System.in);
    //saves the itemID and the lastest bid received
    public static Bid bet(String itemId, long amount){
        Auction auction = AuctionsState.getAuction(itemId);
        if(auction==null){
            System.out.println("Auction not found");
            return null;
        }
        Bid myBid = new Bid(clientWallet, auction, amount);
        if(!AuctionsState.updateBid(myBid)){
            return null;
        }
        if(!BlockchainUtils.getKademliaClient().announceNewBid(myBid)){
            return null;
        }
        return myBid;
        /*if(interactive){

            Thread thread = new Thread(new Client());
            thread.start();
        }*/
    }

    public static Bid getBid(String itemId){
        System.out.println("How much do u wish to bet");

        long amountAnswer = scanner.nextLong();

        return bet(itemId, amountAnswer);
    }

    public static Bid newBid(){
        System.out.println("To which auction do you wish to bid?");
        AuctionsState.printAuctions();

        String auctionAnswer = scanner.nextLine();

        return getBid(auctionAnswer);
    }

    @Override
    public void run() {
        Bid myBid = newBid();
        if (myBid==null){
            System.out.println("Bid not successfull");
            return;
        }
        TreeSet<Bid> competition = AuctionsState.getAuctionBidsTreeSet(myBid.getItemId());
        while(true) {
            if (AuctionsState.checkAuctionGoing(myBid.getItemId())){
                System.out.println("Auction has ended");
                return;
            }
            assert competition != null;
            Bid winBid = competition.last();
            if (myBid != winBid) {
                System.out.println("Someone outbid u at auction : "+ myBid.getItemId() + " with the amount : " + winBid.getAmount() + "\nDo u wish to bet?[y/n]");
                String response = scanner.nextLine();
                if(response.equals("y")){
                    myBid = getBid(myBid.getItemId());
                    if(myBid!=null){
                        System.out.println("Bid not successful");
                    }
                    else{
                        System.out.println("Bid successful");
                    }
                }
                else{
                    return;
                }
            }
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void setClientWallet(Wallet clientWallet) {
        Client.clientWallet = clientWallet;
    }
}