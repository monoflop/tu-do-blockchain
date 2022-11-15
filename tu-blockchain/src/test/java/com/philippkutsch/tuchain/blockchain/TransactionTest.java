package com.philippkutsch.tuchain.blockchain;

import org.junit.Test;

import java.util.Arrays;
import java.util.Base64;

public class TransactionTest {
    private static final int targetAmount = 12;
    private static final byte[] targetPubKey = new byte[]{
            0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
    };
    private static final String targetEncodedBase64 = "AAAADAAAAAoBAQEBAQEBAQEB";
    private static final Transaction.Target targetTarget
            = new Transaction.Target(targetAmount, targetPubKey);

    private static final long transactionId = 1;
    private static final long transactionTimestamp = 1665347615742L;
    private static final byte[] transactionPubKey = new byte[]{
            0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
    };
    private static final int transactionTargetsLength = targetTarget.toBytes().length;
    private static final int transactionTargetsCount = 1;
    private static final Transaction.Target[] transactionTargets = new Transaction.Target[] { targetTarget, targetTarget };
    private static final String transactionEncodedBase64 = "AAAAAAAAAAEAAAGDvnP7/gAAAAoBAQEBAQEBAQEBAAAAJAAAAAIAAAAMAAAACgEBAQEBAQEBAQEAAAAMAAAACgEBAQEBAQEBAQE=";

    private static final byte[] signedTransactionSignature = new byte[]{
            0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
    };
    private static final String signedTransactionEncodedBase64 = "AAAAAAAAAAEAAAGDvnP7/gAAAAoBAQEBAQEBAQEBAAAAJAAAAAIAAAAMAAAACgEBAQEBAQEBAQEAAAAMAAAACgEBAQEBAQEBAQEAAAAKAQEBAQEBAQEBAQ==";

    @Test
    public void importTargetTest() {
        byte[] targetBytes = Base64.getDecoder().decode(targetEncodedBase64);
        Transaction.Target target = Transaction.Target.fromBytes(targetBytes);

        assert target.amount == targetAmount;
        assert Arrays.equals(target.pubKey, targetPubKey);
    }

    @Test
    public void exportTargetTest() {
        Transaction.Target target = new Transaction.Target(targetAmount, targetPubKey);
        byte[] targetBytes = target.toBytes();
        String targetBase64 = Base64.getEncoder().encodeToString(targetBytes);
        assert targetEncodedBase64.equals(targetBase64);
    }

    @Test
    public void importTransactionTest() {
        byte[] transactionBytes = Base64.getDecoder().decode(transactionEncodedBase64);
        Transaction transaction = Transaction.fromBytes(transactionBytes);

        assert transaction.id == transactionId;
        assert transaction.timestamp == transactionTimestamp;
        assert Arrays.equals(transaction.pubKey, transactionPubKey);
        assert transaction.targets.length == transactionTargets.length;
        for(int i = 0; i < transaction.targets.length; i++) {
            assert transaction.targets[i].amount == transactionTargets[i].amount;
            assert Arrays.equals(transaction.targets[i].pubKey, transactionTargets[i].pubKey);
        }
    }

    @Test
    public void exportTransactionTest() {
        Transaction transaction = new Transaction(transactionId, transactionTimestamp, transactionPubKey, transactionTargets);
        byte[] transactionBytes = transaction.toBytes();
        String transactionBase64 = Base64.getEncoder().encodeToString(transactionBytes);
        assert transactionEncodedBase64.equals(transactionBase64);
    }

    @Test
    public void importSignedTransactionTest() {
        byte[] signedTransactionBytes = Base64.getDecoder().decode(signedTransactionEncodedBase64);
        SignedTransaction signedTransaction = SignedTransaction.fromBytes(signedTransactionBytes);

        assert signedTransaction.id == transactionId;
        assert signedTransaction.timestamp == transactionTimestamp;
        assert Arrays.equals(signedTransaction.pubKey, transactionPubKey);
        assert signedTransaction.targets.length == transactionTargets.length;
        for(int i = 0; i < signedTransaction.targets.length; i++) {
            assert signedTransaction.targets[i].amount == transactionTargets[i].amount;
            assert Arrays.equals(signedTransaction.targets[i].pubKey, transactionTargets[i].pubKey);
        }

        assert Arrays.equals(signedTransaction.signature, signedTransactionSignature);
    }

    @Test
    public void exportSignedTransactionTest() {
        SignedTransaction signedTransaction = new SignedTransaction(
                transactionId, transactionTimestamp, transactionPubKey, transactionTargets, signedTransactionSignature);
        byte[] signedTransactionBytes = signedTransaction.toBytes();
        String signedTransactionBase64 = Base64.getEncoder().encodeToString(signedTransactionBytes);
        System.out.println(signedTransactionBase64);
        assert signedTransactionEncodedBase64.equals(signedTransactionBase64);
    }
}
