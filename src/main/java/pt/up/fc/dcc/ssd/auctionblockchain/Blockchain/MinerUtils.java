package pt.up.fc.dcc.ssd.auctionblockchain.Blockchain;

import pt.up.fc.dcc.ssd.auctionblockchain.Wallet;

import java.util.logging.Logger;

public class MinerUtils implements Runnable {
    public static Wallet miner;
    private static Thread alwaysMining;
    private static Thread miningThread;

    public static void startMining(Wallet miner){
        MinerUtils.miner=miner;
        MinerUtils object= new MinerUtils();
        alwaysMining = new Thread(object);
        alwaysMining.start();
    }

    public static void stopMining(){
        waitForMiningToFinish();
        alwaysMining.interrupt();
    }

    public static void waitForMiningToFinish(){
        if (miningThread==null){
            return;
        }
        try {
            miningThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while(true) {
            BlockChain longestChain = BlockchainUtils.original.getLongestChain();
            //if its the first one and has more than 2 transactions

            //or if the previous thread finishes and has more than two transactions
            Boolean enoughSize = longestChain.getUnconfirmedTransaction().size()>=BlockchainUtils.MIN_NR_TRANSACTIONS;
            if (miningThread==null && enoughSize) {
                System.out.println(longestChain.getUnconfirmedTransaction().size());
                Runnable r = new Mine(longestChain);
                miningThread = new Thread(r);
                miningThread.start();
            }
            else if ( miningThread!= null && enoughSize && !longestChain.isMining() && !miningThread.isAlive()){
                System.out.println(longestChain.getUnconfirmedTransaction().size());
                Runnable r = new Mine(longestChain);
                miningThread = new Thread(r);
                miningThread.start();
            }
            else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
            }
        }
    }
}
class Mine implements Runnable {
    private static final Logger logger = Logger.getLogger(Mine.class.getName());
    BlockChain miningChain;
    public Mine(BlockChain miningChain) {
        this.miningChain = miningChain;
    }

    public void run() {
        BlockchainUtils.original.tryResolveForks();
        miningChain.setMining(true);
        logger.info("Trying to mine a block\n");
        Block newBlock = miningChain.createBlock(MinerUtils.miner);
        if(newBlock==null) {
            miningChain.setMining(false);
            return;
        }
        if(newBlock.mineBlock(miningChain)){
            miningChain.addBlock(newBlock);
            logger.info("Added block: " + newBlock.getHash() + " to blockchain\n");
            miningChain.removeTransactionsFromTransPool(newBlock);
            //BlockchainUtils.getKademliaClient().announceNewBlock(newBlock);
        }
        miningChain.setMining(false);
    }
}
