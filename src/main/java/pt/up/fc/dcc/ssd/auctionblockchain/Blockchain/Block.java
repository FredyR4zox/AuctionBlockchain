package pt.up.fc.dcc.ssd.auctionblockchain.Blockchain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jdk.jshell.execution.Util;
import pt.up.fc.dcc.ssd.auctionblockchain.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Logger;

public class Block{
    private static final Logger logger = Logger.getLogger(Block.class.getName());

    private BigInteger previousWork;
    private String hash;
    private final String previousHash;
    private Transaction minersReward;
    private Transaction[] data;
    private int nrTransactions; //index of Transaction
    private final long timeStamp;
    private final int difficulty;
    private long nonce;

    public Block(String hash, String previousHash, Transaction minersReward, Transaction[] data, int difficulty, long timeStamp, long nonce, BigInteger previousWork) {
        this.hash = hash;
        this.previousHash = previousHash;
        this.minersReward = minersReward;
        this.data = data;
        for(Transaction trans: this.data){
            if (trans!=null) this.nrTransactions++;
        }
        this.timeStamp = timeStamp;
        this.difficulty = difficulty;
        this.nonce = nonce;
        this.previousWork = previousWork;
    }

    //hash is initialized but is calculated while mining
    public Block(String previousHash, BigInteger previousWork) {
        this.previousHash = previousHash;
        this.data = new Transaction[BlockchainUtils.MAX_NR_TRANSACTIONS];
        this.timeStamp = new Date().getTime();
        this.difficulty = BlockchainUtils.difficulty;
        this.previousWork=previousWork;
        //cant initialize with zeroes
        this.hash = this.calculateHash();
    }

    @Override
    public Block clone(){
        Block newBlock = new Block(this.hash, this.previousHash, this.minersReward, this.data, this.difficulty, this.timeStamp, this.nonce, this.previousWork);
        return newBlock;
    }

    public Boolean checkTransactions(){
        //also check transaction fees
        long total = 0;
        for (int i=0; i<nrTransactions; i++) {
            Transaction trans = data[i];
            if(!trans.verifyTransaction()){
                return false;
            }
            total += trans.getBid().getFee();
        }
        //check if miner reward with Transaction fees are valid
        if(total != this.getMinersReward().getBid().getAmount() - BlockchainUtils.getMinerReward()){
            logger.warning("transaction fee in miner transaction doesn't match");
            return false;
        }
        return true;
    }

    public long getTransactionFeesTotal() {
        long total = 0;
        for(int i= 0; i < nrTransactions; i++){
            Transaction trans = data[i];
            //Add Transaction fee to total
            total += trans.getBid().getFee();
        }
        return total;
    }

    public String calculateHash() {
        String transData = "";
        for(int i = 0; i< this.nrTransactions; i++){
            transData = transData.concat(data[i].getHash());
        }
        String transSHA = Utils.getHash(transData);
        return Utils.getHash("" + this.previousHash + this.minersReward.getHash() + transSHA + this.nrTransactions + this.timeStamp + this.difficulty + this.nonce + this.previousWork);
    }

    public Boolean checkBlock(){
        if(!this.checkTransactions()){return false;}
        if(!this.isHashValid()){return false;}
        return true;
    }

    public Boolean isHashValid() {
        //compare registered hash and calculated hash:
        //also check according to difficulty
        String target = new String(new char[this.difficulty]).replace('\0', '0'); //Create a string with difficulty * "0"
        String hashTemp = calculateHash();
        boolean output1 = hash.equals(hashTemp);
        boolean output2 = this.hash.substring(0, this.difficulty).equals(target);
        if(!hash.equals(calculateHash()) || !this.hash.substring(0, this.difficulty).equals(target)){
            logger.warning("Hash is not correct");
            return false;
        }

        return true;
    }

    public Boolean mineBlock(BlockChain curBlockchain) {

        String target = new String(new char[difficulty]).replace('\0', '0'); //Create a string with difficulty * "0"
        while(!this.hash.substring(0, difficulty).equals(target)) {
            this.nonce++;
            this.hash = calculateHash();
            if(this.nonce%10000==0){
                if(!curBlockchain.isMining()){
                    logger.warning("A newer block was added while mining");
                    return false;
                }
            }
        }
        if(!curBlockchain.isMining()){
            logger.warning("A newer block was added while mining");
            return false;
        }
        return true;
    }

    public Boolean mineGenesisBlock(Wallet minerWallet) {
        long transactionFeesTotal = getTransactionFeesTotal();
        this.minersReward = new Transaction(minerWallet, transactionFeesTotal);

        String target = new String(new char[difficulty]).replace('\0', '0'); //Create a string with difficulty * "0"
        while(!this.hash.substring(0, difficulty).equals(target)) {
            this.nonce++;
            this.hash = calculateHash();
        }
        return true;
    }

    public Boolean addTransaction(Transaction newtrans){
        if (this.nrTransactions == BlockchainUtils.MAX_NR_TRANSACTIONS){
            System.out.println("Maximum number of transactions reached.");
            return false;
        }

        this.data[this.nrTransactions] = newtrans;
        this.nrTransactions++;

        //recalculate hash
        this.hash= calculateHash();

        return true;
    }

    public void removeLastTransaction(){
        //this.data[nrTransactions-1]=null;
        this.nrTransactions--;

        //recalculate hash
        this.hash= calculateHash();
    }

//    public Boolean addTransactionIfValid(Transaction trans, BlockChain curBlockchain){
//        //Do hashes check && Do signature checks
//        if(!trans.verifyTransaction()) return false;
//        if(!this.addTransaction(trans))return false;
//        return true;
//    }

    public String makeJson(){
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
    public static Block makeFromJson(String bJson){
        return new Gson().fromJson(bJson, Block.class);
    }

    public String getHash() {
        return hash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public Transaction getMinersReward() {
        return minersReward;
    }

    public Transaction[] getData() {
        return data;
    }

    public int getNrTransactions() {
        return nrTransactions;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public BigInteger getBigDifficulty(){return BigInteger.valueOf(difficulty); }
    public long getNonce() {
        return nonce;
    }

    public Transaction getXData(int x) {
        if(x < 0 || x >= nrTransactions)
            return null;

        return data[x];
    }
    public void setMinersReward(Transaction minersReward) {
        this.minersReward = minersReward;
    }

    public BigInteger getPreviousWork() {
        return previousWork;
    }

    public void setPreviousWork(BigInteger previousWork) {
        this.previousWork = previousWork;
    }

    public void setNonce(long nonce) {
        this.nonce = nonce;
    }
}
