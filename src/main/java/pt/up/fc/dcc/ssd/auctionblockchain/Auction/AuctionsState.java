package pt.up.fc.dcc.ssd.auctionblockchain.Auction;

import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.BlockchainUtils;
import pt.up.fc.dcc.ssd.auctionblockchain.Client.Bid;
import pt.up.fc.dcc.ssd.auctionblockchain.Utils;

import java.util.*;
import java.util.logging.Logger;

public class AuctionsState {
    private static final Logger logger = Logger.getLogger(AuctionsState.class.getName());

    private static final HashMap<String, AuctionState> auctionStates = new HashMap<>();
    //state of how much money wallets have spent on Auctions
    private static final HashMap<String, Long> walletsTrans = new HashMap<>();

    public static boolean addAuction(Auction auction){
        if(!auction.verifyAuction()){
            return false;
        }

        AuctionState newAuctionState = new AuctionState(auction);
        auctionStates.put(auction.getItemID(), newAuctionState);

        return true;
    }

    public static boolean updateBid(Bid bid){
        updateAuctionsState();
        if(checkAuctionGoing(bid.getItemId())){
            if(!checkWinningBid(bid))
                return false;

            AuctionState auctionUpdate = auctionStates.get(bid.getItemId());
            Bid previousBid = auctionUpdate.updateBids(bid);

            if(previousBid == null){
                addToWalletTrans(bid);
            }
            else {
                updateWalletTrans(bid, previousBid);
            }
        }

        return true;
    }

    public static boolean checkAuctionGoing(String itemId) {
        return auctionStates.get(itemId) != null;
    }

    public static Boolean checkWinningBid(Bid bid){
        if(!bid.verifyBid()){
            return false;
        }
        if(!verifyBidInAuction(bid, auctionStates.get(bid.getItemId()).getAuction())){
            return false;
        }
        if(!verifyBidInChain(bid)){
            return false;
        }
        return true;
    }

    private static boolean verifyBidInChain(Bid bid) {
        long amountInOtherAuctions= walletsTrans.getOrDefault(bid.getBuyerID(), 0L);
        return BlockchainUtils.getOriginal().checkIfEnoughFunds(bid.getBuyerID(), bid.getAmount() +amountInOtherAuctions);
    }

    private static boolean verifyBidInAuction(Bid bid, Auction auction) {
        //check if bid corresponds to this auction
        boolean output = bid.getItemId().equals(auction.getItemID());
        output &= bid.getSellerID().equals(auction.getSellerID());
        output &= bid.getFee() == auction.getFee();
        if(!output){
            logger.warning("bid parameters aren't valid");
            return false;
        }
        //check if its first bid
        Bid prevBid= auctionStates.get(bid.getItemId()).getLatestBid();
        if(prevBid==null){
            return true;
        }
        //check if minimum increment was followed
        output = Utils.verifyAmountIncrement(prevBid.getAmount(), bid.getAmount(), auction.getMinIncrement(), logger);
        return output;
    }

    private static void addToWalletTrans(Bid bid) {
        if(walletsTrans.putIfAbsent(bid.getBuyerID(), bid.getAmount())!=null){
            long newAmount = walletsTrans.get(bid.getBuyerID()) + bid.getAmount();
            walletsTrans.replace(bid.getBuyerID(), newAmount);
        }
    }

    private static void removeFromWalletTrans(Bid bid) {
        long amount = walletsTrans.get(bid.getBuyerID());
        amount = amount - bid.getAmount();
        walletsTrans.replace(bid.getBuyerID(), amount);
    }

    private static void updateWalletTrans(Bid newBid, Bid previousBid) {
        long newAmount = walletsTrans.get(previousBid.getBuyerID()) - previousBid.getAmount() ;
        walletsTrans.replace(previousBid.getBuyerID(), newAmount);
        newAmount = walletsTrans.get(newBid.getBuyerID()) + newBid.getAmount() ;
        walletsTrans.replace(newBid.getBuyerID(), newAmount);
    }

    //TODO check if blockchain updates change winning bids
    //end auctions that already have transactions
    public static void updateAuctionsState(){
        Set<String> auctionIds = auctionStates.keySet();
        List<String> removeAuctionIds = new LinkedList<>();
        for(String auctionId : auctionIds){
            if(BlockchainUtils.getOriginal().getRegisteredIDs().contains(auctionId)){
                Bid winBid = auctionStates.get(auctionId).getLatestBid();
                if(winBid==null){
                    continue;
                }
                else{
                    removeFromWalletTrans(winBid);
                    //cant remove auctions here becuz of concurrency problems
                    removeAuctionIds.add(auctionId);
                }
            }
        }
        //remove auctions that have finished
        for(String removeId : removeAuctionIds){
            auctionStates.remove(removeId);
        }

    }

    public static List<Auction> getAuctions(){
        List<Auction> auctions = new ArrayList<>();

        for(AuctionState auction : auctionStates.values())
            auctions.add(auction.auction);

        return auctions;
    }

    public static TreeSet<Bid> getAuctionBidsTreeSet(String itemId){
        AuctionState state = auctionStates.get(itemId);

        if(state == null)
            return null;

        return state.getBids();
    }

    public static Auction getAuction(String itemId) {
        AuctionState state = auctionStates.get(itemId);

        if(state == null)
            return null;

        return state.getAuction();
    }

    public static void printAuctions() {
        for(AuctionState print: auctionStates.values()){
            print.printAuctionState();
        }
    }

    public static boolean isNameValid(String name) {
        if(auctionStates.get(name)!= null){
            return false;
        }
        return !BlockchainUtils.original.getRegisteredIDs().contains(name);
    }
}
class AuctionState{
    Auction auction;
    TreeSet<Bid> bids = new TreeSet<>(new BidCompare());

    public AuctionState(Auction auction) {
        this.auction = auction;
    }

    //return previous latest bid
    public Bid updateBids(Bid bid){
        Bid previousBid = null;
        try{
            previousBid = bids.last();
        } catch (NoSuchElementException ignored){}
        bids.add(bid);
        return previousBid;

    }

    public Auction getAuction() {
        return auction;
    }

    public TreeSet<Bid> getBids() {
        return bids;
    }

    public Bid getLatestBid() {
        if(bids.isEmpty()){
            return null;
        }else{
            return bids.last();
        }
    }

    public void printAuctionState(){
        System.out.println("Auction : " + auction.getItemID());
        Bid latest = this.getLatestBid();
        if (latest==null){
            System.out.println("No latest bid");
        }
        else{
            System.out.println("Latest Bid is off" + latest.getAmount());
        }
        System.out.println();
    }

    static class BidCompare implements Comparator<Bid> {
        @Override
        public int compare(Bid bid, Bid b1) {
            //check bigger amount
            int result = Long.compare(bid.getAmount(), b1.getAmount());
            if (bid.getHash().equals(b1.getHash())){
                return 0;
            }
            else if(result == 0){return 1;}
            else return result;
        }
    }
}
