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

    /**
     * Encrypt a plain text string using AES/GCM.
     * Returns Base64(IV + ciphertext).
     */
    public static String encrypt(String plainText, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
        buffer.put(iv);
        buffer.put(cipherText);
        return Base64.encodeToString(buffer.array(), Base64.NO_WRAP);
    }

    /**
     * Decrypt a string that was produced by encrypt().
     */
    public static String decrypt(String cipherText, SecretKey key) throws Exception {
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