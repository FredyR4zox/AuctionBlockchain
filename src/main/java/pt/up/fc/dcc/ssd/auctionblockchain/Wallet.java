package pt.up.fc.dcc.ssd.auctionblockchain;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Scanner;
import java.util.logging.Logger;

public class Wallet {
    private static final Logger logger = Logger.getLogger(Wallet.class.getName());
    public static final String WALLETS_PATH="./wallets/";
    private String address;
    private PrivateKey privKey;
    private PublicKey pubKey;

    public Wallet() {
        generateKeys();
        createAddress();
    }

    public Wallet(String address, PublicKey pubKey, PrivateKey privKey) {
        this.address=address;
        this.privKey=privKey;
        this.pubKey=pubKey;
    }

    public PrivateKey getPrivKey() {
        return privKey;
    }

    public PublicKey getPubKey() {
        return pubKey;
    }

    public String getAddress() { return address; }

    private void createAddress(){
        this.address = getAddressFromPubKey(this.pubKey);
    }

    public static boolean checkAddress(PublicKey pubKey, String address){
        String tempAddress = getAddressFromPubKey(pubKey);
        if(getAddressFromPubKey(pubKey).equals(address))
            return true;
        else {
            logger.warning("public key doesn't correspond to address");
            return false;
        }
    }
    public static String getAddressFromPubKey(PublicKey pubKey){
        return Utils.bytesToHexString(Utils.getHash(pubKey.getEncoded()));
    }

    public void printKeys(){
        logger.info("public key:" + this.pubKey.toString());
        logger.info("private key:" + this.privKey.toString());
    }

    private void generateKeys() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", new BouncyCastleProvider()); //elliptic curve
            // Initialize the key generator and generate a KeyPair
            keyGen.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());   //recommended curve with 256 bytes of security
            KeyPair keyPair = keyGen.generateKeyPair();
            // Set the public and private keys from the keyPair
            this.privKey = keyPair.getPrivate();
            this.pubKey = keyPair.getPublic();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }

    public static byte[] signHash(PrivateKey privKey, String hash, Logger logger){
        byte[] signature = null;
        try {
            Signature ecdsaSign = Signature.getInstance("SHA256withECDSA");
            ecdsaSign.initSign(privKey);
            ecdsaSign.update(Utils.hexStringToBytes(hash));
            signature = ecdsaSign.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            logger.warning("There was an error signing the hash");
            e.printStackTrace();
        } finally {
            //careful errors that don't include the ones in the catch will go unnoticed
            return signature;
        }
    }

    public static Boolean verifySignature(byte[] signature, String hash, PublicKey pubKey, Logger logger){
        if(signature==null){
            logger.warning("Hash is not signed");
            return false;
        }
        try {
            Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA");
            ecdsaVerify.initVerify(pubKey);
            ecdsaVerify.update(Utils.hexStringToBytes(hash));
            ecdsaVerify.verify(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            logger.severe("Signatures don't match");
            e.printStackTrace();
            return false;
        } finally{
            //careful errors that don't include the ones in the catch will go unnoticed
            return true;
        }
    }


    public static PublicKey getPublicKeyFromBytes(byte[] bKey){
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(bKey);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC", new BouncyCastleProvider());
            return keyFactory.generatePublic(pubKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        logger.warning("Couldn't retrieve public key from bytes");
        return null;
    }

    public static byte[] getEncodedPublicKey(PublicKey publicKey){
        return publicKey.getEncoded();
    }

    public static PrivateKey getPrivateKeyFromBytes(byte[] pKey){
        PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(pKey);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC", new BouncyCastleProvider());
            return keyFactory.generatePrivate(privKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        logger.warning("Couldn't retrieve private key from bytes");
        return null;
    }

    public static byte[] getEncodedPrivateKey(PrivateKey privKey){
        return privKey.getEncoded();
    }


    //returns file name
    public static String createFileWithWallet(String filename, Wallet wallet){
        String pubKeyEncoded = Base64.getEncoder().encodeToString(Wallet.getEncodedPublicKey(wallet.getPubKey()));
        String privKeyEncoded = Base64.getEncoder().encodeToString(Wallet.getEncodedPrivateKey(wallet.getPrivKey()));
        String addressEncoded = Base64.getEncoder().encodeToString(wallet.getAddress().getBytes());
        File myWallet = new File(WALLETS_PATH + filename);
        try {
            if(!myWallet.createNewFile()) return null;
            FileWriter writeF = new FileWriter(WALLETS_PATH + filename);
            writeF.write(pubKeyEncoded + " " + privKeyEncoded + " " + addressEncoded);
            writeF.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Wallet createWalletFromFile(String filename){
        File myObj = new File(WALLETS_PATH + filename);
        try {
            Scanner myReader = new Scanner(myObj);
            String stringWallet = myReader.nextLine();
            String[] components = stringWallet.split(" ");
            PublicKey pubKey =  Wallet.getPublicKeyFromBytes(Base64.getDecoder().decode(components[0]));
            PrivateKey privKey =  Wallet.getPrivateKeyFromBytes(Base64.getDecoder().decode(components[1]));
            String address = new String(Base64.getDecoder().decode(components[2]));
            Wallet result = new Wallet(address, pubKey, privKey);
            return result;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
