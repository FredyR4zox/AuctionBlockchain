package com.ssd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class Transaction {
    private final String sellerID;
    private final String buyerID;
    private final Double amount;
    private Double transactionFee;
    private long itemID;
    private PublicKey buyerPublicKey;
    private byte[] signature;
    private String hash;

    public Transaction(String buyerID, String sellerID, byte[] buyerPublicKey, double amount, double transactionFeePercentage, long itemID) {
        this.buyerID = buyerID;
        this.sellerID = sellerID;
        this.buyerPublicKey= Wallet.getPublicKeyFromBytes(buyerPublicKey);
        this.amount = amount;
        this.transactionFee = getTransactionFeeValue(amount, transactionFeePercentage);
        this.itemID = itemID;
        this.hash = this.getHashToBeSigned();
    }

    public Transaction(Wallet buyerWallet, String sellerID, double amount, double transactionFeePercentage, long itemID){
        this.buyerID = buyerWallet.getAddress();
        this.buyerPublicKey= buyerWallet.getPubKey();
        this.sellerID = sellerID;
        this.amount = amount;
        this.transactionFee = getTransactionFeeValue(amount, transactionFeePercentage);
        this.itemID = itemID;
        this.hash = this.getHashToBeSigned();
        signTransaction(buyerWallet.getPrivKey());
    }

    private Double getTransactionFeeValue(double amount, double transactionFeePercentage) {
        return (amount*transactionFeePercentage)/100;
    }

    //coinbase Transaction for miner reward
    public Transaction(String sellerID, double transactionFeesTotal) {
        this.sellerID = sellerID;
        this.buyerID = "0000000000000000000000000000000000000000000000000000000000000000";
        this.amount = BlockChain.getMinerReward()+transactionFeesTotal;
        this.hash = this.getHashToBeSigned();
    }

    public Boolean verifyTransaction() {
        //Do hashes check && Do signature checks
        if(isHashValid()&&verifySignature()&&Wallet.checkAddress(this.buyerPublicKey, this.buyerID)) return true;
        else return false;
    }

    public Boolean isHashValid(){
        if(!this.hash.equals(this.getHashToBeSigned())){
            System.out.println("Transaction Hashes don't match");
            return false;
        }
        else return true;
    }
    public Boolean verifySignature(){
        if(this.signature==null){
            System.out.println("Transaction is not signed");
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
        if (!output) System.out.println("Signatures don't match");
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
            return utils.getsha256(this.sellerID + this.buyerID + utils.getStringFromKey(this.buyerPublicKey) + this.amount + this.transactionFee + this.itemID);
        }
        else{
            return utils.getsha256(this.sellerID + this.buyerID + this.amount);
        }
    }
    public String makeJson(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
    public static Transaction makeFromJson(String tJson){
        return new Gson().fromJson(tJson, Transaction.class);
    }

    public Double getTransactionFee() {
        return transactionFee;
    }

    public Double getAmount() {
        return amount;
    }

    public String getSellerID() {
        return sellerID;
    }

    public String getBuyerID() {
        return buyerID;
    }
}
