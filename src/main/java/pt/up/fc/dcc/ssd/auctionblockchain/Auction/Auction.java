package pt.up.fc.dcc.ssd.auctionblockchain.Auction;

import pt.up.fc.dcc.ssd.auctionblockchain.Kademlia.KademliaNode;
import pt.up.fc.dcc.ssd.auctionblockchain.Utils;
import pt.up.fc.dcc.ssd.auctionblockchain.Wallet;

import java.security.PublicKey;
import java.util.logging.Logger;

public class Auction {
    private static final Logger logger = Logger.getLogger(Auction.class.getName());

    private final String itemID;
    private final String sellerID;
    private final KademliaNode buyerNode;
    private long minAmount;
    private long minIncrement;
    private long fee;
    private long timeout;
    private final PublicKey sellerPublicKey;
    private final String hash;
    private final byte[] signature;

    public Auction(String itemID, String sellerID, KademliaNode buyerNode, long minAmount, long minIncrement, long fee, long timeout, PublicKey sellerPublicKey, String hash, byte[] signature) {
        this.itemID = itemID;
        this.sellerID = sellerID;
        this.buyerNode = buyerNode;
        this.minAmount = minAmount;
        this.minIncrement = minIncrement;
        this.fee = fee;
        this.timeout = timeout;
        this.sellerPublicKey = sellerPublicKey;
        this.hash = hash;
        this.signature = signature;
    }

    public Auction(Wallet seller, String itemID, KademliaNode buyerNode, long minAmount, long minIncrement, long fee, long timeout){
        this.itemID=itemID;
        this.sellerID=seller.getAddress();
        this.buyerNode = buyerNode;
        this.minAmount=minAmount;
        this.minIncrement=minIncrement;
        this.fee = fee;
        this.timeout = timeout;
        this.sellerPublicKey= seller.getPubKey();
        this.hash= this.getHashToBeSigned();
        this.signature = Wallet.signHash(seller.getPrivKey(), this.hash, logger);
    }

    public Boolean verifyAuction(){
        return  isHashValid()
                && Wallet.verifySignature(this.signature, this.hash, this.sellerPublicKey, logger)
                && Wallet.checkAddress(this.sellerPublicKey, this.sellerID);
    }

    private String getHashToBeSigned() {
        return Utils.getsha256(this.itemID + this.sellerID + buyerNode.toString() + minAmount + minIncrement +fee + timeout + this.sellerPublicKey.hashCode());
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

    public String getItemID() {
        return itemID;
    }

    public String getSellerID() {
        return sellerID;
    }

    public KademliaNode getBuyerNode() {
        return buyerNode;
    }

    public long getMinAmount() {
        return minAmount;
    }

    public long getMinIncrement() {
        return minIncrement;
    }

    public long getFee() {
        return fee;
    }

    public long getTimeout() {
        return timeout;
    }
}
