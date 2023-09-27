/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan;

import java.util.Formatter;
import java.util.ServiceLoader;

import org.infinispan.protostream.SerializationContextInitializer;
import org.jboss.logging.Logger;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.jboss.ClassTableContributor;

/**
 * Validates loading of services.
 * @author Paul Ferraro
 */
public class ServiceLoaderTestCase {
    private static final Logger LOGGER = Logger.getLogger(ServiceLoaderTestCase.class);

    @Test
    public void load() {
        load(Formatter.class);
        load(Externalizer.class);
        load(ClassTableContributor.class);
        load(SerializationContextInitializer.class);
    }

    private static <T> void load(Class<T> targetClass) {
        ServiceLoader.load(targetClass, ServiceLoaderTestCase.class.getClassLoader())
                .forEach(object -> LOGGER.tracef("\t" + object.getClass().getName()));
    }
}
