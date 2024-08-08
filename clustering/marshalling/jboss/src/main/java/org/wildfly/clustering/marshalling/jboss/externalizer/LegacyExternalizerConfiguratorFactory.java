/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.jboss.externalizer;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.security.PrivilegedAction;
import java.util.ServiceLoader;
import java.util.function.UnaryOperator;

import org.jboss.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.jboss.ExternalizerProvider;
import org.wildfly.clustering.marshalling.jboss.MarshallingConfigurationBuilder;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Loads legacy {@link Externalizer} instances.
 * @author Paul Ferraro
 */
@Deprecated
public class LegacyExternalizerConfiguratorFactory implements UnaryOperator<MarshallingConfigurationBuilder> {

    private final ClassLoader loader;

    public LegacyExternalizerConfiguratorFactory(ClassLoader loader) {
        this.loader = loader;
    }

    @Override
    public MarshallingConfigurationBuilder apply(MarshallingConfigurationBuilder builder) {
        ClassLoader loader = this.loader;
        return WildFlySecurityManager.doUnchecked(new PrivilegedAction<>() {
            @Override
            public MarshallingConfigurationBuilder run() {
                for (org.wildfly.clustering.marshalling.Externalizer<Object> externalizer : ServiceLoader.load(org.wildfly.clustering.marshalling.Externalizer.class, loader)) {
                    builder.register(new LegacyExternalizerProvider(externalizer));
                }
                return builder;
            }
        });
    }

    private static class LegacyExternalizerProvider implements ExternalizerProvider, Externalizer {
        private static final long serialVersionUID = 4246423698603345459L;

        private final org.wildfly.clustering.marshalling.Externalizer<Object> externalizer;

        LegacyExternalizerProvider(org.wildfly.clustering.marshalling.Externalizer<Object> externalizer) {
            this.externalizer = externalizer;
        }

        @Override
        public Class<?> getType() {
            return this.externalizer.getTargetClass();
        }

        @Override
        public Externalizer getExternalizer() {
            return this;
        }

        @Override
        public void writeExternal(Object subject, ObjectOutput output) throws IOException {
            this.externalizer.writeObject(output, subject);
        }

        @Override
        public Object createExternal(Class<?> subjectType, ObjectInput input) throws IOException, ClassNotFoundException {
            return this.externalizer.readObject(input);
        }
    }
}
