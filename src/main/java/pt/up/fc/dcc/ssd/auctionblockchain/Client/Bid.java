package pt.up.fc.dcc.ssd.auctionblockchain.Client;

import pt.up.fc.dcc.ssd.auctionblockchain.Auction.Auction;
import pt.up.fc.dcc.ssd.auctionblockchain.Blockchain.BlockchainUtils;
import pt.up.fc.dcc.ssd.auctionblockchain.Utils;
import pt.up.fc.dcc.ssd.auctionblockchain.Wallet;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.logging.Logger;

public class Bid {
    private static final Logger logger = Logger.getLogger(Bid.class.getName());

    private String itemId;
    private final String sellerID;
    private String buyerID;
    private long amount;
    private long fee;
    PublicKey buyerPublicKey;
    private String hash;
    private byte[] signature;

    public Bid(Wallet buyer, String itemId, String sellerID, long amount, long fee) {
        this.itemId = itemId;
        this.sellerID = sellerID;
        this.buyerID = buyer.getAddress();
        this.amount = amount;
        this.fee = fee;
        this.buyerPublicKey = buyer.getPubKey();
        this.hash = this.getHashToBeSigned();
        this.signature = Wallet.signHash(buyer.getPrivKey(), this.hash, logger);
    }

    public Bid(String itemId, String sellerID, String buyerID, long amount, long fee, byte[] buyerPublicKey, String hash, byte[] signature) {
        this.itemId = itemId;
        this.sellerID = sellerID;
        this.buyerID = buyerID;
        this.amount = amount;
        this.fee = fee;
        this.buyerPublicKey = Wallet.getPublicKeyFromBytes(buyerPublicKey);
        this.hash = hash;
        this.signature = signature;
    }

    public Bid(Wallet buyer, Auction auction, long amount){
        this.itemId = auction.getItemID();
        this.sellerID = auction.getSellerID();
        this.buyerID = buyer.getAddress();
        this.amount = amount;
        this.fee = auction.getFee();
        this.buyerPublicKey = buyer.getPubKey();
        this.hash = this.getHashToBeSigned();
        this.signature = Wallet.signHash(buyer.getPrivKey(), this.hash, logger);
    }

    //coin base reward
    public Bid(String sellerID, long fee) {
        this.sellerID = sellerID;
        this.buyerID = Utils.getStandardString();
        this.amount = BlockchainUtils.getMinerReward();
        this.fee = fee;
        buyerPublicKey = null;
        this.hash = Utils.getHash(this.sellerID + this.buyerID + this.amount + this.fee);
    }

    public Boolean verifyBid() {
        return this.isHashValid()
                && Wallet.verifySignature(this.signature,this.hash, this.buyerPublicKey, logger)
                && Wallet.checkAddress(this.buyerPublicKey, this.buyerID);
    }

    private Boolean isHashValid() {
        if(!this.hash.equals((this.getHashToBeSigned()))){
            logger.warning("Bids Hashes don't match");
            return false;
        }
        else return true;
    }

    public String getHashToBeSigned(){
        return Utils.getHash(this.itemId + this.sellerID + this.buyerID + this.amount + this.fee + buyerPublicKey.hashCode());
    }

    public String getHashWithSig() {
        if (signature != null){
            return Utils.getHash(this.itemId + this.sellerID + this.buyerID + this.amount + this.fee + buyerPublicKey.hashCode()+ Arrays.toString(this.signature));
        }else{
            return this.hash;
        }
    }

    public byte[] getSignature() {
        return signature;
    }

    public String getSellerID() {
        return this.sellerID;
    }

    public String getItemId() {
        return itemId;
    }

    public String getBuyerID() {
        return buyerID;
    }

    public long getAmount() {
        return amount;
    }

    public long getFee() {
        return fee;
    }

    public PublicKey getBuyerPublicKey() {
        return buyerPublicKey;
    }

    public String getHash() {
        return hash;
    }
}
