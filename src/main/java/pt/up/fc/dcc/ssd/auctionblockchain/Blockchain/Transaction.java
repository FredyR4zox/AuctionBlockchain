package pt.up.fc.dcc.ssd.auctionblockchain.Blockchain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import pt.up.fc.dcc.ssd.auctionblockchain.Client.Bid;
import pt.up.fc.dcc.ssd.auctionblockchain.Utils;
import pt.up.fc.dcc.ssd.auctionblockchain.Wallet;

import java.security.*;
import java.util.Date;
import java.util.logging.Logger;

public class Transaction {
    private static final Logger logger = Logger.getLogger(Transaction.class.getName());

    private Bid bid;
    private PublicKey sellerPublicKey;
    private final long timeStamp;
    private final String hash;
    private final byte[] signature;

    public Transaction(Bid bid, byte[] sellerPublicKey, long timeStamp, String hash, byte[] signature) {
        this.bid = bid;
        this.sellerPublicKey = Wallet.getPublicKeyFromBytes(sellerPublicKey);
        this.timeStamp = timeStamp;
        this.hash = hash;
        this.signature = signature;
    }

    public Transaction(Wallet seller, Bid bid){
        this.bid=bid;
        this.timeStamp = new Date().getTime();
        this.sellerPublicKey=seller.getPubKey();
        this.hash = this.getHashToBeSigned();
        this.signature = Wallet.signHash(seller.getPrivKey(), this.hash, logger);
    }

    //create transaction and bid
    public Transaction(Wallet buyer, Wallet seller, long amount, long fee, String itemId){
        this.bid= new Bid(buyer, itemId, seller.getAddress(), amount, fee);
        this.sellerPublicKey = seller.getPubKey();
        this.timeStamp = new Date().getTime();
        this.hash = this.getHashToBeSigned();
        this.signature = Wallet.signHash(seller.getPrivKey(), this.hash, logger);
    }

    public Transaction(Transaction transaction){
        this.bid = transaction.getBid();
        this.sellerPublicKey = transaction.getSellerPublicKey();
        this.hash = transaction.getHash();
        this.timeStamp = transaction.getTimeStamp();
        this.signature =transaction.getSignature();
    }

    //coinbase Transaction for miner reward
    public Transaction(Wallet miner, long transactionFeesTotal) {
        this.bid = new Bid(miner.getAddress(), transactionFeesTotal);
        this.timeStamp = new Date().getTime();
        this.sellerPublicKey = miner.getPubKey();
        this.hash = this.getHashToBeSigned();
        this.signature = Wallet.signHash(miner.getPrivKey(), this.hash, logger);
    }

    private Double getTransactionFeeValueFromPercentage(double amount, double transactionFeePercentage) {
        return (amount*transactionFeePercentage)/100;
    }

    public Boolean verifyTransaction() {
        //Do hashes check && Do signature checks
        return isHashValid()
                && Wallet.verifySignature(this.signature, this.hash, this.sellerPublicKey, logger)
                && Wallet.checkAddress(this.sellerPublicKey, this.bid.getSellerID())
                && this.bid.verifyBid();
    }

    public Boolean isHashValid(){
        logger.info("Received hash : " +  this.hash + "\nRecalculated hash : " + this.getHashToBeSigned() + "\n");
        if(!this.hash.equals(this.getHashToBeSigned())){
            logger.warning("Transaction Hashes don't match");
            return false;
        }
        else return true;
    }

    private String getHashToBeSigned(){
        return Utils.getHash(this.bid.getHashWithSig() + this.sellerPublicKey.hashCode() + this.timeStamp);
    }
    public String makeJson(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
    public static Transaction makeFromJson(String tJson){
        return new Gson().fromJson(tJson, Transaction.class);
    }


    public byte[] getSignature() {
        return signature;
    }

    public String getHash() {
        return hash;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public Bid getBid() {
        return bid;
    }
    public void setBid(Bid bid){
        this.bid=bid;
    }

    public PublicKey getSellerPublicKey() {
        return sellerPublicKey;
    }
}
