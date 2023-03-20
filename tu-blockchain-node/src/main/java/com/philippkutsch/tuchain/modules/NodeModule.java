package com.philippkutsch.tuchain.modules;

import com.philippkutsch.tuchain.Node;

import javax.annotation.Nonnull;

/**
 * Abstract blockchain node module
 */
public abstract class NodeModule implements NodeEnvironment {
    protected final Node node;

    public NodeModule(@Nonnull Node node)
            throws ModuleLoadException {
        this.node = node;

        try {
            //Load dependencies
            Class<? extends NodeModule>[] dependencies = loadDependencies();
            for(Class<? extends NodeModule> dep : dependencies) {
                if(node.getModule(dep) == null) {
                    throw new ModuleLoadException(dep.getSimpleName() + " not found. Failed to load dependencies for " + this.getClass().getSimpleName());
                }
            }
        }
        catch (Exception e) {
            throw new ModuleLoadException(e);
        }
    }

    protected Class<? extends NodeModule>[] loadDependencies() {
        //noinspection unchecked
        return new Class[0];
    }
}
