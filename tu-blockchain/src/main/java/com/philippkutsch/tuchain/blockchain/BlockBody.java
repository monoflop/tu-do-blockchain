package com.philippkutsch.tuchain.blockchain;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class BlockBody {
    protected final int transactionLength;
    protected final int transactionCount;
    protected final SignedTransaction[] signedTransactions;

    private final ByteBuffer buffer;

    public BlockBody(@Nonnull SignedTransaction[] signedTransactions) {
        this.signedTransactions = signedTransactions;
        this.transactionCount = signedTransactions.length;

        //Calculate transactionLength
        int length = 0;
        for(SignedTransaction signedTransaction : signedTransactions) {
            length += signedTransaction.toBytes().length;
        }
        this.transactionLength = length;

        this.buffer = ByteBuffer.allocate(
                4 /*transactionLength*/
                        + 4 /*transactionCount*/
                        + transactionLength /*length of signedTransactions blob*/);
    }

    public SignedTransaction[] getSignedTransactions() {
        return signedTransactions;
    }

    @Nonnull
    public byte[] toBytes() {
        buffer.clear();
        buffer.putInt(0, transactionLength);
        buffer.putInt(4, transactionCount);

        ByteBuffer transactionBuffer = ByteBuffer.allocate(transactionLength);
        for(SignedTransaction signedTransaction : signedTransactions) {
            transactionBuffer.put(signedTransaction.toBytes());
        }
        buffer.put(8, transactionBuffer.array());
        return buffer.array();
    }

    @Nonnull
    public static BlockBody fromBytes(@Nonnull byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        int transactionLength = byteBuffer.getInt(0);
        int transactionCount = byteBuffer.getInt(4);

        //Get transactions from bytes
        byte[] transactionBytes = new byte[transactionLength];
        byteBuffer.get(8, transactionBytes, 0, transactionLength);

        SignedTransaction[] transactions = new SignedTransaction[transactionCount];

        int offset = 0;
        for(int i = 0; i < transactionCount; i++) {
            byte[] targetArray = Arrays.copyOfRange(transactionBytes, offset, transactionBytes.length);
            SignedTransaction signedTransaction = SignedTransaction.fromBytes(targetArray);
            transactions[i] = signedTransaction;

            int signedTransactionLength = signedTransaction.toBytes().length;
            offset += signedTransactionLength;
        }

        return new BlockBody(transactions);
    }

    @Override
    public String toString() {
        StringBuilder transactionBuilder = new StringBuilder();
        for(SignedTransaction transaction : signedTransactions) {
            transactionBuilder.append(transaction).append("\n");
        }

        return "--------------------------------------------\n" +
                "Body\n" +
                "transactionCount: " + transactionCount + "\n" +
                "signedTransactions:\n" +
                transactionBuilder +
                "--------------------------------------------";
    }
}
