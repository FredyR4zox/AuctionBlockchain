package com.ssd;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class Wallet {
    public String address;
    public PrivateKey privKey;
    public PublicKey pubKey;

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
        this.address = utils.getsha256(utils.getStringFromKey(this.pubKey));
    }

    public static boolean checkAddress(PublicKey pubKey, String address){
        if(getAddressFromPubKey(pubKey).equals(address))
            return true;
        else {
            System.out.println("public key doesn't correspond to address");
            return false;
        }
    }
    public static String getAddressFromPubKey(PublicKey pubKey){
        return utils.getsha256(utils.getStringFromKey(pubKey));
    }

    public void printKeys(){
        System.out.println("public key:" + this.pubKey.toString());
        System.out.println("private key:" + this.privKey.toString());
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
    public static PublicKey getPublicKeyFromBytes(byte[] bKey){
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(bKey);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC", new BouncyCastleProvider());
            return keyFactory.generatePublic(pubKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        System.out.println("Couldnt retrieve public key from bytes");
        return null;
    }

    public static byte[] getEncodedPublicKey(PublicKey publicKey){
        return publicKey.getEncoded();
    }

}
