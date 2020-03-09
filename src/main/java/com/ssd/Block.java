package com.ssd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Date;

public class Block{
    public String hash;
    public String previousHash;
    private transaction minersReward;
    private transaction[] data;
    private int nrTransactions; //last occupied value in data
    private long timeStamp;
    private long nonce;

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
        this.nrTransactions = -1; //because no transactions in the block
        this.previousHash = previousHash;
        this.data = new transaction[5];
        this.timeStamp = new Date().getTime();
        this.nonce = 0;
        this.hash = calculateHash();
    }

    public String calculateHash() {
        return utils.getsha256(this.previousHash + this.timeStamp + this.nonce + Arrays.toString(this.data));
    }

    public void mineBlock(int difficulty, Wallet minerWallet) {
        this.minersReward = new transaction(minerWallet.getPubKey());
        this.minersReward.signTransaction(minerWallet.getPrivKey());
        for (transaction trans: this.data
             ) {  if(trans != null && !trans.verifySignature()){
                 System.out.println("A signature doesn't match");
                 return;
        }

        }
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
