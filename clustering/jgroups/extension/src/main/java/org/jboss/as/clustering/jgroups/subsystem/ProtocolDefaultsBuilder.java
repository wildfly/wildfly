/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.clustering.jgroups.logging.JGroupsLogger.ROOT_LOGGER;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.clustering.jgroups.ProtocolDefaults;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.Value;
import org.jgroups.conf.ProtocolStackConfigurator;
import org.jgroups.conf.XmlConfigurator;
import org.wildfly.clustering.service.AsynchronousServiceBuilder;
import org.wildfly.clustering.service.Builder;

/**
 * Service that provides protocol property defaults per protocol type.
 * @author Paul Ferraro
 */
public class ProtocolDefaultsBuilder implements Builder<ProtocolDefaults>, Value<ProtocolDefaults>, ProtocolDefaults {

    static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(JGroupsExtension.SUBSYSTEM_NAME, "defaults");

    private static final String DEFAULTS = "jgroups-defaults.xml";

    private static ProtocolStackConfigurator load(String resource) throws IllegalStateException {
        URL url = find(resource, JGroupsExtension.class.getClassLoader());
        ROOT_LOGGER.debugf("Loading JGroups protocol defaults from %s", url.toString());
        try {
            return XmlConfigurator.getInstance(url);
        } catch (IOException e) {
            throw new IllegalArgumentException(JGroupsLogger.ROOT_LOGGER.parserFailure(url));
        }
    }

    private static URL find(String resource, ClassLoader... loaders) {
        for (ClassLoader loader: loaders) {
            if (loader != null) {
                URL url = loader.getResource(resource);
                if (url != null) {
                    return url;
                }
            }
        }
        throw new IllegalArgumentException(JGroupsLogger.ROOT_LOGGER.notFound(resource));
    }

    private final String resource;
    private final Map<String, Map<String, String>> map = new HashMap<>();

    public ProtocolDefaultsBuilder() {
        this(DEFAULTS);
    }

    public ProtocolDefaultsBuilder(String resource) {
        this.resource = resource;
    }

    @Override
    public ServiceName getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public ServiceBuilder<ProtocolDefaults> build(ServiceTarget target) {
        return new AsynchronousServiceBuilder<>(SERVICE_NAME, new ValueService<>(this)).build(target);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.value.Value#getValue()
     */
    @Override
    public ProtocolDefaults getValue() {
        ProtocolStackConfigurator configurator = load(ProtocolDefaultsBuilder.this.resource);
        for (org.jgroups.conf.ProtocolConfiguration config: configurator.getProtocolStack()) {
            this.map.put(config.getProtocolName(), Collections.unmodifiableMap(config.getProperties()));
        }
        return this;
    }

    @Override
    public Map<String, String> getProperties(String protocol) {
        Map<String, String> properties = this.map.get(protocol);
        return (properties != null) ? Collections.unmodifiableMap(properties) : Collections.<String, String>emptyMap();
    }
}
