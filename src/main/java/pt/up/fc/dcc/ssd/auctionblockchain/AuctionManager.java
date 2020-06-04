package pt.up.fc.dcc.ssd.auctionblockchain;

import java.util.Comparator;
import java.util.Date;
import java.util.TreeSet;
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
