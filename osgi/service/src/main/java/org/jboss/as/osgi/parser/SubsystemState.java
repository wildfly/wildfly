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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.modules.ModuleIdentifier;
import org.jboss.osgi.spi.util.UnmodifiableDictionary;

/**
 * The OSGi subsystem state.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 13-Oct-2010
 */
public final class SubsystemState implements Serializable {

    private static final long serialVersionUID = 6268537612248019022L;

    public static final String PROP_JBOSS_OSGI_SYSTEM_MODULES = "org.jboss.osgi.system.modules";

    private final Map<String, Dictionary<String, String>> configurations = new LinkedHashMap<String, Dictionary<String, String>>();
    private final Map<String, Object> properties = new LinkedHashMap<String, Object>();
    private final List<OSGiModule> modules = new ArrayList<OSGiModule>();
    private Activation activationPolicy = Activation.LAZY;

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

    void addProperty(String name, Object value) {
        properties.put(name, value);
    }

    public List<OSGiModule> getModules() {
        return Collections.unmodifiableList(modules);
    }

    void addModule(OSGiModule module) {
        modules.add(module);
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
    }
}
