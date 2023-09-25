/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan;

import java.util.ServiceLoader;

import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.jboss.logging.Logger;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Validates loading of services.
 *
 * @author Paul Ferraro
 */
public class ServiceLoaderTestCase {
    private static final Logger LOGGER = Logger.getLogger(ServiceLoaderTestCase.class);

    private static <T> void load(Class<T> targetClass) {
        ServiceLoader.load(targetClass, ServiceLoaderTestCase.class.getClassLoader())
                .forEach(object -> LOGGER.trace("\t" + object.getClass().getName()));
    }

    @Test
    public void load() {
        load(Externalizer.class);
        load(TwoWayKey2StringMapper.class);
    }
}
