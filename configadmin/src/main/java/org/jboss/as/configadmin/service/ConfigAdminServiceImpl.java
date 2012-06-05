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

package org.jboss.as.configadmin.service;

import static org.jboss.as.configadmin.ConfigAdminLogger.ROOT_LOGGER;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.as.configadmin.parser.ConfigAdminExtension;
import org.jboss.as.configadmin.parser.ConfigAdminState;
import org.jboss.as.configadmin.parser.ModelConstants;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Maintains a set of {@link Dictionary}s in the domain model keyed be persistent ID (PID).
 *
 * @author Thomas.Diesler@jboss.com
 * @author David Bosschaert
 * @since 29-Nov-2010
 */
public class ConfigAdminServiceImpl implements ConfigAdminService {
    private final InjectedValue<ConfigAdminState> injectedSubsystemState = new InjectedValue<ConfigAdminState>();
    private final InjectedValue<ModelController> injectedModelController = new InjectedValue<ModelController>();
    private final Set<ConfigAdminListener> listeners = new CopyOnWriteArraySet<ConfigAdminListener>();
    private final Executor executor = Executors.newSingleThreadExecutor();
    private ModelControllerClient controllerClient;

    private ConfigAdminServiceImpl() {
    }

    public static ServiceController<?> addService(final ServiceTarget target, final ServiceListener<Object>... listeners) {
        ConfigAdminServiceImpl service = new ConfigAdminServiceImpl();
        ServiceBuilder<?> builder = target.addService(SERVICE_NAME, service);
        builder.addDependency(ConfigAdminState.SERVICE_NAME, ConfigAdminState.class, service.injectedSubsystemState);
        builder.addDependency(Services.JBOSS_SERVER_CONTROLLER, ModelController.class, service.injectedModelController);
        builder.addListener(listeners);
        return builder.install();
    }

    @Override
    public Set<String> getConfigurations() {
        return injectedSubsystemState.getValue().getConfigurations();
    }

    @Override
    public boolean hasConfiguration(String pid) {
        return injectedSubsystemState.getValue().hasConfiguration(pid);
    }

    @Override
    public Dictionary<String, String> getConfiguration(String pid) {
        return injectedSubsystemState.getValue().getConfiguration(pid);
    }

    @Override
    public Dictionary<String, String> putConfiguration(String pid, Dictionary<String, String> newConfig) {
        // The change did not come from the DMR so we need to inform it.
        ModelNode op;

        ModelNode address = getSubsystemAddress();
        address.add(new ModelNode().set(ModelConstants.CONFIGURATION, pid));

        Dictionary<String, String> oldConfig = getConfiguration(pid);
        if (oldConfig != null) {
            op = Util.getEmptyOperation(ModelConstants.UPDATE, address);
        } else {
            op = Util.getEmptyOperation(ModelDescriptionConstants.ADD, address);
        }

        ModelNode entries = new ModelNode();
        Enumeration<String> keys = newConfig.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            entries.get(key).set(newConfig.get(key));
        }
        entries.get(SOURCE_PROPERTY_KEY).set(FROM_NONDMR_SOURCE_VALUE);
        op.get(ModelConstants.ENTRIES).set(entries);

        try {
            ModelNode node = controllerClient.execute(op);
            ModelNode outcome = node.get(ModelDescriptionConstants.OUTCOME);
            if (ModelDescriptionConstants.SUCCESS.equals(outcome.asString())) {
                putConfigurationInternal(pid, newConfig);
            } else {
                ROOT_LOGGER.cannotAddConfiguration(pid, node);
            }
        } catch (IOException ex) {
            ROOT_LOGGER.cannotAddConfiguration(ex, pid);
        }

