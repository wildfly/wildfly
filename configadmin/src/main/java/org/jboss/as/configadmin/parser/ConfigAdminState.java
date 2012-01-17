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

package org.jboss.as.configadmin.parser;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * The ConfigAdmin subsystem state.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 13-Oct-2010
 */
public class ConfigAdminState extends Observable implements Serializable, Service<ConfigAdminState> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("configadmin", "subsystemstate");

    private final Map<String, Dictionary<String, String>> configurations = new LinkedHashMap<String, Dictionary<String, String>>();

    public static ServiceController<ConfigAdminState> addService(ServiceTarget serviceTarget) {
        ConfigAdminState state = new ConfigAdminState();

        ServiceBuilder<ConfigAdminState> builder = serviceTarget.addService(SERVICE_NAME, state);
        builder.setInitialMode(Mode.LAZY);
        return builder.install();
    }

    static ConfigAdminState getSubsystemState(OperationContext context) {
        ServiceController<?> controller = context.getServiceRegistry(true).getService(ConfigAdminState.SERVICE_NAME);
        return controller != null ? (ConfigAdminState) controller.getValue() : null;
    }

    ConfigAdminState() {}

    @Override
    public ConfigAdminState getValue() throws IllegalStateException, IllegalArgumentException {
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
        try {
            synchronized (configurations) {
                return configurations.put(pid, new UnmodifiableDictionary<String, String>(props));
            }
        } finally {
            notifyObservers(new ChangeEvent(ChangeType.CONFIG, false, pid));
        }
    }

    public Dictionary<String, String> removeConfiguration(String pid) {
        try {
            synchronized (configurations) {
                return configurations.remove(pid);
            }
        } finally {
            notifyObservers(new ChangeEvent(ChangeType.CONFIG, true, pid));
        }
    }

    @Override
    public void notifyObservers(Object arg) {
        setChanged();
        super.notifyObservers(arg);
    }

    public static class ChangeEvent {
        private final String id;
        private final boolean isRemoved;
        private final ChangeType type;

        public ChangeEvent(ChangeType type, boolean isRemoved, String id) {
            this.type = type;
            this.isRemoved = isRemoved;
            this.id = id;
        }

        public ChangeType getType() {
            return type;
        }

        public boolean isRemoved() {
            return isRemoved;
        }

        public String getId() {
            return id;
        }
    }

    public enum ChangeType { CONFIG };
}
