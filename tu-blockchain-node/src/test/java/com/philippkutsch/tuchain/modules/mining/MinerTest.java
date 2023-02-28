package com.philippkutsch.tuchain.modules.mining;

import com.philippkutsch.tuchain.chain.HashedBlock;
import com.philippkutsch.tuchain.chain.utils.ChainUtils;
import org.junit.Ignore;
import org.junit.Test;

public class MinerTest {
    private static final byte[] PREFIX_ZERO_LENGTH = new byte[]{(byte)0xff, (byte)0xff};
    private static final byte[] PREFIX_FOUR_LENGTH = new byte[]{(byte)0x0f, (byte)0xff};
    private static final byte[] PREFIX_EIGHT_LENGTH = new byte[]{(byte)0x0, (byte)0xff};
    private static final byte[] PREFIX_TEN_LENGTH = new byte[]{(byte)0x0, (byte)0x3f};

    @Test
    public void calculateZeroPrefix_shouldCalculateCorrectly() {
        assert Miner.calculateZeroPrefix(PREFIX_ZERO_LENGTH) == 0;
        assert Miner.calculateZeroPrefix(PREFIX_FOUR_LENGTH) == 4;
        assert Miner.calculateZeroPrefix(PREFIX_EIGHT_LENGTH) == 8;
        assert Miner.calculateZeroPrefix(PREFIX_TEN_LENGTH) == 10;
    }

    @Test
    @Ignore
    public void testThrotteling() throws Exception {
        new Miner(8, 100, HashedBlock.generateGenesisBlock(), (number, hash) -> System.out.println("Hash " + ChainUtils.bytesToBase64(hash))).call();
    }
}
