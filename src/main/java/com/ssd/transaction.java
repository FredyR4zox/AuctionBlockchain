package com.ssd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.security.*;

public class transaction {
    public String sellerID;
    public String buyerID;
    public Double amount;
    public Double transactionFee;
    public long itemID;
    public PublicKey buyerPublicKey;
    public byte[] signature;
    public String hash;

    public transaction(String buyerID, String sellerID, PublicKey buyerPublicKey, double amount, double transactionFee, long itemID) {
        this.buyerID = buyerID;
        this.sellerID = sellerID;
        this.buyerPublicKey= buyerPublicKey;
        this.amount = amount;
        this.transactionFee = transactionFee;
        this.itemID = itemID;
        this.hash = this.getHashToBeSigned();
    }

    //coinbase transaction for miner reward
    public transaction(String sellerID) {
        this.sellerID = sellerID;
        this.buyerID = "0000000000000000000000000000000000000000000000000000000000000000";
        this.amount = BlockChain.getMinerReward();
        this.hash = this.getHashToBeSigned();
    }

    public String getAddressFromPubKey(PublicKey pubKey){
        utils.getsha256(utils.getStringFromKey(pubKey));
        return hash;
    }
    public boolean checkAddress(PublicKey pubKey){
        if(getAddressFromPubKey(pubKey).equals(this.buyerID))
            return true;
        else
            return false;
    }

    public Boolean verifyTransaction() {
        //Do hashes check && Do signature checks
        if(isHashValid()&&verifySignature()) return true;
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
        if(this.buyerPublicKey != null) {
            return utils.getsha256(this.sellerID + this.buyerID + utils.getStringFromKey(this.buyerPublicKey) + this.amount + this.transactionFee + this.itemID);
        }
        else{
            return utils.getsha256(this.sellerID + this.buyerID + this.amount);
        }
    }
    public String makeJson(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
    public static transaction makeFromJson(String tJson){
        return new Gson().fromJson(tJson, transaction.class);
    }

}
