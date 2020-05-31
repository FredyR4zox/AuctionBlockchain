package pt.up.fc.dcc.ssd.auctionblockchain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Arrays;
import java.util.Date;
import java.util.logging.Logger;

public class Block{
    private static final Logger logger = Logger.getLogger(Block.class.getName());

    private String hash;
    private final String previousHash;
    private Transaction minersReward;
    private Transaction[] data;
    private int nrTransactions; //index of Transaction
    private final long timeStamp;
    private int difficulty;
    private long nonce;

    public Block(String hash, String previousHash, Transaction minersReward, Transaction[] data, int difficulty, long timeStamp, long nonce) {
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
    }

    //hash is initialized but is calculated while mining
    public Block(String previousHash) {
        this.previousHash = previousHash;
        this.data = new Transaction[BlockchainUtils.MAX_NR_TRANSACTIONS];
        this.timeStamp = new Date().getTime();
        this.difficulty = BlockchainUtils.difficulty;
        //cant initialize with zeroes
        this.hash = "11111111";
    }

    @Override
    public Block clone(){
        Block newBlock = new Block(this.hash, this.previousHash, this.minersReward, this.data, this.difficulty, this.timeStamp, this.nonce);
        return newBlock;
    }

    public Boolean areTransactionFeesValid(){
        //check if miner reward with Transaction fees are valid
        //Percorrer lista de transações e somar os fees
        if(this.getTransactionFeesTotal() != (minersReward.getAmount() - BlockchainUtils.getMinerReward())) {
            logger.warning("Transaction fee amount is invalid");
            return false;
        }

        return true;
    }

    public Boolean areSignaturesAndHashValid(){
        for (int i=0; i<nrTransactions; i++) {
            Transaction trans = data[i];
            if(!trans.verifyTransaction()){
                return false;
            }
        }
        return true;
    }

    public String calculateHash() {
        return Utils.getsha256(this.previousHash + this.minersReward + Arrays.toString(this.data) + this.nrTransactions + this.timeStamp + this.difficulty + this.nonce);
    }

    public Boolean isHashValid() {
        //compare registered hash and calculated hash:
        if(!hash.equals(calculateHash()) ){
            System.out.println("Current Hashes not equal");
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
        this.minersReward = new Transaction(minerWallet.getAddress(), transactionFeesTotal);

        String target = new String(new char[difficulty]).replace('\0', '0'); //Create a string with difficulty * "0"
        while(!this.hash.substring(0, difficulty).equals(target)) {
            this.nonce++;
            this.hash = calculateHash();
        }
        return true;
    }

    public long getTransactionFeesTotal() {
        long total = 0;
        for(int i= 0; i < nrTransactions; i++){
            Transaction trans = data[i];
            //Add Transaction fee to total
            total += trans.getTransactionFee();
        }
        return total;
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
}
