package com.ssd;

import com.google.gson.GsonBuilder;

public class transaction {
    public long sellerID;
    public long buyerID;
    public long amount;

    public transaction(long sellerID, long buyerID, long amount) {
        this.sellerID = sellerID;
        this.buyerID = buyerID;
        this.amount = amount;
    }

    public Boolean verifyTransaction() {
        //check if buyer has the amount of
        return true;
    }
    public String makeJson(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }

}
