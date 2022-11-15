package com.philippkutsch.tuchain.blockchain;

import com.google.common.hash.Hashing;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.Base64;

public class SignedTransaction extends Transaction {
    protected final int signatureLength;
    protected final byte[] signature;

    private final ByteBuffer signedBuffer;

    public SignedTransaction(long id,
                             long timestamp,
                             @Nonnull byte[] pubKey,
                             @Nonnull Target[] targets,
                             @Nonnull byte[] signature) {
        super(id, timestamp, pubKey, targets);
        this.signature = signature;
        this.signatureLength = signature.length;

        signedBuffer = ByteBuffer.allocate(super.toBytes().length + 4 + signature.length);
    }

    public byte[] getSignature() {
        return signature;
    }

    @Nonnull
    public static SignedTransaction fromTransaction(
            @Nonnull Transaction transaction,
            @Nonnull byte[] signature) {
        return new SignedTransaction(transaction.id, transaction.timestamp,
                transaction.pubKey, transaction.targets, signature);
    }

    @Nonnull
    public byte[] toBytes() {
        byte[] transactionBytes = super.toBytes();
        signedBuffer.put(0, transactionBytes);
        signedBuffer.putInt(transactionBytes.length, signatureLength);
        signedBuffer.put(transactionBytes.length + 4, signature);
        return signedBuffer.array();
    }

    @Nonnull
    public byte[] transactionToBytes() {
        return super.toBytes();
    }

    @Nonnull
    public static SignedTransaction fromBytes(@Nonnull byte[] bytes) {
        Transaction transaction = Transaction.fromBytes(bytes);
        int length = transaction.toBytes().length;

        ByteBuffer buff = ByteBuffer.wrap(bytes);
        int signatureLength = buff.getInt(length);

        byte[] signature = new byte[signatureLength];
        buff.get(length + 4, signature, 0, signatureLength);

        return new SignedTransaction(transaction.id, transaction.timestamp, transaction.pubKey, transaction.targets, signature);
    }

    @Override
    public String toString() {
        StringBuilder targetBuilder = new StringBuilder();
        for(Target target : targets) {
            targetBuilder.append(target).append("\n");
        }

        return "--------------------------------------------\n" +
                "Signed Transaction #" + id + "\n" +
                "tx: " + Base64.getEncoder()
                .encodeToString(Hashing.sha256().hashBytes(transactionToBytes()).asBytes()) + "\n" +
                "timestamp: " + timestamp + "\n" +
                "pubKey: " + new String(Base64.getEncoder().encode(pubKey)) + "\n" +
                "targetsCount: " + targetsCount + "\n" +
                "targets: " + "\n" +
                targetBuilder +
                "signature: " + new String(Base64.getEncoder().encode(signature)) + "\n" +
                "--------------------------------------------";
    }
}
