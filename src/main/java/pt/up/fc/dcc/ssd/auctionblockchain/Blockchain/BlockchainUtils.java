package pt.up.fc.dcc.ssd.auctionblockchain.Blockchain;

import pt.up.fc.dcc.ssd.auctionblockchain.Kademlia.KademliaClient;
import pt.up.fc.dcc.ssd.auctionblockchain.Utils;
import pt.up.fc.dcc.ssd.auctionblockchain.Wallet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.logging.Logger;

public class BlockchainUtils{
    private static final Logger logger = Logger.getLogger(BlockchainUtils.class.getName());
    public static final int MAX_NR_TRANSACTIONS = 3;
    public static final int difficulty = 5;
    public static final long minerReward = 100;
    public static final int MIN_NR_TRANSACTIONS = 2;
    public static final BigInteger WORK_RESOLVE_SPLIT = BigInteger.valueOf(4);
    public static BlockChain original = new BlockChain();
    private static final String CHAIN_FILEPATH = "./chain/";
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
        if(!trans.verifyTransaction()){
            return false;
        }
        return original.addTransactionToCorrectChains(trans);
    }

    public static void createGenesisBlock(Wallet creator){
        Block genesis = new Block(Utils.getStandardString(), BigInteger.valueOf(0));
        //Mine block
        genesis.mineGenesisBlock(creator);
        original.addBlock(genesis);
        logger.info("created genesis block");
        BlockchainUtils.getKademliaClient().announceNewBlock(genesis);
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

    public static void setKademliaClient(KademliaClient client){
        kademliaClient = client;
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
    public static Boolean createBlockChain(List<Block> chain){
        original = new BlockChain();
        Block currentBlock = null;
        Block previousBlock;
        //loop through chain
        for(Block block: chain) {
            previousBlock=currentBlock;
            currentBlock=block;
            //Do Hashes check
            if (previousBlock==null){
                if(!currentBlock.isHashValid()) return false;
            }
            else{
                if (!original.areHashesValid(currentBlock, previousBlock)) return false;
            }
            if(!original.checkAddBlock(currentBlock)){
                logger.severe("Errors in saved blockchain");
                return false;
            }
        }
        logger.info("Chain was validated\n");
        return true;
    }

    public static Boolean saveBlockchainInFile(String filename){
        File mychain = new File(CHAIN_FILEPATH + filename);
        try {
            mychain.createNewFile();
            FileWriter writeF = new FileWriter(CHAIN_FILEPATH + filename);
            writeF.write(original.makeJson());
            writeF.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    //TODO fix this function
    public static void makeBlockchainFromFile(String filename){
        File mychain = new File(CHAIN_FILEPATH + filename);
        try {
            Scanner myReader = new Scanner(mychain);
            String chain = myReader.nextLine();
            ArrayList<Block> blockchain = original.makeFromJson(chain);
            createBlockChain(blockchain);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
