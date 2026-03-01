package com.softcraft.freechat.utils;

import android.util.Base64;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtils {
    private static final String SECRET = "FreeChatSecretKey";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * Generate a symmetric AES key from two user IDs.
     * The key is deterministic – both parties can compute it without exchange.
     */
    public static SecretKey generateKey(String userId1, String userId2) {
        // Sort IDs so the order doesn't matter
        String sorted = userId1.compareTo(userId2) < 0
                ? userId1 + userId2
                : userId2 + userId1;
        String input = sorted + SECRET;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(hash, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    // Derive a secondary key from the base key and a salt
    private static SecretKey deriveKey(SecretKey baseKey, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = baseKey.getEncoded();
            byte[] saltBytes = salt.getBytes(StandardCharsets.UTF_8);
            byte[] combined = new byte[keyBytes.length + saltBytes.length];
            System.arraycopy(keyBytes, 0, combined, 0, keyBytes.length);
            System.arraycopy(saltBytes, 0, combined, keyBytes.length, saltBytes.length);
            byte[] hash = digest.digest(combined);
            return new SecretKeySpec(hash, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Multi‑layer encryption:
     * 1. Apply custom obfuscation (transformString)
     * 2. AES‑GCM encrypt with base key → base64
     * 3. Reverse the base64 string
     * 4. AES‑GCM encrypt with derived key → final base64
     */
    public static String encrypt(String plainText, SecretKey baseKey) throws Exception {
        // Layer 0: custom obfuscation
        String obfuscated = transformString(plainText);

        // Layer 1: encrypt with base key
        String layer1 = aesEncrypt(obfuscated, baseKey);

        // Layer 2: reverse the string
        String reversed = new StringBuilder(layer1).reverse().toString();

        // Layer 3: encrypt with derived key
        SecretKey layer2Key = deriveKey(baseKey, "layer2");
        return aesEncrypt(reversed, layer2Key);
    }

    /**
     * Reverse of the multi‑layer encryption.
     */
    public static String decrypt(String cipherText, SecretKey baseKey) throws Exception {
        // Layer 3 decrypt
        SecretKey layer2Key = deriveKey(baseKey, "layer2");
        String reversed = aesDecrypt(cipherText, layer2Key);

        // Undo reverse
        String layer1 = new StringBuilder(reversed).reverse().toString();

        // Layer 1 decrypt
        String obfuscated = aesDecrypt(layer1, baseKey);

        // Undo custom obfuscation
        return reverseTransformString(obfuscated);
    }

    // Standard AES‑GCM encryption returning Base64 string
    private static String aesEncrypt(String plain, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
        buffer.put(iv);
        buffer.put(cipherText);
        return Base64.encodeToString(buffer.array(), Base64.NO_WRAP);
    }

    // Standard AES‑GCM decryption from Base64 string
    private static String aesDecrypt(String cipherText, SecretKey key) throws Exception {
        byte[] combined = Base64.decode(cipherText, Base64.NO_WRAP);
        ByteBuffer buffer = ByteBuffer.wrap(combined);
        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);
        byte[] cipherBytes = new byte[buffer.remaining()];
        buffer.get(cipherBytes);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        byte[] plainBytes = cipher.doFinal(cipherBytes);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }

    // Existing custom obfuscation (kept unchanged)
    public static String transformString(String input) {
        if (input == null) return null;
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                char shifted = (c == 'a') ? 'z' : (char) (c - 1);
                sb.append(Character.toUpperCase(shifted));
            } else if (c >= 'A' && c <= 'Z') {
                char shifted = (c == 'A') ? 'Z' : (char) (c - 1);
                sb.append(Character.toLowerCase(shifted));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String reverseTransformString(String input) {
        if (input == null) return null;
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            // First swap case to undo the earlier case swap
            char swapped = Character.isLowerCase(c) ? Character.toUpperCase(c) : Character.toLowerCase(c);
            // Then shift forward
            if (swapped >= 'a' && swapped <= 'z') {
                char shifted = (swapped == 'z') ? 'a' : (char) (swapped + 1);
                sb.append(shifted);
            } else if (swapped >= 'A' && swapped <= 'Z') {
                char shifted = (swapped == 'Z') ? 'A' : (char) (swapped + 1);
                sb.append(shifted);
            } else {
                sb.append(swapped);
            }
        }
        return sb.toString();
    }
}