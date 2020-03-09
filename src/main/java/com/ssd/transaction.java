package com.ssd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.security.*;

public class transaction {
    public PublicKey sellerID;
    public PublicKey buyerID;
    public long amount;
    public long transactionFee;
    public long itemID;
    public byte[] signature;
    public String hash;

    public transaction(PublicKey buyerID, PublicKey sellerID, long amount, long transactionFee, long itemID) {
        this.buyerID = buyerID;
        this.sellerID = sellerID;
        this.amount = amount;
        this.transactionFee = transactionFee;
        this.itemID = itemID;
        this.hash = this.getHashToBeSigned();
    }

    public transaction(PublicKey sellerID) {
        this.sellerID = sellerID;
        this.amount = BlockChain.getMinerReward();
        this.hash = this.getHashToBeSigned();
    }

    public Boolean verifyTransaction() {
        //check if buyer has the amount of
        return true;
    }
    public boolean verifySignature(){
        if(!this.hash.equals(this.getHashToBeSigned())){
            System.out.println("Hashes don't match");
            return false;
        }
        boolean output =false;
        try {
            Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA");
            ecdsaVerify.initVerify(this.buyerID);
            ecdsaVerify.update(this.hash.getBytes(StandardCharsets.UTF_8));
            output = ecdsaVerify.verify(this.signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
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
        if(this.buyerID != null) {
            return utils.getsha256(utils.getStringFromKey(this.sellerID) + utils.getStringFromKey(this.buyerID) + this.amount + this.transactionFee + this.itemID);
        }
        else{
            return utils.getsha256(utils.getStringFromKey(this.sellerID) + this.amount);
        }
    }
    public String makeJson(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
    public static transaction makeFromJson(String tJson){
        return new Gson().fromJson(tJson, transaction.class);
    }

}
