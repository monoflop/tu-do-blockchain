package com.philippkutsch.tuchain.blockchain;

import org.junit.Test;

import java.util.Base64;

public class BlockBodyTest {
    private static final SignedTransaction testTransaction = new SignedTransaction(
            1, 1665347615742L,
            new byte[] {
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
            },
            new Transaction.Target[]{new Transaction.Target(100, new byte[] {
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
            })},
            new byte[] {
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
            }
    );
    private static final String blockBodyEncodedBase64 = "AAAARgAAAAEAAAAAAAAAAQAAAYO+c/v+AAAACgEBAQEBAQEBAQEAAAASAAAAAQAAAGQAAAAKAQEBAQEBAQEBAQAAAAoBAQEBAQEBAQEB";

    @Test
    public void importTest() {
        byte[] bytes = Base64.getDecoder().decode(blockBodyEncodedBase64);
        BlockBody blockBody = BlockBody.fromBytes(bytes);
        //TODO test
        assert blockBody.signedTransactions.length == 1;
    }

    @Test
    public void exportTest() {
        BlockBody blockBody = new BlockBody(new SignedTransaction[] {testTransaction});
        byte[] bytes = blockBody.toBytes();
        String base64 = Base64.getEncoder().encodeToString(bytes);
        assert blockBodyEncodedBase64.equals(base64);
    }
}
