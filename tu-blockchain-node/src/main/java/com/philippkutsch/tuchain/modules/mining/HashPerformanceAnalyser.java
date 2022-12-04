package com.philippkutsch.tuchain.modules.mining;

//Ticks on every generated hash
//Calculate hash performance
public class HashPerformanceAnalyser implements HashMiningStepInterface {
    long hashCounter;
    long lastTimeStamp;

    public HashPerformanceAnalyser() {
        this.hashCounter = 0;
        this.lastTimeStamp = System.currentTimeMillis();
    }

    @Override
    public void onHashCreated(long number, byte[] hash) {
        hashCounter++;
        if(System.currentTimeMillis() > lastTimeStamp + 1000) {
            System.out.println("Generated " + number + " hashes with " + (hashCounter / 1000.0) + " kh/s");
            hashCounter = 0;
            lastTimeStamp = System.currentTimeMillis();
        }
    }
}
