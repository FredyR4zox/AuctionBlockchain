package pt.up.fc.dcc.ssd.auctionblockchain;

import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Comparator;
import java.util.logging.Logger;

public class Utils {
    public static final Charset charset = Charset.forName("ASCII");
    public static final String hashAlgorithm = "SHA-1";
    public static final int hashAlgorithmLengthInBits = 160;
    public static final int hashAlgorithmLengthInBytes = hashAlgorithmLengthInBits/8;
    public static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(charset);

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

    public static String getHash(String input){
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance(hashAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        assert digest != null;
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return new String(Hex.encode(hash));
    }

    public static String randomString(int size){
        // chose a Character random from this String
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789"
                + "abcdefghijklmnopqrstuvxyz";

        // create StringBuffer size of AlphaNumericString
        StringBuilder sb = new StringBuilder(size);

        for (int i = 0; i < size; i++) {

            // generate a random number between
            // 0 to AlphaNumericString variable length
            int index
                    = (int)(AlphaNumericString.length()
                    * Math.random());

            // add Character one by one in end of sb
            sb.append(AlphaNumericString
                    .charAt(index));
        }

        return sb.toString();
    }

    public static Boolean verifyAmountIncrement(long prevValue, long newValue, float incPercentage, Logger logger) {
        long increment = newValue-prevValue;
        long minIncrement= (long) (incPercentage * prevValue)/100;
        if(increment>=minIncrement){
            return true;
        }else{
            logger.warning("Bid doesn't have sufficient increment");
            return false;
        }
    }

    public static String getStandardString() {
        return "00000000000000000000";
    }

    public static String bytesToHexString(byte[] bytes) {
        int len = bytes.length;
        byte[] hexChars = new byte[len * 2];

        for (int j = 0; j < len; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }

        return new String(hexChars, StandardCharsets.US_ASCII);
    }

    public static byte[] hexStringToBytes(String string) {
        int len = string.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(string.charAt(i), 16) << 4)
                    + Character.digit(string.charAt(i+1), 16));
        }

        return data;
    }
}
