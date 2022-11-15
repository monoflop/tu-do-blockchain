package com.philippkutsch.tuchain.jsonchain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.philippkutsch.tuchain.jsonchain.utils.Base64TypeAdapter;
import org.junit.Before;
import org.junit.Test;

public class TransactionTest {
    private static final Transaction.Target target = new Transaction.Target(
            100,
            new byte[]{0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7}
    );
    private static final Transaction transaction = new Transaction(
            1,
            1665347615742L,
            new byte[]{0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7},
            new Transaction.Target[]{target});

    private Gson gson;
    @Before
    public void setUp() {
        gson = new GsonBuilder().registerTypeAdapter(byte[].class, new Base64TypeAdapter()).create();
    }

    @Test
    public void encodeDecodeTargetTest() {
        String encoded = gson.toJson(target);
        Transaction.Target decoded = gson.fromJson(encoded, Transaction.Target.class);
        assert decoded.equals(target);
    }

    @Test
    public void encodeDecodeTransactionTest() {
        String encoded = gson.toJson(transaction);
        Transaction decoded = gson.fromJson(encoded, Transaction.class);
        assert decoded.equals(transaction);
    }
}
