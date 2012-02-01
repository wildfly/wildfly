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

import org.jboss.as.controller.OperationContext;
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;

/**
 * The OSGi subsystem state.
 *
 * @author Thomas.Diesler@jboss.com
 * @author David Bosschaert
 * @since 13-Oct-2010
 */
public class SubsystemState  extends Observable implements Serializable, Service<SubsystemState> {

    public static final ServiceName SERVICE_NAME = FrameworkBootstrapService.FRAMEWORK_BASE_NAME.append("subsystemstate");
    public static final String PROP_JBOSS_OSGI_SYSTEM_MODULES = "org.jboss.osgi.system.modules";
    public static final String PROP_JBOSS_OSGI_SYSTEM_PACKAGES = "org.jboss.osgi.system.packages";
    public static final String PROP_JBOSS_OSGI_SYSTEM_MODULES_EXTRA = "org.jboss.osgi.system.modules.extra";

    private final Map<String, Object> properties = new LinkedHashMap<String, Object>();
    private final List<OSGiCapability> capabilities = new ArrayList<OSGiCapability>();
    private volatile Activation activationPolicy = Activation.LAZY;

    static final Activation DEFAULT_ACTIVATION = Activation.LAZY;

    public static ServiceController<SubsystemState> addService(ServiceTarget serviceTarget, Activation activation) {
        SubsystemState state = new SubsystemState();
        state.setActivation(activation);

        ServiceBuilder<SubsystemState> builder = serviceTarget.addService(SERVICE_NAME, state);
        builder.setInitialMode(Mode.LAZY);
        return builder.install();
    }

    static SubsystemState getSubsystemState(OperationContext context) {
        ServiceController<?> controller = context.getServiceRegistry(true).getService(SubsystemState.SERVICE_NAME);
        return controller != null ? (SubsystemState) controller.getValue() : null;
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

    public enum Activation {
        EAGER, LAZY
    }

    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    Object setProperty(String name, Object value) {
        try {
            if (value == null)
                return properties.remove(name);
            else
                return properties.put(name, value);
        } finally {
            notifyObservers(new ChangeEvent(ChangeType.PROPERTY, value == null, name));
        }
    }

    public List<OSGiCapability> getCapabilities() {
        return Collections.unmodifiableList(capabilities);
    }

    public void addCapability(OSGiCapability module) {
        capabilities.add(module);
        notifyObservers(new ChangeEvent(ChangeType.CAPABILITY, false, module.getIdentifier().toString()));
    }

    public OSGiCapability removeCapability(String id) {
        ModuleIdentifier identifier = ModuleIdentifier.fromString(id);
        synchronized (capabilities) {
            for (Iterator<OSGiCapability> it = capabilities.iterator(); it.hasNext(); ) {
                OSGiCapability module = it.next();
                if (module.getIdentifier().equals(identifier)) {
                    it.remove();
                    notifyObservers(new ChangeEvent(ChangeType.CAPABILITY, true, identifier.toString()));
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
        if (activationPolicy == activation)
            return;

        try {
            activationPolicy = activation;
        } finally {
            notifyObservers(new ChangeEvent(ChangeType.ACTIVATION, false, activation.name()));
        }
    }

    @Override
    public void notifyObservers(Object arg) {
        setChanged();
        super.notifyObservers(arg);
    }

    public static class OSGiCapability implements Serializable {
        private static final long serialVersionUID = -2280880859263752474L;

        private final String identifier;
        private final Integer startlevel;

        public OSGiCapability(String identifier, Integer startlevel) {
            this.identifier = identifier;
            this.startlevel = startlevel;
        }

        public String getIdentifier() {
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
            if (obj instanceof OSGiCapability == false)
                return false;

            OSGiCapability om = (OSGiCapability) obj;
            return identifier == null ? om.identifier == null : identifier.equals(om.identifier);
        }
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

    public enum ChangeType { ACTIVATION, PROPERTY, CAPABILITY };
}
