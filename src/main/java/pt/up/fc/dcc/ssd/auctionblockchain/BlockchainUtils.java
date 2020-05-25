package pt.up.fc.dcc.ssd.auctionblockchain;

import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import java.util.TreeSet;
import java.util.logging.Logger;

public class BlockchainUtils {
    public static final int MAX_NR_TRANSACTIONS = 5;
    public static final int difficulty = 5;
    public static final long minerReward = 100;

    public static String getsha256(String input){
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        assert digest != null;
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        String sha256hex = new String(Hex.encode(hash));
        return sha256hex;
    }

    public static Boolean addTransactionIfValidToPool(Transaction trans, TreeSet<Transaction> transPool, Logger logger){
        //Do hashes check && Do signature checks
        if(!trans.verifyTransaction()){
            logger.warning("There was an attempt to add an invalid transaction to pool");
            return false;
        }
        if(!BlockChain.checkIfEnoughFunds(trans, BlockChain.walletsMoney)) {
            logger.warning("There was an attempt to add a transaction with insufficient funds to pool");
            return false;
        }
        if(!transPool.add(trans)){
            logger.warning("Transaction already exists in transaction pool");
            return false;
        }
        return true;
    }
}
