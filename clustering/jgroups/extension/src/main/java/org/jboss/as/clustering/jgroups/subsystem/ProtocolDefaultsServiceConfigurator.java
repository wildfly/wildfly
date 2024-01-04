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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.jgroups.ProtocolDefaults;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jgroups.Global;
import org.jgroups.conf.ProtocolStackConfigurator;
import org.jgroups.conf.XmlConfigurator;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.service.AsyncServiceConfigurator;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.SimpleServiceNameProvider;

/**
 * Service that provides protocol property defaults per protocol type.
 * @author Paul Ferraro
 */
public class ProtocolDefaultsServiceConfigurator extends SimpleServiceNameProvider implements ServiceConfigurator, ProtocolDefaults, Supplier<ProtocolDefaults> {

    static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(JGroupsExtension.SUBSYSTEM_NAME, "defaults");
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

    private final String resource;
    private final Map<Class<? extends Protocol>, Map<String, String>> map = new IdentityHashMap<>();

    public ProtocolDefaultsServiceConfigurator() {
        this(DEFAULTS);
    }

    public ProtocolDefaultsServiceConfigurator(String resource) {
        super(SERVICE_NAME);
        this.resource = resource;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = new AsyncServiceConfigurator(SERVICE_NAME).build(target);
        Consumer<ProtocolDefaults> defaults = builder.provides(SERVICE_NAME);
        Service service = new FunctionalService<>(defaults, Function.identity(), this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public ProtocolDefaults get() {
        ProtocolStackConfigurator configurator = load(ProtocolDefaultsServiceConfigurator.this.resource);
        try {
            for (org.jgroups.conf.ProtocolConfiguration config: configurator.getProtocolStack()) {
                String protocolClassName = Global.PREFIX + config.getProtocolName();
                Class<? extends Protocol> protocolClass = Protocol.class.getClassLoader().loadClass(protocolClassName).asSubclass(Protocol.class);
                this.map.put(protocolClass, Collections.unmodifiableMap(config.getProperties()));
            }
            return this;
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Map<String, String> getProperties(Class<? extends Protocol> protocolClass) {
        Map<String, String> properties = this.map.get(protocolClass);
        return (properties != null) ? Collections.unmodifiableMap(properties) : Collections.<String, String>emptyMap();
    }
}
