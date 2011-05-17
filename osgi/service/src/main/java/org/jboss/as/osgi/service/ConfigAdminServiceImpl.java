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

package org.jboss.as.osgi.service;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.as.osgi.parser.SubsystemState;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Maintains a set of {@link Dictionary}s in the domain model keyd be persistent ID (PID).
 *
 * @author Thomas.Diesler@jboss.com
 * @since 29-Nov-2010
 */
public class ConfigAdminServiceImpl implements ConfigAdminService {

    private static final Logger log = Logger.getLogger("org.jboss.as.osgi");

    //private final InjectedValue<ServerConfigurationPersister> injectedConfigPersister = new InjectedValue<ServerConfigurationPersister>();
    private final Set<ConfigAdminListener> listeners = new CopyOnWriteArraySet<ConfigAdminListener>();
    private final AtomicLong updateCount = new AtomicLong();
    private final SubsystemState subsystemState;
    private ServiceContainer serviceContainer;

    ConfigAdminServiceImpl(SubsystemState subsystemState) {
        this.subsystemState = subsystemState;
    }

    public static void addService(final ServiceTarget target, SubsystemState subsystemState) {
        ConfigAdminServiceImpl service = new ConfigAdminServiceImpl(subsystemState);
        ServiceBuilder<?> builder = target.addService(ConfigAdminService.SERVICE_NAME, service);
        //builder.addDependency(ServerConfigurationPersister.SERVICE_NAME, ServerConfigurationPersister.class, service.injectedConfigPersister);
        builder.install();
    }

    @Override
    public Set<String> getConfigurations() {
        return subsystemState.getConfigurations();
    }

    @Override
    public boolean hasConfiguration(String pid) {
        return subsystemState.hasConfiguration(pid);
    }

    @Override
    public Dictionary<String, String> getConfiguration(String pid) {
        return subsystemState.getConfiguration(pid);
    }

    @Override
    public Dictionary<String, String> putConfiguration(String pid, Dictionary<String, String> newconfig) {
        Dictionary<String, String> oldconfig = subsystemState.putConfiguration(pid, newconfig);
        new ConfigurationModifiedService(pid, newconfig).configurationModified();
        return oldconfig;
    }

    @Override
    public Dictionary<String, String> removeConfiguration(String pid) {
        Dictionary<String, String> oldconfig = subsystemState.removeConfiguration(pid);
        new ConfigurationModifiedService(pid, oldconfig).configurationModified();
        return oldconfig;
    }

    @Override
    public void start(StartContext context) throws StartException {
        log.debugf("Starting ConfigAdminService");
        serviceContainer = context.getController().getServiceContainer();
    }

    @Override
    public void addListener(ConfigAdminListener listener) {
        log.debugf("Add listener: %s", listener);
        listeners.add(listener);

        // Call the newly registered listener with a potentially null dictionaly for every registered pid
        Set<String> pids = listener.getPIDs();
        if (pids != null) {
            for (String pid : pids) {
                Dictionary<String, String> props = subsystemState.getConfiguration(pid);
                listener.configurationModified(pid, props);
            }
        }
    }

    @Override
    public void removeListener(ConfigAdminListener listener) {
        log.debugf("Remove listener: %s", listener);
        listeners.remove(listener);
    }

    @Override
    public void stop(StopContext context) {
        log.debugf("Stopping ConfigAdminService");
    }

    @Override
    public ConfigAdminService getValue() throws IllegalStateException {
        return this;
    }

    /**
     * A service that asynchronously updates the persistet configuration and calls the set of registered listeners
     */
    class ConfigurationModifiedService extends AbstractService<Void> {

        private final String pid;
        private final Dictionary<String, String> props;

        private ConfigurationModifiedService(String pid, Dictionary<String, String> props) {
            this.pid = pid;
            this.props = props;
        }

        public void configurationModified() {
            String updatePart = new Long(updateCount.incrementAndGet()).toString();
            ServiceName serviceName = ServiceName.of(ConfigurationModifiedService.class.getName(), updatePart);
            ServiceBuilder<?> builder = serviceContainer.addService(serviceName, this);
            builder.install();
        }

        @Override
        public void start(StartContext context) throws StartException {
            //injectedConfigPersister.getValue().configurationModified();
            Set<ConfigAdminListener> snapshot = new HashSet<ConfigAdminListener>(listeners);
            for (ConfigAdminListener aux : snapshot) {
                Set<String> pids = aux.getPIDs();
                if (pids == null || pids.contains(pid)) {
                    try {
                        aux.configurationModified(pid, props);
                    } catch (Exception ex) {
                        log.errorf(ex, "Error in configuration listener: %s", aux);
                    }
                }
            }
            // Remove this service again after it has called the listeners
            context.getController().setMode(Mode.REMOVE);
        }
    }
}
