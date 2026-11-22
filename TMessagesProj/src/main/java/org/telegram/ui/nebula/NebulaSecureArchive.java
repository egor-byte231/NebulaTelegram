package org.telegram.ui.nebula;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** AES-256-GCM archive encryption with an Android Keystore non-exportable key.
 * Never silently falls back to plaintext on encryption/decryption failure. */
public final class NebulaSecureArchive {
    private static final String ALIAS = "nebula_local_archive_aes_v1";
    private static final String STORE = "nebula_message_archive_v1";
    private static final String CIPHERTEXT = "entries_encrypted_v1";
    private static final String LEGACY = "entries";
    private NebulaSecureArchive() {}

    private static SecretKey key() throws Exception {
        if (Build.VERSION.SDK_INT < 23) throw new IllegalStateException("Android Keystore requires API 23");
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        java.security.Key existing = store.getKey(ALIAS, null);
        if (existing != null) return (SecretKey) existing;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build());
        return generator.generateKey();
    }
    private static String encrypt(String plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] result = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
        return Base64.encodeToString(result, Base64.NO_WRAP);
    }
    private static String decrypt(String payload) throws Exception {
        byte[] bytes = Base64.decode(payload, Base64.NO_WRAP);
        if (bytes.length < 29) throw new SecurityException("Invalid encrypted archive");
        byte[] iv = new byte[12];
        System.arraycopy(bytes, 0, iv, 0, iv.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(bytes, 12, bytes.length - 12), StandardCharsets.UTF_8);
    }
    /** Migration commits ciphertext and removal of legacy plaintext in one transaction. */
    public static synchronized String read(Context context) throws Exception {
        SharedPreferences p = context.getSharedPreferences(STORE, Context.MODE_PRIVATE);
        String secured = p.getString(CIPHERTEXT, null);
        if (secured != null) return decrypt(secured);
        String old = p.getString(LEGACY, "[]");
        String payload = encrypt(old);
        if (!p.edit().putString(CIPHERTEXT, payload).remove(LEGACY).commit())
            throw new IllegalStateException("Archive migration failed");
        return old;
    }
    /** Delete both encrypted data and legacy plaintext without creating a new key. */
    public static synchronized boolean clear(Context context) {
        return context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit()
                .remove(CIPHERTEXT).remove(LEGACY).commit();
    }
    public static synchronized boolean write(Context context, String json) {
        try {
            String encrypted = encrypt(json);
            return context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit()
                    .putString(CIPHERTEXT, encrypted).remove(LEGACY).commit();
        } catch (Exception e) { return false; }
    }
}
