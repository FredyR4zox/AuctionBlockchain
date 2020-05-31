package pt.up.fc.dcc.ssd.auctionblockchain;

import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;

public class Utils {
    public static int[] twoLargest(int values[]){
        int largestA = Integer.MIN_VALUE, largestB = Integer.MIN_VALUE;
        int posA = -1, posB = -1;

        for(int i=0; i<values.length; i++) {
            if(values[i] > largestA) {
                largestB = largestA;
                posB=posA;
                largestA = values[i];
                posA=i;
            } else if (values[i] > largestB) {
                largestB = values[i];
                posB=i;
            }
        }
        return new int[] { largestA, posA, largestB, posB };
    }

    public static int largest(int values[]){
        int largest = Integer.MIN_VALUE;

        for (int value : values) {
            if (value > largest) {
                largest = value;
            }
        }
        return largest;
    }

    public static int largestIndex(int values[]){
        int largestIndex = -1;
        int largest = Integer.MIN_VALUE;

        for (int i=0; i<values.length; i++) {
            if(values[i] > largest){
                largest = values[i];
                largestIndex = i;
            }
        }
        return largestIndex;
    }

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

    static class transactionCompare implements Comparator<Transaction> {
        @Override
        public int compare(Transaction transaction, Transaction t1) {
            //check bigger transaction fee
            int result = Long.compare(t1.getTransactionFee(), transaction.getTransactionFee());
            if (result == 0){
                //check timestamp
                return Long.compare(transaction.getTimeStamp(), t1.getTimeStamp());
            }
            else return result;
        }
    }
}
