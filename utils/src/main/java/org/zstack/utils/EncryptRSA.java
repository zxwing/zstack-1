package org.zstack.utils;

import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Created by mingjian.deng on 16/11/1.
 */
public class EncryptRSA {
    private static RSAPrivateKey privateKey;
    private static RSAPublicKey publicKey;
    private static final CLogger logger = Utils.getLogger(EncryptRSA.class);
    private static final String PRIVATE_RSA_KEY = "ansible/rsaKeys_java/RSAPrivate";
    private static final String PUBLIC_RSA_KEY = "ansible/rsaKeys_java/RSAPublic";

    static {
        try {
            ObjectInputStream in1 = new ObjectInputStream(
                    new FileInputStream(
                            PathUtil.findFileOnClassPath(PRIVATE_RSA_KEY, true).getAbsolutePath()));
            ObjectInputStream in2 = new ObjectInputStream(
                    new FileInputStream(
                            PathUtil.findFileOnClassPath(PUBLIC_RSA_KEY, true).getAbsolutePath()));
            privateKey = (RSAPrivateKey) in1.readObject();
            publicKey = (RSAPublicKey) in2.readObject();
            in1.close();
            in2.close();
        } catch (FileNotFoundException e) {
            logger.warn(e.getMessage());
            logger.warn("start to generate a new but temporary RSAPrivateKey/RSAPublicKey pair...");
            generate_rsa();
        } catch (Exception e){
            logger.error(e.getMessage());
            logger.error("init RSAPrivateKey or RSAPublicKey failed, system exit...");
            System.exit(1);
        }
    }

    private static void generate_rsa(){
        try{
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(512);
            KeyPair keyPair = keyPairGen.generateKeyPair();
            ObjectOutputStream out1 = new ObjectOutputStream(
                    new FileOutputStream("/root/RSAPrivate"));
            ObjectOutputStream out2 = new ObjectOutputStream(
                    new FileOutputStream("/root/RSAPublic"));

            privateKey = (RSAPrivateKey)keyPair.getPrivate();
            publicKey = (RSAPublicKey)keyPair.getPublic();
            out1.writeObject(privateKey);
            out2.writeObject(publicKey);
            out1.close();
            out2.close();
        }catch (Exception e){
            logger.error(e.getMessage());
            logger.error("generate RSAPrivateKey or RSAPublicKey failed, system exit...");
            System.exit(1);
        }
    }

    /**
     * encrypt
     * @param publicKey
     * @param srcBytes
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public byte[] encrypt(RSAPublicKey publicKey,byte[] srcBytes) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {
        if(publicKey!=null){
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
//            byte[] resultBytes = doEcrypt(cipher, srcBytes);
            logger.debug("yyyyyyyy");
            byte[] resultBytes = cipher.doFinal(srcBytes);
            logger.debug("xxxxxxxx");
            return resultBytes;
        }
        return null;
    }

    /**
     * avoid "Data must not be longer than 117 bytes" while encrypt
     * @param cipher
     * @param srcBytes
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws IOException
     */
    private byte[] doEcrypt(Cipher cipher, byte[] srcBytes) throws IOException, BadPaddingException, IllegalBlockSizeException {
        int inputLen = srcBytes.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] cache;
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > 99) {
                cache = cipher.doFinal(srcBytes, offSet, 99);
            } else {
                cache = cipher.doFinal(srcBytes, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            offSet += 99;
        }
        logger.debug(String.format("ddddddd: %d", out.toByteArray().length));
        out.close();
        return out.toByteArray();
    }

    /**
     * avoid "Data must not be longer than 128 bytes" while decrypt
     * @param privateKey
     * @param srcBytes
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public byte[] decrypt(RSAPrivateKey privateKey,byte[] srcBytes) throws BadPaddingException, IllegalBlockSizeException, IOException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        if(privateKey!=null){
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
//            byte[] resultBytes = doDecrypt(cipher, srcBytes);
            byte[] resultBytes = cipher.doFinal(srcBytes);
            return resultBytes;
        }
        return null;
    }

    /**
     *
     * @param cipher
     * @param srcBytes
     * @return
     * @throws IOException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    private byte[] doDecrypt(Cipher cipher, byte[] srcBytes) throws IOException, BadPaddingException, IllegalBlockSizeException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] cache;
        int inputLen = srcBytes.length;
        logger.debug(String.format("ssssss: %d", srcBytes.length));
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > 99) {
                cache = cipher.update(srcBytes, offSet, 99);
            } else {
                cache = cipher.doFinal(srcBytes, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            offSet += 99;
        }
        out.close();
        return out.toByteArray();
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }
}

