package pt.up.fc.dcc.ssd.auctionblockchain.Blockchain;

import pt.up.fc.dcc.ssd.auctionblockchain.Kademlia.KademliaClient;
import pt.up.fc.dcc.ssd.auctionblockchain.Utils;
import pt.up.fc.dcc.ssd.auctionblockchain.Wallet;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

public class BlockchainUtils{
    private static final Logger logger = Logger.getLogger(BlockchainUtils.class.getName());
    public static final int MAX_NR_TRANSACTIONS = 5;
    public static final int difficulty = 2;
    public static final long minerReward = 100;
    public static final int MIN_NR_TRANSACTIONS = 1;
    public static final BigInteger WORK_RESOLVE_SPLIT = BigInteger.valueOf(4);
    public static final BlockChain original = new BlockChain();
    private static KademliaClient kademliaClient;

    public static Boolean addBlock(Block newBlock){
        return original.addBlockToCorrectChain(newBlock);
    }

    public static Block getBlockWithPreviousHash(String hash){
        BlockChain longest= getLongestChain();
        return longest.getBlocksPreviousHashes().get(hash);
    }

    //adds transactions to all chains terminations
    public static Boolean addTransaction(Transaction trans){
        return original.addTransactionToCorrectChains(trans);
    }

    public static void createGenesisBlock(Wallet creator){
        Block genesis= new Block("0", BigInteger.valueOf(0));
        //Mine block
        genesis.mineGenesisBlock(creator);
        original.addBlock(genesis);
        logger.info("created genesis block");
    }

    public static BlockChain getLongestChain(){
        return original.getLongestChain();
    }

    public static long getMinerReward() {
        return minerReward;
    }

    public static BlockChain largestChain(BlockChain[] chains) {
        BigInteger[] chainsWork = new BigInteger[chains.length];
        for(int i=0; i< chains.length; i++){
            chainsWork[i]=chains[i].getWork();
        }
        int largestIndex = Utils.largestIndex(chainsWork);
        return chains[largestIndex];
    }

    public static BlockChain getOriginal() {
        return original;
    }

    public static KademliaClient getKademliaClient() {
        return kademliaClient;
    }

    public static int getMinNrTransactions() {
        return MIN_NR_TRANSACTIONS;
    }

    static class transactionCompare implements Comparator<Transaction> {
        @Override
        public int compare(Transaction transaction, Transaction t1) {
            //check bigger transaction fee
            int result = Long.compare(t1.getBid().getFee(), transaction.getBid().getFee());
            if (result == 0){
                //check timestamp
                return Long.compare(transaction.getTimeStamp(), t1.getTimeStamp());
            }
            else return result;
        }
    }

}
