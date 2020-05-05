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
        signTransaction(buyerWallet.getPrivKey());
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
        if(isHashValid()&&verifySignature()&&Wallet.checkAddress(this.buyerPublicKey, this.buyerID)) return true;
        else return false;
    }

    public Boolean isHashValid(){
        if(!this.hash.equals(this.getHashToBeSigned())){
            logger.warning("Transaction Hashes don't match");
            return false;
        }
        else return true;
    }
    public Boolean verifySignature(){
        if(this.signature==null){
            logger.warning("Transaction is not signed");
            return false;
        }
        boolean output =false;
        try {
            Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA");
            ecdsaVerify.initVerify(this.buyerPublicKey);
            ecdsaVerify.update(this.hash.getBytes(StandardCharsets.UTF_8));
            output = ecdsaVerify.verify(this.signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
        if (!output) logger.severe("Signatures don't match");
        this.hash=this.getHashToBeSigned();
        return output;
    }

    public void signTransaction(PrivateKey privKey){
        try {
            Signature ecdsaSign = Signature.getInstance("SHA256withECDSA");
            ecdsaSign.initSign(privKey);
            ecdsaSign.update(this.hash.getBytes(StandardCharsets.UTF_8));
            this.signature = ecdsaSign.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
    }

    private String getHashToBeSigned(){
        if(this.buyerPublicKey!=null){
            return BlockchainUtils.getsha256(this.sellerID + this.buyerID + BlockchainUtils.getStringFromKey(this.buyerPublicKey) + this.amount + this.transactionFee + this.itemID + this.timeStamp);
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
