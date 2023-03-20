package com.philippkutsch.tuchain;

import com.philippkutsch.tuchain.chain.utils.ChainUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.UUID;

/**
 * Key-Pair container class
 *
 * Wraps private and public key.
 */
public class Wallet {
    private final byte[] privateKey;
    private final byte[] publicKey;

    @SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
    @Nonnull
    public static Wallet generate()
            throws IOException,
            InterruptedException,
            NoSuchAlgorithmException,
            InvalidKeySpecException{
        //Generate random file names for key files
        String privatePemName = UUID.randomUUID() + ".pem";
        String privateDerName = UUID.randomUUID() + ".der";
        String publicDerName = UUID.randomUUID() + ".der";

        //Run openssl and generate keys
        Runtime runtime = Runtime.getRuntime();
        Process privateKeyProcess = runtime.exec(
                "openssl genrsa -out " + privatePemName + " 2048");
        int privateKeyReturn = privateKeyProcess.waitFor();
        if(privateKeyReturn != 0) {
            throw new IllegalStateException("Private key generation failed with code: " + privateKeyReturn);
        }

        Process privateKeyDerProcess = runtime.exec(
                "openssl pkcs8 -topk8 -inform PEM -outform DER -in "
                        + privatePemName + " -out " + privateDerName + " -nocrypt");
        int privateKeyDerReturn = privateKeyDerProcess.waitFor();
        if(privateKeyDerReturn != 0) {
            throw new IllegalStateException("Private key der converting failed with code: " + privateKeyDerReturn);
        }

        Process publicKeyGenerationProcess = runtime.exec("openssl rsa -in "
                + privatePemName + " -pubout -outform DER -out " + publicDerName);
        int publicKeyGenerationReturn = publicKeyGenerationProcess.waitFor();
        if(publicKeyGenerationReturn != 0) {
            throw new IllegalStateException("Public key generation failed with code: " + publicKeyGenerationReturn);
        }

        //Check if files exists
        File privateKeyPemFile = new File(privatePemName);
        File privateKeyDerFile = new File(privateDerName);
        File publicKeyDerFile = new File(publicDerName);
        if(!privateKeyPemFile.exists()) {
            throw new FileNotFoundException("Generated private key pem file " + privateKeyPemFile + " not found");
        }
        if(!privateKeyDerFile.exists()) {
            throw new FileNotFoundException("Generated private key der file " + privateKeyDerFile + " not found");
        }
        if(!publicKeyDerFile.exists()) {
            throw new FileNotFoundException("Generated public key der file " + publicKeyDerFile + " not found");
        }

        //Load files
        RsaKeys rsaKeys = RsaKeys.fromFiles(publicKeyDerFile, privateKeyDerFile);

        //Remove local files
        privateKeyPemFile.delete();
        privateKeyDerFile.delete();
        publicKeyDerFile.delete();

        return new Wallet(rsaKeys.getPrivateKeyBytes(), rsaKeys.getPublicKeyBytes());
    }

    @Nonnull
    public static Wallet load(@Nonnull File walletFile)
            throws IOException {
        String serialized = Files.readString(walletFile.toPath());
        return ChainUtils.decodeFromString(serialized, Wallet.class);
    }

    public Wallet(@Nonnull byte[] privateKey,
                  @Nonnull byte[] publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public void save(@Nonnull File walletFile)
            throws IOException {
        String serialized = ChainUtils.encodeToString(this);
        Files.writeString(walletFile.toPath(), serialized);
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }
}
