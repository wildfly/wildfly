/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.marshalling.jboss;

import java.util.AbstractMap;

import org.infinispan.commons.dataconversion.MediaType;
import org.jboss.marshalling.ClassResolver;
import org.wildfly.clustering.infinispan.marshalling.UserMarshaller;
import org.wildfly.clustering.marshalling.jboss.JBossByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.MarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingConfigurationRepository;

/**
 * @author Paul Ferraro
 */
public class JBossMarshaller extends UserMarshaller {

    public JBossMarshaller(ClassResolver resolver, ClassLoader loader) {
        this(new SimpleMarshallingConfigurationRepository(JBossMarshallingVersion.class, JBossMarshallingVersion.CURRENT, new AbstractMap.SimpleImmutableEntry<>(resolver, loader)), loader);
    }

    public JBossMarshaller(MarshallingConfigurationRepository repository, ClassLoader loader) {
        super(MediaType.APPLICATION_JBOSS_MARSHALLING, new JBossByteBufferMarshaller(repository, loader));
    }
}
