/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.osgi.parser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.osgi.service.FrameworkBootstrapService;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.osgi.spi.util.UnmodifiableDictionary;

/**
 * The OSGi subsystem state.
 *
 * @author Thomas.Diesler@jboss.com
 * @author David Bosschaert
 * @since 13-Oct-2010
 */
public final class SubsystemState implements Serializable, Service<SubsystemState> {
    private static final long serialVersionUID = 6268537612248019022L;

    public static final ServiceName SERVICE_NAME = FrameworkBootstrapService.FRAMEWORK_BASE_NAME.append("subsystemstate");
    public static final String PROP_JBOSS_OSGI_SYSTEM_MODULES = "org.jboss.osgi.system.modules";

    private final Map<String, Dictionary<String, String>> configurations = new LinkedHashMap<String, Dictionary<String, String>>();
    private final Map<String, Object> properties = new LinkedHashMap<String, Object>();
    private final List<OSGiModule> modules = new ArrayList<OSGiModule>();
    private Activation activationPolicy = Activation.LAZY;

    public static ServiceController<SubsystemState> addService(ServiceTarget serviceTarget, Activation activation) {
        SubsystemState state = new SubsystemState();
        state.setActivation(activation);

        ServiceBuilder<SubsystemState> builder = serviceTarget.addService(SERVICE_NAME, state);
        builder.setInitialMode(Mode.LAZY);
        return builder.install();
    }

    SubsystemState() {}

    @Override
    public SubsystemState getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
        // Nothing to do
    }

    @Override
    public void stop(StopContext context) {
        // Nothing to do
    }

    public Set<String> getConfigurations() {
        synchronized (configurations) {
            Collection<String> values = configurations.keySet();
            return Collections.unmodifiableSet(new HashSet<String>(values));
        }
    }

    public boolean hasConfiguration(String pid) {
        synchronized (configurations) {
            return configurations.containsKey(pid);
        }
    }

    public Dictionary<String, String> getConfiguration(String pid) {
        synchronized (configurations) {
            return configurations.get(pid);
        }
    }

    public Dictionary<String, String> putConfiguration(String pid, Dictionary<String, String> props) {
        synchronized (configurations) {
            return configurations.put(pid, new UnmodifiableDictionary<String, String>(props));
        }
    }

    public Dictionary<String, String> removeConfiguration(String pid) {
        synchronized (configurations) {
            return configurations.remove(pid);
        }
    }

    public enum Activation {
        EAGER, LAZY
    }

    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    Object setProperty(String name, Object value) {
        if (value == null)
            return properties.remove(name);
        else
            return properties.put(name, value);
    }

    public List<OSGiModule> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public void addModule(OSGiModule module) {
        modules.add(module);
    }

    public OSGiModule removeModule(String id) {
        ModuleIdentifier identifier = ModuleIdentifier.fromString(id);
        synchronized (modules) {
            for (Iterator<OSGiModule> it = modules.iterator(); it.hasNext(); ) {
                OSGiModule module = it.next();
                if (module.getIdentifier().equals(identifier)) {
                    it.remove();
                    return module;
                }
            }
            return null;
        }
    }

    public Activation getActivationPolicy() {
        return activationPolicy;
    }

    void setActivation(Activation activation) {
        this.activationPolicy = activation;
    }

    boolean isEmpty() {
        return properties.isEmpty() && modules.isEmpty() && configurations.isEmpty();
    }

    public static class OSGiModule implements Serializable {
        private static final long serialVersionUID = -2280880859263752474L;

        private final ModuleIdentifier identifier;
        private final Integer startlevel;

        OSGiModule(ModuleIdentifier identifier, Integer startlevel) {
            this.identifier = identifier;
            this.startlevel = startlevel;
        }

        public ModuleIdentifier getIdentifier() {
            return identifier;
        }

        public Integer getStartLevel() {
            return startlevel;
        }

        @Override
        public int hashCode() {
            return identifier.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof OSGiModule == false)
                return false;

            OSGiModule om = (OSGiModule) obj;
            return identifier == null ? om.identifier == null : identifier.equals(om.identifier);
        }
    }
}
