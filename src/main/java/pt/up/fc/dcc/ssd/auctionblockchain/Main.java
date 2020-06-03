package pt.up.fc.dcc.ssd.auctionblockchain;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

import static java.lang.Thread.sleep;

public class Main {


        public static void main(String[] args) {
                Security.addProvider(new BouncyCastleProvider());
                long id = 0;

                Wallet creator= new Wallet();
                System.out.println("creator address:" + creator.getAddress());
                Wallet miner = new Wallet();
                System.out.println("miner address:" + miner.getAddress());
                Wallet wallet1 = new Wallet();
                System.out.println("wallet1 address:" + wallet1.getAddress());
                Wallet wallet2 = new Wallet();
                System.out.println("wallet2 address:" + wallet2.getAddress());
                Wallet alice = new Wallet();
                System.out.println("alice address:" + alice.getAddress());
                Wallet bob = new Wallet();
                System.out.println("bob address:" + bob.getAddress());

                BlockchainUtils.createGenesisBlock(creator);
                BlockchainUtils.getOriginal().printHashMap();
                System.out.println();


                Transaction trans10 = new Transaction(creator, wallet1.getAddress(), 10, 2, ++id);
                Transaction trans11 = new Transaction(creator, wallet2.getAddress(), 20, 1, ++id);
                Transaction trans12 = new Transaction(creator, bob.getAddress(), 20, 1, ++id);

                BlockchainUtils.addTransaction(trans10);
                BlockchainUtils.addTransaction(trans11);
                BlockchainUtils.addTransaction(trans12);

                BlockchainUtils.mineBlock(miner);
                try {
                        sleep(1000);
                } catch (InterruptedException e) {
                        e.printStackTrace();
                }
                //BlockchainUtils.getOriginal().printHashMap();

                for(int i=0; i<0;i++) {
                       // ableToAdd = minerAddition.addTransactionIfValidToPool(trans11);
                }

                Transaction trans20 = new Transaction(miner, alice.getAddress(), 30, 2, ++id);
                BlockchainUtils.addTransaction(trans20);
                Transaction trans21 = new Transaction(miner, alice.getAddress(), 60, 2, ++id);
                BlockchainUtils.addTransaction(trans21);

                BlockchainUtils.mineBlock(creator);
                try {
                        sleep(1000);
                } catch (InterruptedException e) {
                        e.printStackTrace();
                }

                //BlockchainUtils.getOriginal().printHashMap();

                //create mock for test
                BlockChain original = BlockchainUtils.getOriginal();
                Block conflictingBlock =  createFakeBlock(original);
                //actual test
                BlockchainUtils.addBlock(conflictingBlock);
                //BlockchainUtils.addBlock(conflictingBlock);

                Transaction trans30 = new Transaction(alice, bob.getAddress(), 10 , 1, ++id);
                Transaction trans31 = new Transaction(alice, bob.getAddress(), 10 , 1, ++id);
                BlockchainUtils.addTransaction(trans30);
                BlockchainUtils.addTransaction(trans31);
                BlockchainUtils.mineBlock(bob);
                try {
                        sleep(1000);
                } catch (InterruptedException e) {
                        e.printStackTrace();
                }


                Transaction trans40 = new Transaction(bob, creator.getAddress(), 30, 2,++id);
                BlockchainUtils.addTransaction(trans40);
                BlockChain big = original.getLongestChain();
                conflictingBlock = createFakeBlock(big);
                BlockchainUtils.addBlock(conflictingBlock);
                BlockchainUtils.mineBlock(alice);
                try {
                        sleep(1000);
                } catch (InterruptedException e) {
                        e.printStackTrace();
                }

                Boolean output;
                output = BlockchainUtils.addBlock(conflictingBlock);
                System.out.println(output);

                original.tryResolveForks();
                //String BlockChainJson = BlockChain.makeJson();
                //System.out.println(BlockChainJson);

//                Auction auction = new Auction(0, 60, creator);
//                Boolean output= auction.verifyAuction();
//                System.out.println(output);
        }
        public static Block createFakeBlock(BlockChain branch){
                Block lastBlock= branch.getXBlock(branch.getBlockchain().size());
                Block conflictingBlock = lastBlock.clone();
                conflictingBlock.removeLastTransaction();
                branch.setMining(true);
                Boolean output = conflictingBlock.mineBlock(branch);
                branch.setMining(false);
                return conflictingBlock;
        }
}

