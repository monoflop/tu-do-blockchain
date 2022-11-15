package com.philippkutsch.tuchain.blockchain;

import com.google.common.hash.HashCode;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;

public class HashedBlock extends Block {
    protected final long timestamp;
    protected final long nuOnce;
    protected final byte[] hash;

    public HashedBlock(
            @Nonnull Block block,
            long timestamp,
            long nuOnce,
            @Nonnull byte[] hash) {
        super(block.id, block.prevHash, block.dataSize, block.data);
        this.timestamp = timestamp;
        this.nuOnce = nuOnce;
        this.hash = hash;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getNuOnce() {
        return nuOnce;
    }

    public byte[] getHash() {
        return hash;
    }

    public boolean isValid() {
        byte[] generatedBlockHash = generateHash(timestamp, nuOnce);
        return Arrays.equals(generatedBlockHash, hash);
    }

    public byte[] toBytes() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(8 /*ID*/ + 8 /*timestamp*/ + 32 /*prevHash*/ + 32 /*hash*/ + 8 /*nuOnce*/ + 8 /*dataSize*/ + data.length /*data*/);
        byteBuffer.putLong(0, id);
        byteBuffer.putLong(8, timestamp);
        byteBuffer.put(16, prevHash);
        byteBuffer.put(48, hash);
        byteBuffer.putLong(80, nuOnce);
        byteBuffer.putLong(88, dataSize);
        byteBuffer.put(96, data);
        return byteBuffer.array();
    }

    public static HashedBlock fromBytes(@Nonnull byte[] bytes) {
        if(bytes.length <= Block.HEADER_SIZE_BYTES) {
            throw new IllegalArgumentException("byte array is too small. required > " + Block.HEADER_SIZE_BYTES);
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        long id = byteBuffer.getLong(0);
        long timestamp = byteBuffer.getLong(8);
        byte[] prevHash = new byte[32];
        byte[] hash = new byte[32];
        byteBuffer.get(16, prevHash, 0, 32);
        byteBuffer.get(48, hash, 0, 32);
        long nuOnce = byteBuffer.getLong(80);
        long dataSize = byteBuffer.getLong(88);

        //Ensure we have enough data
        long remainingSize = bytes.length - Block.HEADER_SIZE_BYTES;
        if(remainingSize != dataSize) {
            throw new IllegalArgumentException("Invalid header data size");
        }

        int dataBufferSize = (int)dataSize;
        byte[] data = new byte[dataBufferSize];
        byteBuffer.get(96, data, 0, dataBufferSize);

        return new HashedBlock(new Block(id, prevHash, dataSize, data), timestamp, nuOnce, hash);
    }

    @Override
    public String toString() {
        return "--------------------------------------------\n" +
                "Hashed Block #" + id + "\n" +
                "timestamp: " + timestamp + "\n" +
                "prevHash: " + HashCode.fromBytes(prevHash) + "\n" +
                "hash: " + HashCode.fromBytes(hash) + "\n" +
                "nuOnce: " + nuOnce + "\n" +
                "dataSize: " + dataSize + "\n" +
                "data: " + new String(Base64.getEncoder().encode(data)) + "\n" +
                //"binary block: " + new String(Base64.getEncoder().encode(toBytes())) + "\n" +
                "--------------------------------------------";
    }
}
