package com.philippkutsch.tuchain.blockchain;

public interface HashMiningStepInterface {
    void onHashCreated(long number, byte[] hash);
}
