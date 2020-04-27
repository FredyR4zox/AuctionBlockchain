package com.ssd;

import java.util.HashMap;

public class addBlock {
    //class to manage adding blocks and checking transactions by miners
    HashMap<String, Double> fundsTracking;
    Block newBlock;
    Wallet minerWallet;
    public addBlock(Wallet minerWallet){
        this.minerWallet = minerWallet;
        newBlock = new Block(BlockChain.getLastHash());
        fundsTracking = new HashMap<>();
    }
    public Boolean addTransactionIfValid(Transaction trans){
        //Do hashes check && Do signature checks
        if(!trans.verifyTransaction()) return false;
        addTransactionToFundsTracking(trans);
        if(!BlockChain.checkIfEnoughFunds(trans, fundsTracking)) return false;
        if(!this.newBlock.addTransaction(trans))return false;
        BlockChain.updateHashMapValues(trans, fundsTracking);
        return true;
    }
    private void addTransactionToFundsTracking(Transaction trans){
        if(BlockChain.walletsMoney.containsKey(trans.getBuyerID())&&!fundsTracking.containsKey(trans.getBuyerID())){
            fundsTracking.put(trans.getBuyerID(), BlockChain.walletsMoney.get(trans.getBuyerID()));
        }
        if(BlockChain.walletsMoney.containsKey(trans.getSellerID())&&!fundsTracking.containsKey(trans.getSellerID())){
            fundsTracking.put(trans.getSellerID(),BlockChain.walletsMoney.get(trans.getSellerID()));
        }
    }
    public Boolean checkMineAddBlock(){
        //check if transactions in block are valid
        if(!newBlock.areSignaturesAndHashValid()) return false;
        //check block hash
        if(!newBlock.isHashValid()) return false;
        //Mine block
        newBlock.mineBlock(minerWallet);
        //Add block to blockchain and update Hashmap
        BlockChain.addBlock(newBlock);

        return true;
    }
    public void reset(){
        newBlock = new Block(BlockChain.getLastHash());
        fundsTracking.clear();
    }
}
