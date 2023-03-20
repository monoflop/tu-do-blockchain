package com.philippkutsch.tuchain;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * RsaKeys utility class
 *
 * Holds keys and allows signing / verification
 */
public class RsaKeys {
    private final byte[] privateKeyBytes;
    private final byte[] publicKeyBytes;

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public RsaKeys(@Nullable byte[] publicKeyBytes,
                   @Nullable byte[] privateKeyBytes)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        this.privateKeyBytes = privateKeyBytes;
        this.publicKeyBytes = publicKeyBytes;

        //If present, import public key
        if(publicKeyBytes != null) {
            this.publicKey = publicKeyFromBytes(publicKeyBytes);
        }
        else {
            this.publicKey = null;
        }

        //If present, import private key
        if(privateKeyBytes != null) {
            this.privateKey = privateKeyFromBytes(privateKeyBytes);
        }
        else {
            this.privateKey = null;
        }
    }

    @Nonnull
    public static RsaKeys fromFiles(@Nonnull File publicKeyFile,
                                    @Nullable File privateKeyFile)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] publicKeyBytes = Files.readAllBytes(publicKeyFile.toPath());
        byte[] privateKeyBytes = null;
        if(privateKeyFile != null) {
            privateKeyBytes = Files.readAllBytes(privateKeyFile.toPath());
        }
        return new RsaKeys(publicKeyBytes, privateKeyBytes);
    }

    @Nonnull
    public static RsaKeys fromFilesPaths(@Nonnull String publicKeyPath,
                                         @Nullable String privateKeyPath)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] publicKeyBytes = Files.readAllBytes(Paths.get(publicKeyPath));
        byte[] privateKeyBytes = null;
        if(privateKeyPath != null) {
            privateKeyBytes = Files.readAllBytes(Paths.get(privateKeyPath));
        }
        return new RsaKeys(publicKeyBytes, privateKeyBytes);
    }

    @Nonnull
    public byte[] signData(@Nonnull byte[] data)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        if(!hasPrivateKey()) {
            throw new IllegalStateException("No private key");
        }
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(data);
        return signature.sign();
    }

    public boolean verifyData(@Nonnull byte[] data, @Nonnull byte[] sig)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        if(!hasPublicKey()) {
            throw new IllegalStateException("No public key");
        }
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(sig);
    }

    @Nonnull
    public byte[] getPublicKeyBytes() {
        return publicKeyBytes;
    }

    @Nullable
    public byte[] getPrivateKeyBytes() {
        return privateKeyBytes;
    }

    public boolean hasPublicKey() {
        return publicKeyBytes != null;
    }

    public boolean hasPrivateKey() {
        return privateKeyBytes != null;
    }

    @Nonnull
    private static PublicKey publicKeyFromBytes(@Nonnull byte[] bytes)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec x509EncodedKeySpec =
                new X509EncodedKeySpec(bytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(x509EncodedKeySpec);
    }

    @Nonnull
    private static PrivateKey privateKeyFromBytes(@Nonnull byte[] bytes)
            throws NoSuchAlgorithmException, InvalidKeySpecException{
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec =
                new PKCS8EncodedKeySpec(bytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(pkcs8EncodedKeySpec);
    }
}

