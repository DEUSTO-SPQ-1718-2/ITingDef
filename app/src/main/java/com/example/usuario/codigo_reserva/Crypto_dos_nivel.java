package com.example.usuario.codigo_reserva;

import android.util.Base64;
import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypto_dos_nivel {

    private static final String TAG = Crypto_dos_nivel.class.getSimpleName();

    public static final String PKCS12_DERIVATION_ALGORITHM = "PBEWITHSHA256AND256BITAES-CBC-BC";
    public static final String PBKDF2_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

    private static String DELIMITER = "]";

    private static int KEY_LENGTH = 256;
    // minimum values recommended by PKCS#5, increase as necessary
    private static int ITERATION_COUNT = 1000;
    private static final int PKCS5_SALT_LENGTH = 8;

    private static final Logger log = LoggerFactory.getLogger(Gestion_dos_nivel.class);

    private static SecureRandom random = new SecureRandom();

    private Crypto_dos_nivel() {
    }

    public static SecretKey deriveKeyPbkdf2(byte[] salt, String password) {
        try {
            long start = System.currentTimeMillis();
            KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt,
                    ITERATION_COUNT, KEY_LENGTH);
            SecretKeyFactory keyFactory = SecretKeyFactory
                    .getInstance(PBKDF2_DERIVATION_ALGORITHM);
            byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
            //Log.d(TAG, "key bytes: " + toHex(keyBytes));
            log.debug("Bytes de la clave: "+toHex(keyBytes));


            SecretKey result = new SecretKeySpec(keyBytes, "AES");
            long elapsed = System.currentTimeMillis() - start;
           // Log.d(TAG, String.format("PBKDF2 key derivation took %d [ms].", elapsed));
            log.debug("La derivación PBKDF2 de la clave ha llevado %d [ms]", elapsed);

            return result;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] generateIv(int length) {
        byte[] b = new byte[length];
        random.nextBytes(b);

        return b;
    }

    public static byte[] generateSalt() {
        byte[] b = new byte[PKCS5_SALT_LENGTH];
        random.nextBytes(b);

        return b;
    }



    public static String encrypt(String plaintext, SecretKey key, byte[] salt) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);

            byte[] iv = generateIv(cipher.getBlockSize());
            //Log.d(TAG, "IV: " + toHex(iv));
            log.debug("IV: "+ toHex(iv));

            IvParameterSpec ivParams = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivParams);
            //Log.d(TAG, "Cipher IV: "+ (cipher.getIV() == null ? null : toHex(cipher.getIV())));
            log.debug("Cifrado IV: "
                    + (cipher.getIV() == null ? null : toHex(cipher.getIV())));

            byte[] cipherText = cipher.doFinal(plaintext.getBytes("UTF-8"));

            if (salt != null) {
                return String.format("%s%s%s%s%s", toBase64(salt), DELIMITER,
                        toBase64(iv), DELIMITER, toBase64(cipherText));
            }

            return String.format("%s%s%s", toBase64(iv), DELIMITER,
                    toBase64(cipherText));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toHex(byte[] bytes) {
        StringBuffer buff = new StringBuffer();
        for (byte b : bytes) {
            buff.append(String.format("%02X", b));
        }

        return buff.toString();
    }

    public static String toBase64(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    public static byte[] fromBase64(String base64) {
        return Base64.decode(base64, Base64.NO_WRAP);
    }


    public static String decrypt(byte[] cipherBytes, SecretKey key, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            IvParameterSpec ivParams = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, key, ivParams);
            //Log.d(TAG, "Cipher IV: " + toHex(cipher.getIV()));
            log.debug("Cifrado IV: " + toHex(cipher.getIV()));
            log.warn("******probamos los bytes " + cipherBytes);
            byte[] plaintext = cipher.doFinal(cipherBytes);
            String plainrStr = new String(plaintext, "UTF-8");
            log.info("Texto cifrado: " + plainrStr);
            //System.out.println(plainrStr+"Texto descifrado");
            return plainrStr;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String decryptPbkdf2(String ciphertext, String password) {
        String[] fields = ciphertext.split(DELIMITER);
        if (fields.length != 3) {
            throw new IllegalArgumentException("Invalid encrypted text format");
        }

        byte[] salt = fromBase64(fields[0]);
        byte[] iv = fromBase64(fields[1]);
        byte[] cipherBytes = fromBase64(fields[2]);
        SecretKey key = deriveKeyPbkdf2(salt, password);
        log.debug("Pasa por la desencriptacion! ");
        //System.out.println("PASAAAAAAAAAAAA");
        return decrypt(cipherBytes, key, iv);
    }


}
