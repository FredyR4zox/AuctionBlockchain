package com.ssd;

import com.google.gson.GsonBuilder;

import java.util.Arrays;
import java.util.Date;

public class Block{
    public String hash;
    public String previousHash;
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
        return utils.getsha256(previousHash + timeStamp + nonce + Arrays.toString(data));
    }

    public void mineBlock(int difficulty) {
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
}
