package com.philippkutsch.tuchain.network.protocol.user.handlers;

import com.google.common.hash.Hashing;
import com.philippkutsch.tuchain.jsonchain.BlockBody;
import com.philippkutsch.tuchain.jsonchain.HashedBlock;
import com.philippkutsch.tuchain.jsonchain.SignedTransaction;
import com.philippkutsch.tuchain.jsonchain.Transaction;
import com.philippkutsch.tuchain.jsonchain.utils.ChainUtils;
import com.philippkutsch.tuchain.network.NodeController;
import com.philippkutsch.tuchain.network.RemoteNode;
import com.philippkutsch.tuchain.network.protocol.user.UserMessageHandler;
import com.philippkutsch.tuchain.network.protocol.user.messages.BalanceMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Base64;
import java.util.List;

public class BalanceMessageHandler implements UserMessageHandler<BalanceMessage> {
    private static final Logger logger
            = LoggerFactory.getLogger(BalanceMessageHandler.class);

    @Override
    public void handleMessage(@Nonnull NodeController nodeController,
                              @Nonnull RemoteNode currentNode,
                              @Nonnull BalanceMessage message) {
        List<HashedBlock> blockList = nodeController.getBlockChain().getBlockchain();

        //TODO handle unspent transactions better
        //TODO substract spent transactions
        int balance = 0;
        for(HashedBlock hashedBlock : blockList) {
            //Do not process genesis block
            if(hashedBlock.getId() == 1) {
                continue;
            }
            BlockBody blockBody = hashedBlock.getData();
            for(SignedTransaction signedTransaction : blockBody.getSignedTransactions()) {
                for(Transaction.Target target : signedTransaction.getTargets()) {
                    String pubKeyBase64 = Base64.getEncoder().encodeToString(target.getPubKey());
                    if(pubKeyBase64.equals(message.getPublicKey())) {
                        logger.info("Found in block #" + hashedBlock.getId()
                                + " tx: " + new String(
                                Base64.getEncoder()
                                        .encode(Hashing.sha256().hashBytes(ChainUtils.encodeToBytes(signedTransaction)).asBytes()))
                                + " amount: " + target.getAmount());
                        balance += target.getAmount();
                    }
                }
            }
        }
        logger.info("Total available balance:  " + balance);
    }
}
