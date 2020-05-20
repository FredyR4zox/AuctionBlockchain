package pt.up.fc.dcc.ssd.auctionblockchain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Logger;

public class Transaction {
    private static final Logger logger = Logger.getLogger(Transaction.class.getName());
    private final String sellerID;
    private final String buyerID;
    private PublicKey buyerPublicKey;
    private long amount;
    private long transactionFee;
    private long itemID;
    private byte[] signature;
    private String hash;
    private final long timeStamp;

    public Transaction(String buyerID, String sellerID, byte[] buyerPublicKey, long amount, long transactionFee, long itemID, byte[] signature) {
        this.buyerID = buyerID;
        this.sellerID = sellerID;
        this.buyerPublicKey= Wallet.getPublicKeyFromBytes(buyerPublicKey);
        this.amount = amount;
        this.transactionFee = transactionFee;
        this.itemID = itemID;
        this.signature = signature;
        this.timeStamp = new Date().getTime();
        this.hash = this.getHashToBeSigned();

    }

    public Transaction(Wallet buyerWallet, String sellerID, long amount, long transactionFee, long itemID){
        this.buyerID = buyerWallet.getAddress();
        this.buyerPublicKey= buyerWallet.getPubKey();
        this.sellerID = sellerID;
        this.amount = amount;
        this.transactionFee = transactionFee;
        this.itemID = itemID;
        this.timeStamp = new Date().getTime();
        this.hash = this.getHashToBeSigned();
        this.signature = Wallet.signHash(buyerWallet.getPrivKey(), this.hash, logger);
    }

    public Transaction(Transaction transaction){
        this.buyerID = transaction.getBuyerID();
        this.buyerPublicKey= transaction.getBuyerPublicKey();
        this.sellerID = transaction.getSellerID();
        this.amount = transaction.getAmount();
        this.transactionFee = transaction.getTransactionFee();
        this.itemID = transaction.getItemID();
        this.hash = transaction.getHash();
        this.timeStamp = transaction.getTimeStamp();
        this.signature =transaction.getSignature();
    }

    //coinbase Transaction for miner reward
    public Transaction(String sellerID, long transactionFeesTotal) {
        this.sellerID = sellerID;
        this.buyerID = "0000000000000000000000000000000000000000000000000000000000000000";
        this.amount = BlockchainUtils.minerReward + transactionFeesTotal;
        this.timeStamp = new Date().getTime();
        this.hash = this.getHashToBeSigned();
    }

    private Double getTransactionFeeValue(double amount, double transactionFeePercentage) {
        return (amount*transactionFeePercentage)/100;
    }

    public Boolean verifyTransaction() {
        //Do hashes check && Do signature checks
        return isHashValid()
                && Wallet.verifySignature(this.signature, this.hash, this.buyerPublicKey, logger)
                && Wallet.checkAddress(this.buyerPublicKey, this.buyerID);
    }

    public Boolean isHashValid(){
        if(!this.hash.equals(this.getHashToBeSigned())){
            logger.warning("Transaction Hashes don't match");
            return false;
        }
        else return true;
    }

    private String getHashToBeSigned(){
        if(this.buyerPublicKey!=null){
            return BlockchainUtils.getsha256(this.sellerID + this.buyerID + Wallet.getAddressFromPubKey(this.buyerPublicKey) + this.amount + this.transactionFee + this.itemID + this.timeStamp);
        }
        else{
            return BlockchainUtils.getsha256(this.sellerID + this.buyerID + this.amount + this.timeStamp);
        }
    }
    public String makeJson(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
    public static Transaction makeFromJson(String tJson){
        return new Gson().fromJson(tJson, Transaction.class);
    }

    public long getTransactionFee() {
        return transactionFee;
    }

    public long getAmount() {
        return amount;
    }

    public String getSellerID() {
        return sellerID;
    }

    public String getBuyerID() {
        return buyerID;
    }

    public long getItemID(){
        return itemID;
    }

    public PublicKey getBuyerPublicKey() {
        return buyerPublicKey;
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
}
