package pt.up.fc.dcc.ssd.auctionblockchain;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.logging.Logger;

public class Wallet {
    private static final Logger logger = Logger.getLogger(Wallet.class.getName());
    private String address;
    private PrivateKey privKey;
    private PublicKey pubKey;

    public Wallet() {
        generateKeys();
        createAddress();
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
        if(getAddressFromPubKey(pubKey).equals(address))
            return true;
        else {
            logger.warning("public key doesn't correspond to address");
            return false;
        }
    }
    public static String getAddressFromPubKey(PublicKey pubKey){
        return BlockchainUtils.getsha256(Base64.getEncoder().encodeToString(pubKey.getEncoded()));
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
            ecdsaSign.update(hash.getBytes(StandardCharsets.UTF_8));
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
            ecdsaVerify.update(hash.getBytes(StandardCharsets.UTF_8));
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
}
