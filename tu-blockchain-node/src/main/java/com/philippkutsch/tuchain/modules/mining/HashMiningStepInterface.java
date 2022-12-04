package com.philippkutsch.tuchain.modules.mining;

public interface HashMiningStepInterface {
    void onHashCreated(long number, byte[] hash);
}
