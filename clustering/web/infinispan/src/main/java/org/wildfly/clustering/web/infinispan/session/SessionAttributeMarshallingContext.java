/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.infinispan.session;

import java.io.Externalizable;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.as.clustering.marshalling.MarshallingConfigurationFactory;
import org.jboss.as.clustering.marshalling.SimpleClassTable;
import org.jboss.as.clustering.marshalling.VersionedMarshallingConfiguration;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.modules.Module;

/**
 * Versioned session attribute marshalling context.
 * @author Paul Ferraro
 */
public class SessionAttributeMarshallingContext implements VersionedMarshallingConfiguration {
    private static final int CURRENT_VERSION = 1;

    private final Map<Integer, MarshallingConfiguration> configurations = new ConcurrentHashMap<>();

    public SessionAttributeMarshallingContext(Module module) {
        MarshallingConfiguration configuration = MarshallingConfigurationFactory.createMarshallingConfiguration(module.getModuleLoader());
        configuration.setClassTable(new SimpleClassTable(Serializable.class, Externalizable.class));
        this.configurations.put(CURRENT_VERSION, configuration);
    }

    @Override
    public int getCurrentMarshallingVersion() {
        return CURRENT_VERSION;
    }

    @Override
    public MarshallingConfiguration getMarshallingConfiguration(int version) {
        MarshallingConfiguration config = this.configurations.get(version);
        if (config == null) {
            throw new IllegalArgumentException(String.valueOf(version));
        }
        return config;
    }
}
