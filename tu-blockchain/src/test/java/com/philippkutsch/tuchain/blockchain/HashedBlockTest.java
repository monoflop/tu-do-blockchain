package com.philippkutsch.tuchain.blockchain;

import com.google.common.hash.HashCode;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

public class HashedBlockTest {
    private static final String testGenesisBlock =
            "AAAAAAAAAAEAAAGDvnP7/gAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA9m5XwHv84tX2V57Mo43ZomFvvcyvpDdLwvTqnqYAAAAAAJytxQAAAAAAAAAqVFUtRG9ydG11bmQtQ2hhaW4gR2VuZXNpcyB+IFBoaWxpcHAgS3V0c2No";
    private static final long id = 1L;
    private static final byte[] prevHash = new byte[32];
    private static final long timestamp = 1665347615742L;
    private static final long nuOnce = 10268101L;
    private static final long dataSize = 42;
    private static final byte[] data = "TU-Dortmund-Chain Genesis ~ Philipp Kutsch".getBytes(StandardCharsets.UTF_8);

    private static final String hash = "000000f66e57c07bfce2d5f6579ecca38dd9a2616fbdccafa4374bc2f4ea9ea6";
    private static final String hashBase64 = "AAAA9m5XwHv84tX2V57Mo43ZomFvvcyvpDdLwvTqnqY=";

    @Test
    public void importTest() {
        byte[] blockBytes = Base64.getDecoder().decode(testGenesisBlock);
        HashedBlock hashedBlock = HashedBlock.fromBytes(blockBytes);

        assert hashedBlock.id == id;
        assert Arrays.equals(hashedBlock.prevHash, prevHash);
        assert hashedBlock.timestamp == timestamp;
        assert hashedBlock.nuOnce == nuOnce;
        assert hashedBlock.dataSize == dataSize;
        assert HashCode.fromBytes(hashedBlock.hash).toString().equals(hash);
        assert Arrays.equals(hashedBlock.data, data);
    }

    @Test
    public void exportTest() {
        byte[] hashBytes = Base64.getDecoder().decode(hashBase64);
        HashedBlock hashedBlock = new HashedBlock(new Block(id, prevHash, dataSize, data), timestamp, nuOnce, hashBytes);
        byte[] exportedBlockBytes = hashedBlock.toBytes();
        String exportedBlockString = Base64.getEncoder().encodeToString(exportedBlockBytes);
        assert testGenesisBlock.equals(exportedBlockString);
    }

    @Test
    public void isValid() {
        byte[] blockBytes = Base64.getDecoder().decode(testGenesisBlock);
        HashedBlock hashedBlock = HashedBlock.fromBytes(blockBytes);
        assert hashedBlock.isValid();
    }

    @Test
    public void isValid_invalid() {
        byte[] blockBytes = Base64.getDecoder().decode(testGenesisBlock);
        HashedBlock hashedBlock = HashedBlock.fromBytes(blockBytes);
        hashedBlock.hash[0] = 64;
        hashedBlock.hash[1] = 2;
        assert !hashedBlock.isValid();
    }
}
