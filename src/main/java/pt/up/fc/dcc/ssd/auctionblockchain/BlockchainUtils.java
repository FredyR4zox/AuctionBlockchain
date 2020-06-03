package pt.up.fc.dcc.ssd.auctionblockchain;

import java.math.BigInteger;
import java.util.logging.Logger;

public class BlockchainUtils {
    private static final Logger logger = Logger.getLogger(BlockchainUtils.class.getName());
    public static final int MAX_NR_TRANSACTIONS = 5;
    public static final int difficulty = 2;
    public static final long minerReward = 100;
    public static final int MIN_NR_TRANSACTIONS = 3 ;
    public static final BigInteger WORK_RESOLVE_SPLIT = BigInteger.valueOf(4);
    public static final BlockChain original = new BlockChain();
    private static Block newBlock;

    public static Boolean addBlock(Block newBlock){
        return original.addBlockToCorrectChain(newBlock);
    }

    public static void mineBlock(Wallet miner){
        original.tryResolveForks();
        BlockChain longest= getLongestChain();
        newBlock = longest.createBlock(miner);
        if (newBlock==null){
            longest.setMining(false);
            return;
        }
        Thread thread = new Thread(longest);
        thread.start();
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

    public static Block getNewBlock() {
        return newBlock;
    }

    public static BlockChain getOriginal() {
        return original;
    }
}
