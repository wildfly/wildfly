/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
