package com.philippkutsch.tuchain.chain;

import com.philippkutsch.tuchain.RsaKeys;
import com.philippkutsch.tuchain.chain.utils.ChainUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class TransactionTest {
    public static final String coinbaseTransactionId = "eBU5sEhChxQeqdOro2Fmhpz/VPoKfEFYQLYqxwvJL0A=";

    public RsaKeys rsaKeys;
    public Transaction coinbaseTransaction;

    @Before
    public void setup() throws IOException,
            NoSuchAlgorithmException, InvalidKeySpecException {
        File publicKeyFile = new File("src/test/resources/public.der");
        File privateKeyFile = new File("src/test/resources/private.der");
        rsaKeys = RsaKeys.fromFiles(publicKeyFile, privateKeyFile);

        //Coinbase transaction
        //32 null bytes since coinbase transaction has no input
        Transaction.Input input = new Transaction.Input(new byte[32], 0);
        //Coinbase can have variable bytes as signature
        byte[] signature = "PK coinbase transaction".getBytes(StandardCharsets.UTF_8);
        Transaction.SignedInput signedInput = new Transaction.SignedInput(input.getTxId(), input.getvOut(), signature);
        //Transaction output to miner public key
        Transaction.Output output = new Transaction.Output(100, rsaKeys.getPublicKeyBytes());
        Transaction.SignedInput[] inputs = { signedInput };
        Transaction.Output[] outputs = { output };
        coinbaseTransaction = new Transaction(1669935899L, inputs, outputs);
    }

    @Test
    public void getTransactionIdTest() {
        byte[] transactionId = coinbaseTransaction.getTransactionId();
        String idString = ChainUtils.bytesToBase64(transactionId);
        assert coinbaseTransactionId.equals(idString);
    }
}
