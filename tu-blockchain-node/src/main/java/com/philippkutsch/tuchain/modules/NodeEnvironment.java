package com.philippkutsch.tuchain.modules;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutorService;

public interface NodeEnvironment {
    @Nonnull
    ExecutorService getExecutorService();
}
