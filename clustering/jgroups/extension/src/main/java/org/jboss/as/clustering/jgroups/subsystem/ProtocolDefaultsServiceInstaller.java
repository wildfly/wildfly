/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.clustering.jgroups.logging.JGroupsLogger.ROOT_LOGGER;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jboss.as.clustering.jgroups.ProtocolDefaults;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.RequirementServiceTarget;
import org.jboss.msc.service.ServiceController;
import org.jgroups.Global;
import org.jgroups.conf.ProtocolStackConfigurator;
import org.jgroups.conf.XmlConfigurator;
import org.jgroups.stack.Protocol;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Service that provides protocol property defaults per protocol type.
 * @author Paul Ferraro
 */
public class ProtocolDefaultsServiceInstaller implements ServiceInstaller {

    private static final String DEFAULTS = "jgroups-defaults.xml";

    private static ProtocolStackConfigurator load(String resource) throws IllegalStateException {
        URL url = find(resource, JGroupsExtension.class.getClassLoader());
        ROOT_LOGGER.debugf("Loading JGroups protocol defaults from %s", url.toString());
        try (InputStream input = url.openStream()) {
            return XmlConfigurator.getInstance(input);
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

    @Override
    public ServiceController<?> install(RequirementServiceTarget target) {
        Supplier<ProtocolDefaults> factory = ResourceProtocolDefaults::new;
        return ServiceInstaller.builder(factory).blocking()
                .provides(ProtocolDefaults.SERVICE_NAME)
                .build()
                .install(target);
    }

    private static class ResourceProtocolDefaults implements ProtocolDefaults {
        private final Map<Class<? extends Protocol>, Map<String, String>> protocols = new IdentityHashMap<>();

        ResourceProtocolDefaults() {
            this(DEFAULTS);
        }

        ResourceProtocolDefaults(String resource) {
            ProtocolStackConfigurator configurator = load(resource);
            try {
                for (org.jgroups.conf.ProtocolConfiguration config: configurator.getProtocolStack()) {
                    String protocolClassName = Global.PREFIX + config.getProtocolName();
                    Class<? extends Protocol> protocolClass = Protocol.class.getClassLoader().loadClass(protocolClassName).asSubclass(Protocol.class);
                    this.protocols.put(protocolClass, Collections.unmodifiableMap(config.getProperties()));
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        public Map<String, String> getProperties(Class<? extends Protocol> protocolClass) {
            return this.protocols.getOrDefault(protocolClass, Map.of());
        }
    }
}
