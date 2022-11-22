package com.philippkutsch.tuchain;

public interface HashMiningStepInterface {
    void onHashCreated(long number, byte[] hash);
}
