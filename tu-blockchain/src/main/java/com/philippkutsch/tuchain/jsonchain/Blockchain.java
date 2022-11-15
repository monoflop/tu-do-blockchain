package com.philippkutsch.tuchain.jsonchain;


import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class Blockchain {
    private final List<HashedBlock> blockList;

    public Blockchain(@Nonnull List<HashedBlock> initialState) {
        this.blockList = new ArrayList<>(initialState);
    }

    public void addBlock(@Nonnull HashedBlock hashedBlock) {
        blockList.add(hashedBlock);
    }

    @Nonnull
    public List<HashedBlock> getBlockchain() {
        return blockList;
    }

    @Nonnull
    public HashedBlock getLastBlock() {
        return blockList.get(blockList.size() - 1);
    }
}
