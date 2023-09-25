/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.PrivilegedAction;

import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractSerializationContextInitializer implements SerializationContextInitializer, PrivilegedAction<FileDescriptorSource> {

    private final String resourceName;
    private final ClassLoader loader;

    protected AbstractSerializationContextInitializer() {
        this(null);
    }

    protected AbstractSerializationContextInitializer(String resourceName) {
        this(resourceName, null);
    }

    protected AbstractSerializationContextInitializer(String resourceName, ClassLoader loader) {
        this.resourceName = (resourceName == null) ? this.getClass().getPackage().getName() + ".proto" : resourceName;
        this.loader = (loader == null) ? WildFlySecurityManager.getClassLoaderPrivileged(this.getClass()) : loader;
    }

    @Deprecated
    @Override
    public final String getProtoFileName() {
        return null;
    }

    @Deprecated
    @Override
    public final String getProtoFile() {
        return null;
    }

    @Override
    public void registerSchema(SerializationContext context) {
        context.registerProtoFiles(WildFlySecurityManager.doUnchecked(this));
    }

    @Override
    public FileDescriptorSource run() {
        try {
            return FileDescriptorSource.fromResources(this.loader, this.resourceName);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String toString() {
        return this.resourceName;
    }
}
