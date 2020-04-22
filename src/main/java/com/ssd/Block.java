package com.ssd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Date;

public class Block{
    public String hash;
    public String previousHash;
    public transaction minersReward;
    public transaction[] data;
    public int nrTransactions; //last occupied value in data
    public long timeStamp;
    public long nonce;

    public Block(transaction data, String previousHash) {
        this.nrTransactions = 0;
        this.previousHash = previousHash;
        this.data = new transaction[5];
        this.data[0] = data;
        this.timeStamp = new Date().getTime();
        this.nonce = 0;
        this.hash = calculateHash();
    }

    public Block(String previousHash) {
        this.nrTransactions = -1; //pretend its full
        this.previousHash = previousHash;
        this.data = new transaction[5];
        this.timeStamp = new Date().getTime();
        this.nonce = 0;
        this.hash = calculateHash();
    }

    public Boolean areSignaturesAndHashValid(){
        for (int i=0; i<=nrTransactions; i++) {
            transaction trans = data[i];
            if(!trans.verifyTransaction()){
                return false;
            }
        }
        return true;
    }
    public String calculateHash() {
        return utils.getsha256(this.previousHash + this.timeStamp + this.nonce + Arrays.toString(this.data) + this.minersReward + this.nrTransactions);
    }

    public Boolean isHashValid() {
        //compare registered hash and calculated hash:
        if(!hash.equals(calculateHash()) ){
            System.out.println("Current Hashes not equal");
            return false;
        }
        else return true;
    }

    public void mineBlock(Wallet minerWallet) {
        int difficulty = BlockChain.getDifficulty();
        this.minersReward = new transaction(minerWallet.getAddress());

        String target = new String(new char[difficulty]).replace('\0', '0'); //Create a string with difficulty * "0"
        while(!this.hash.substring( 0, difficulty).equals(target)) {
            this.nonce ++;
            this.hash = calculateHash();
        }
        System.out.println("Block Mined!!! : " + this.hash);
    }

    public void addTransaction(transaction newtrans){
        if (this.nrTransactions==4){
            System.out.println("Maximum number of transactions reached.");
        }
        else{
            this.nrTransactions++;
            this.data[this.nrTransactions]=newtrans;
        }
    }

    public String makeJson(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
    public static Block makeFromJson(String bJson){
        return new Gson().fromJson(bJson, Block.class);
    }
}
