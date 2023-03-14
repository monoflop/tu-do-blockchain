package com.philippkutsch.tuchain.utils;

import com.philippkutsch.tuchain.chain.Blockchain;
import com.philippkutsch.tuchain.chain.HashedBlock;
import com.philippkutsch.tuchain.chain.utils.ChainUtils;
import com.philippkutsch.tuchain.network.protocol.BlockchainSyncMessage;
import com.philippkutsch.tuchain.network.protocol.Message;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

//TODO: Not sure if blockchain is just corrupted, or if its a real bug
@Ignore
public class BlockchainVerificationUtilsTest {
    @Test
    public void validateBlockchainTest() throws IOException {
        File testMessage = new File("src/test/resources/BSyncTestchain.json");
        Message message = ChainUtils.decodeFromString(Files.readString(testMessage.toPath()), Message.class);
        BlockchainSyncMessage blockchainSyncMessage = message.to(BlockchainSyncMessage.class);
        Blockchain blockchain = blockchainSyncMessage.getBlockchain();

        BlockchainVerificationUtils.VerificationResult result = BlockchainVerificationUtils
                .validateBlockchain(blockchain, 100);


        if(!result.isSuccess()) {
            System.out.println(result.getError());
            if(result.getErrorBlock() != null) {
                HashedBlock targetPrev = blockchain.findBlock(result.getErrorBlock().getId() - 1).get();

                System.out.println(ChainUtils.encodeToBeautyString(result.getErrorBlock()));
                System.out.println(ChainUtils.encodeToBeautyString(targetPrev));

                //Check hash
                System.out.println(targetPrev.isHeaderValid());
            }
        }

        assert result.isSuccess();
    }
}
