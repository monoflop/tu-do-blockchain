package com.philippkutsch.tuchain.blockchain;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.Base64;

/**
 * Block transactions
 *
 * id: 0 => mining reward
 */
public class Transaction {
    protected final long id;
    protected final long timestamp;

    protected final int pubKeyLength;
    protected final byte[] pubKey;

    protected final int targetsLength;
    protected final int targetsCount;
    protected final Target[] targets;

    private final ByteBuffer buffer;

    public Transaction(long id,
                       long timestamp,
                       @Nonnull byte[] pubKey,
                       @Nonnull Target[] targets) {
        this.id = id;
        this.timestamp = timestamp;
        this.pubKeyLength = pubKey.length;
        this.pubKey = pubKey;

        this.targetsCount = targets.length;
        this.targets = targets;

        //Calculate targetsLength
        int length = 0;
        for(Target target : targets) {
            length += target.toBytes().length;
        }
        this.targetsLength = length;

        this.buffer = ByteBuffer.allocate(
                8 /*ID*/
                + 8 /*timestamp*/
                + 4 /*pubKeyLength*/
                + pubKey.length /*pubKey*/
                + 4 /*targetsLength*/
                + 4 /*targetsCount*/
                + targetsLength /*length of transaction blob*/);
    }

    public long getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getPubKey() {
        return pubKey;
    }

    public Target[] getTargets() {
        return targets;
    }

    @Nonnull
    public byte[] toBytes() {
        buffer.clear();
        buffer.putLong(0, id);
        buffer.putLong(8, timestamp);
        buffer.putInt(16, pubKeyLength);
        buffer.put(20, pubKey);
        buffer.putInt(20 + pubKey.length, targetsLength);
        buffer.putInt(20 + pubKey.length + 4, targetsCount);

        ByteBuffer targetBuffer = ByteBuffer.allocate(targetsLength);
        for(Target target : targets) {
            targetBuffer.put(target.toBytes());
        }

        buffer.put(20 + pubKey.length + 8, targetBuffer.array());
        return buffer.array();
    }

    @Nonnull
    public static Transaction fromBytes(@Nonnull byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        long id = byteBuffer.getLong(0);
        long timestamp = byteBuffer.getLong(8);
        int pubKeyLength = byteBuffer.getInt(16);
        byte[] pubKey = new byte[pubKeyLength];
        byteBuffer.get(20, pubKey, 0, pubKeyLength);

        int targetsLength = byteBuffer.getInt(20 + pubKeyLength);
        int targetsCount = byteBuffer.getInt(20 + pubKeyLength + 4);

        byte[] targetBytes = new byte[targetsLength];
        byteBuffer.get(20 + pubKeyLength + 8, targetBytes, 0, targetsLength);

        //Get targets from bytes
        Target[] targets = new Target[targetsCount];
        ByteBuffer targetsByteBuffer = ByteBuffer.wrap(targetBytes);

        int offset = 0;
        for(int i = 0; i < targetsCount; i++) {
            int amount = targetsByteBuffer.getInt(0 + offset);
            int targetPubKeyLength = targetsByteBuffer.getInt(4 + offset);
            byte[] targetPubKey = new byte[targetPubKeyLength];
            targetsByteBuffer.get(8 + offset, targetPubKey, 0, targetPubKeyLength);

            targets[i] = new Target(amount, targetPubKey);
            offset += 8 + targetPubKeyLength;
        }

        return new Transaction(id, timestamp, pubKey, targets);
    }

    @Override
    public String toString() {
        StringBuilder targetBuilder = new StringBuilder();
        for(Target target : targets) {
            targetBuilder.append(target).append("\n");
        }

        return "--------------------------------------------\n" +
                "Not signed Transaction #" + id + "\n" +
                "timestamp: " + timestamp + "\n" +
                "pubKey: " + new String(Base64.getEncoder().encode(pubKey)) + "\n" +
                "targetsCount: " + targetsCount + "\n" +
                "targets: " + "\n" +
                targetBuilder +
                "--------------------------------------------";
    }

    public static class Target {
        protected final int amount;
        protected final int pubKeyLength;
        protected final byte[] pubKey;

        private final ByteBuffer targetBuffer;

        public Target(int amount,
                      @Nonnull byte[] pubKey) {
            this.amount = amount;
            this.pubKey = pubKey;
            this.pubKeyLength = pubKey.length;

            this.targetBuffer = ByteBuffer.allocate(
                    4 /*amount*/
                    + 4 /*pubKeyLength*/
                    + pubKey.length
            );
        }

        @Nonnull
        public byte[] toBytes() {
            targetBuffer.clear();
            targetBuffer.putInt(0, amount);
            targetBuffer.putInt(4, pubKeyLength);
            targetBuffer.put(8, pubKey);
            return targetBuffer.array();
        }

        @Nonnull
        public static Target fromBytes(@Nonnull byte[] bytes) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
            int amount = byteBuffer.getInt(0);
            int pubKeyLength = byteBuffer.getInt(4);
            byte[] pubKey = new byte[pubKeyLength];
            byteBuffer.get(8, pubKey, 0, pubKeyLength);

            return new Target(amount, pubKey);
        }

        public byte[] getPubKey() {
            return pubKey;
        }

        public int getAmount() {
            return amount;
        }

        @Override
        public String toString() {
            return "Transfer amount: " + amount + " -> " + new String(Base64.getEncoder().encode(pubKey));
        }
    }
}
