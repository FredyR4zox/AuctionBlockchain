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
        fundsTracking = new HashMap<String, Double>();
    }
    public Boolean addTransactionIfValid(transaction trans){
        //Do hashes check && Do signature checks
        if(!trans.verifyTransaction()) return false;
        addTransactionToFundsTracking(trans);
        if(!BlockChain.checkIfEnoughFunds(trans, fundsTracking)) return false;
        BlockChain.updateHashMapValues(trans, fundsTracking);
        this.newBlock.addTransaction(trans);
        return true;
    }
    private void addTransactionToFundsTracking(transaction trans){
        if(BlockChain.walletsMoney.containsKey(trans.buyerID)&&!fundsTracking.containsKey(trans.buyerID)){
            fundsTracking.put(trans.buyerID, BlockChain.walletsMoney.get(trans.buyerID));
        }
        if(BlockChain.walletsMoney.containsKey(trans.sellerID)&&!fundsTracking.containsKey(trans.sellerID)){
            fundsTracking.put(trans.sellerID,BlockChain.walletsMoney.get(trans.sellerID));
        }
    }
    public Boolean checkMineAddBlock(){
        //check if transactions in block are valid
        if(!newBlock.areSignaturesAndHashValid()) return false;
        //check block hash
        if(!newBlock.isHashValid()) return false;
        //Mine block
        newBlock.mineBlock(minerWallet);
        //Add minersReward to HashMap
        BlockChain.addMinerRewardToHashMap(newBlock.minersReward);
        //Add block to blockchain and update Hashmap
        BlockChain.addBlock(newBlock);
        for(int i= 0; i<=newBlock.nrTransactions; i++){
            transaction trans = newBlock.data[i];
            //Add transaction to HashMap
            BlockChain.updateHashMapValues(trans, BlockChain.walletsMoney);
        }

        return true;
    }
}