        return oldConfig;
    }

    public void putConfigurationFromDMR(String pid, Dictionary<String, String> dictionary) {
        if (dictionary.get(SOURCE_PROPERTY_KEY) == null) {
            // The configuration originated in the DMR so mark it as such
            dictionary.put(SOURCE_PROPERTY_KEY, FROM_DMR_SOURCE_VALUE);
        }

        putConfigurationInternal(pid, dictionary);
    }

    private void putConfigurationInternal(String pid, Dictionary<String, String> dictionary) {
        injectedSubsystemState.getValue().putConfiguration(pid, dictionary);
        executor.execute(new ConfigurationModifiedService(pid, dictionary));
    }

    @Override
    public Dictionary<String, String> removeConfiguration(String pid) {
        Dictionary<String, String> oldConfig = getConfiguration(pid);
        if (oldConfig != null) {
            ModelNode address = getSubsystemAddress();
            address.add(new ModelNode().set(ModelConstants.CONFIGURATION, pid));
            ModelNode op = Util.getEmptyOperation(ModelDescriptionConstants.REMOVE, address);
            try {
                ModelNode node = controllerClient.execute(op);
                ModelNode outcome = node.get(ModelDescriptionConstants.OUTCOME);
                if (ModelDescriptionConstants.SUCCESS.equals(outcome.asString())) {
                    removeConfigurationInternal(pid);
                } else {
                    ROOT_LOGGER.cannotRemoveConfiguration(pid, node);
                }
            } catch (IOException ex) {
                ROOT_LOGGER.cannotRemoveConfiguration(ex, pid);
            }
        }
        return oldConfig;
    }

    public void removeConfigurationFromDMR(String pid) {
        removeConfigurationInternal(pid);
    }

    private void removeConfigurationInternal(String pid) {
        injectedSubsystemState.getValue().removeConfiguration(pid);
        executor.execute(new ConfigurationModifiedService(pid, null));
    }

    @Override
    public void start(StartContext context) throws StartException {
        ServiceController<?> controller = context.getController();
        ROOT_LOGGER.debugf("Starting: %s in mode %s", controller.getName(), controller.getMode());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        controllerClient = injectedModelController.getValue().createClient(executor);
    }

    @Override
    public void stop(StopContext context) {
        ServiceController<?> controller = context.getController();
        ROOT_LOGGER.debugf("Stopping: %s in mode %s", controller.getName(), controller.getMode());
    }

    @Override
    public ConfigAdminService getValue() throws IllegalStateException {
        return this;
    }

    @Override
    public void addListener(ConfigAdminListener listener) {
        ROOT_LOGGER.debugf("Add listener: %s", listener);
        listeners.add(listener);

        // Call the newly registered listener with a potentially null dictionary for every registered pid
        Set<String> pids = listener.getPIDs();
        if (pids != null) {
            for (String pid : pids) {
                Dictionary<String, String> props = getConfiguration(pid);
                listener.configurationModified(pid, props);
            }
        }
    }

    @Override
    public void removeListener(ConfigAdminListener listener) {
        ROOT_LOGGER.debugf("Remove listener: %s", listener);
        listeners.remove(listener);
    }

    private ModelNode getSubsystemAddress() {
        ModelNode address = new ModelNode();
        address.add(new ModelNode().set(ModelDescriptionConstants.SUBSYSTEM, ConfigAdminExtension.SUBSYSTEM_NAME));
        return address;
    }

    /**
     * Asynchronously update the persistent configuration and call the set of registered listeners
     */
    class ConfigurationModifiedService implements Runnable {

        private final String pid;
        private final Dictionary<String, String> props;

        private ConfigurationModifiedService(String pid, Dictionary<String, String> props) {
            this.pid = pid;
            this.props = props;
        }

        @Override
        public void run() {
            ROOT_LOGGER.debugf("Updating configuration: %s", pid);
            Set<ConfigAdminListener> snapshot = new HashSet<ConfigAdminListener>(listeners);
            for (ConfigAdminListener aux : snapshot) {
                Set<String> pids = aux.getPIDs();
                if (pids == null || pids.contains(pid)) {
                    try {
                        aux.configurationModified(pid, props);
                    } catch (Exception ex) {
                        ROOT_LOGGER.configurationListenerError(ex, aux);
                    }
                }
            }
        }
    }
}
