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

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jboss.as.clustering.jgroups.ProtocolDefaults;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jgroups.conf.ProtocolStackConfigurator;
import org.jgroups.conf.XmlConfigurator;

import static org.jboss.as.clustering.jgroups.JGroupsLogger.ROOT_LOGGER;
import static org.jboss.as.clustering.jgroups.JGroupsMessages.MESSAGES;

/**
 * Service that provides protocol property defaults per protocol type.
 * @author Paul Ferraro
 */
public class ProtocolDefaultsService implements Service<ProtocolDefaults> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(JGroupsExtension.SUBSYSTEM_NAME, "defaults");

    private static final String DEFAULTS = "jgroups-defaults.xml";

    private final Executor executor = Executors.newCachedThreadPool();
    private final String resource;
    private volatile ProtocolDefaults defaults;

    public ProtocolDefaultsService() {
        this(DEFAULTS);
    }

    public ProtocolDefaultsService(String resource) {
        this.resource = resource;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.value.Value#getValue()
     */
    @Override
    public ProtocolDefaults getValue() {
        return this.defaults;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#start(org.jboss.msc.service.StartContext)
     */
    @Override
    public void start(final StartContext context) throws StartException {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    ProtocolDefaultsService.this.start();
                    context.complete();
                } catch (StartException e) {
                    context.failed(e);
                }
            }
        };
        context.asynchronous();
        this.executor.execute(task);
    }

    void start() throws StartException {
        ProtocolStackConfigurator configurator = load(ProtocolDefaultsService.this.resource);
        Defaults defaults = new Defaults();
        for (org.jgroups.conf.ProtocolConfiguration config: configurator.getProtocolStack()) {
            defaults.add(config.getProtocolName(), config.getProperties());
        }
        ProtocolDefaultsService.this.defaults = defaults;
    }

    private static ProtocolStackConfigurator load(String resource) throws StartException {
        URL url = find(resource, JGroupsExtension.class.getClassLoader());
        ROOT_LOGGER.debugf("Loading JGroups protocol defaults from %s", url.toString());
        try {
            return XmlConfigurator.getInstance(url);
        } catch (IOException e) {
            throw new StartException(MESSAGES.parserFailure(url));
        }
    }

    private static URL find(String resource, ClassLoader... loaders) throws StartException {
        for (ClassLoader loader: loaders) {
            if (loader != null) {
                URL url = loader.getResource(resource);
                if (url != null) {
                    return url;
                }
            }
        }
        throw new StartException(MESSAGES.notFound(resource));
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#stop(org.jboss.msc.service.StopContext)
     */
    @Override
    public void stop(StopContext context) {
        this.defaults = null;
    }

    static class Defaults implements ProtocolDefaults {
        private final Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();

        void add(String protocol, Map<String, String> properties) {
            this.map.put(protocol, Collections.unmodifiableMap(properties));
        }

        /**
         * {@inheritDoc}
         * @see org.jboss.as.clustering.jgroups.ProtocolDefaults#getProperties(java.lang.String)
         */
        @Override
        public Map<String, String> getProperties(String protocol) {
            Map<String, String> properties = this.map.get(protocol);
            return (properties != null) ? Collections.unmodifiableMap(properties) : Collections.<String, String>emptyMap();
        }
    }
}
