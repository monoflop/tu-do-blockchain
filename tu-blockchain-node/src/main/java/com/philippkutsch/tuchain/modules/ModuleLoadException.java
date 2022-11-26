package com.philippkutsch.tuchain.modules;

public class ModuleLoadException extends Exception {
    public ModuleLoadException() {
        super();
    }

    public ModuleLoadException(Throwable throwable) {
        super(throwable);
    }

    public ModuleLoadException(String message) {
        super(message);
    }
}
