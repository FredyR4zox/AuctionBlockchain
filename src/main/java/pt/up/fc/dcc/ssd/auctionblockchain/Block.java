package pt.up.fc.dcc.ssd.auctionblockchain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Arrays;
import java.util.Date;

public class Block{
    private static final Integer MAX_NR_TRANSACTIONS = 5;
    private String hash;
    private final String previousHash;
    private Transaction minersReward;
    private Transaction[] data;
    private int nrTransactions; //index of Transaction
    private final long timeStamp;
    private long nonce;

    public Block(String hash, String previousHash, Transaction minersReward, Transaction[] data, int nrTransactions, long timeStamp, long nonce) {
        this.hash = hash;
        this.previousHash = previousHash;
        this.minersReward = minersReward;
        this.data = data;
        this.nrTransactions = nrTransactions;
        this.timeStamp = timeStamp;
        this.nonce = nonce;
    }

    public Block(String previousHash) {
        this.nrTransactions = -1;
        this.previousHash = previousHash;
        this.data = new Transaction[MAX_NR_TRANSACTIONS];
        this.timeStamp = new Date().getTime();
        this.nonce = 0;
        this.hash = calculateHash();
    }

    public Boolean areTransactionFeesValid(){
        //check if miner reward with Transaction fees are valid
        //need to use different comparisons because of doubles inherent uncertainty
        if(Math.abs(this.getTransactionFeesTotal()-(this.minersReward.getAmount() - BlockChain.getMinerReward())) >= 0.000001) {
            System.out.println("Transaction fee amount is invalid");
            return false;
        }
        else return true;
    }

    public Boolean areSignaturesAndHashValid(){
        for (int i=0; i<=nrTransactions; i++) {
            Transaction trans = data[i];
            if(!trans.verifyTransaction()){
                return false;
            }
        }
        return true;
    }

    public String calculateHash() {
        return utils.getsha256(this.previousHash + this.minersReward + Arrays.toString(this.data) + this.nrTransactions + this.timeStamp + this.nonce);
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
        Double transactionFeesTotal = getTransactionFeesTotal();
        this.minersReward = new Transaction(minerWallet.getAddress(),transactionFeesTotal);

        String target = new String(new char[difficulty]).replace('\0', '0'); //Create a string with difficulty * "0"
        while(!this.hash.substring( 0, difficulty).equals(target)) {
            this.nonce ++;
            this.hash = calculateHash();
        }
        System.out.println("Block Mined!!! : " + this.hash);
    }

    public Double getTransactionFeesTotal() {
        Double total= 0.0;
        for(int i= 0; i<=nrTransactions; i++){
            Transaction trans = data[i];
            //Add Transaction fee to total
            total += trans.getTransactionFee();
        }
        return total;
    }

    public Boolean addTransaction(Transaction newtrans){
        if (this.nrTransactions==MAX_NR_TRANSACTIONS-1){
            System.out.println("Maximum number of transactions reached.");
            return false;
        }
        else{
            this.nrTransactions++;
            this.data[this.nrTransactions]=newtrans;
            //recalculate hash
            this.hash= calculateHash();
            return true;
        }
    }

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

    public Transaction getXData(int x) {
        return data[x];
    }

    public int getNrTransactions() {
        return nrTransactions;
    }
}
