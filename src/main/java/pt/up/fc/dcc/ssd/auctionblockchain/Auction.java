package pt.up.fc.dcc.ssd.auctionblockchain;

import java.security.PublicKey;
import java.util.Comparator;
import java.util.Date;
import java.util.TreeSet;
import java.util.logging.Logger;

public class Auction {
    private static final Logger logger = Logger.getLogger(Auction.class.getName());

    private final long itemID;
    private final String sellerID;
    private final PublicKey sellerPublicKey;
    private final long endTimeStamp;
    private final String hash;
    private final byte[] signature;


    public Auction( long itemID, long time_to_end, Wallet seller){
        this.itemID=itemID;
        this.sellerID=seller.getAddress();
        this.endTimeStamp= new Date().getTime() + time_to_end;
        this.sellerPublicKey= seller.getPubKey();
        this.hash= this.getHashToBeSigned();
        this.signature = Wallet.signHash(seller.getPrivKey(), this.hash, logger);
    }

    public Auction(Auction auction) {
        this.itemID = auction.getItemID();
        this.sellerID = auction.getSellerID();
        this.sellerPublicKey = auction.getSellerPublicKey();
        this.endTimeStamp = auction.getEndTimeStamp();
        this.hash = auction.getHash();
        this.signature = auction.getSignature();
    }

    public Boolean verifyAuction(){
        return  isHashValid()
                && Wallet.verifySignature(this.signature, this.hash, this.sellerPublicKey, logger)
                && Wallet.checkAddress(this.sellerPublicKey, this.sellerID);
    }

    private String getHashToBeSigned() {
        return Utils.getsha256(this.itemID + this.sellerID + Wallet.getAddressFromPubKey(this.sellerPublicKey) + this.endTimeStamp);
    }

    private Boolean isHashValid() {
        if(!this.hash.equals(this.getHashToBeSigned())){
            logger.warning("Auction Hashes don't match");
            return false;
        }
        else return true;
    }

    public PublicKey getSellerPublicKey() {
        return sellerPublicKey;
    }

    public String getHash() {
        return hash;
    }

    public byte[] getSignature() {
        return signature;
    }

    public long getItemID() {
        return itemID;
    }

    public String getSellerID() {
        return sellerID;
    }

    public long getEndTimeStamp() {
        return endTimeStamp;
    }
}
