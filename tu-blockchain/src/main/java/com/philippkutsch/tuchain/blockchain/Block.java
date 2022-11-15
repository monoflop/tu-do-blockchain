package com.philippkutsch.tuchain.blockchain;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.Base64;

public class Block {
    public static final int HEADER_SIZE_BYTES = 96;

    protected final long id;
    protected final byte[] prevHash;
    protected final long dataSize;
    protected final byte[] data;
    protected final ByteBuffer blockBuffer;

    public Block(
            long id,
            @Nonnull byte[] prevHash,
            long dataSize,
            @Nonnull byte[] data) {
        this.id = id;
        this.prevHash = prevHash;
        this.dataSize = dataSize;
        this.data = data;
        this.blockBuffer = ByteBuffer.allocate(8 /*ID*/ + 8 /*timestamp*/ + 32 /*prevHash*/ + 8 /*nuonce*/ + 8 /*dataSize*/ + data.length);
    }

    public long getId() {
        return id;
    }

    public byte[] getPrevHash() {
        return prevHash;
    }

    public long getDataSize() {
        return dataSize;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] generateHash(long timestamp, long nuOnce) {
        blockBuffer.clear();
        blockBuffer.putLong(id);
        blockBuffer.putLong(timestamp);
        blockBuffer.put(prevHash);
        blockBuffer.putLong(nuOnce);
        blockBuffer.putLong(dataSize);
        blockBuffer.put(data);
        return Hashing.sha256()
                .hashBytes(blockBuffer.array())
                .asBytes();
    }

    @Override
    public String toString() {
        return "--------------------------------------------\n" +
                "Not hashed Block #" + id + "\n" +
                "prevHash: " + HashCode.fromBytes(prevHash) + "\n" +
                "dataSize: " + dataSize + "\n" +
                "data: " + new String(Base64.getEncoder().encode(data)) + "\n" +
                "--------------------------------------------";
    }
}
