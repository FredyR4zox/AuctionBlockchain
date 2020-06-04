package pt.up.fc.dcc.ssd.auctionblockchain;

import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;

public class Utils {
    public static BigInteger[] twoLargest(BigInteger values[]){
        BigInteger largestA = BigInteger.valueOf(Integer.MIN_VALUE), largestB = BigInteger.valueOf(Integer.MIN_VALUE);
        BigInteger posA = BigInteger.valueOf(-1), posB = BigInteger.valueOf(-1);

        for(int i=0; i<values.length; i++) {
            if(values[i].compareTo(largestA)>0) {
                largestB = largestA;
                posB=posA;
                largestA = values[i];
                posA=BigInteger.valueOf(i);
            } else if (values[i].compareTo(largestB)>0) {
                largestB = values[i];
                posB=BigInteger.valueOf(i);
            }
        }
        return new BigInteger[] { largestA, posA, largestB, posB };
    }

    public static BigInteger largest(BigInteger values[]){
        BigInteger largest = BigInteger.valueOf(Integer.MIN_VALUE);

        for (BigInteger value : values) {
            if (value.compareTo(largest) > 0) {
                largest = value;
            }
        }
        return largest;
    }

    public static int largestIndex(BigInteger values[]){
        int largestIndex = -1;
        BigInteger largest = BigInteger.valueOf(Integer.MIN_VALUE);

        for (int i=0; i<values.length; i++) {
            if(values[i].compareTo(largest)>0){
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
        return new String(Hex.encode(hash));
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
