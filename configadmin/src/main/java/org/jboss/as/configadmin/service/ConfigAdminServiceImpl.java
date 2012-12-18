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

import static org.jboss.as.configadmin.ConfigAdminLogger.LOGGER;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.configadmin.ConfigAdminListener;
import org.jboss.as.configadmin.parser.ConfigAdminExtension;
import org.jboss.as.configadmin.parser.ModelConstants;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.spi.util.UnmodifiableDictionary;

/**
 * Maintains a set of {@link Dictionary}s in the domain model keyed be persistent ID (PID).
 *
 * @author Thomas.Diesler@jboss.com
 * @author David Bosschaert
 * @since 29-Nov-2010
 */
public class ConfigAdminServiceImpl implements ConfigAdminService, ConfigAdminInternal, Service<ConfigAdminInternal> {

    static final String TRANSIENT_PROPERTY_SKIP_MANAGEMENT_OPERATION = ".transient.skip.mgmnt.op";

    private final InjectedValue<ModelController> injectedModelController = new InjectedValue<ModelController>();

    private final Set<ConfigAdminListener> listeners = new CopyOnWriteArraySet<ConfigAdminListener>();
    private final ConfigAdminState configurations = new ConfigAdminState();
    private ModelControllerClient controllerClient;
    private ExecutorService mgmntOperationExecutor;
    private ExecutorService asyncListnersExecutor;

    private ConfigAdminServiceImpl() {
    }

    public static ServiceController<ConfigAdminInternal> addService(final ServiceTarget target, final ServiceListener<Object>... listeners) {
        ConfigAdminServiceImpl service = new ConfigAdminServiceImpl();
        ServiceBuilder<ConfigAdminInternal> builder = target.addService(SERVICE_NAME, service);
        builder.addDependency(Services.JBOSS_SERVER_CONTROLLER, ModelController.class, service.injectedModelController);
        builder.addListener(listeners);
        return builder.install();
    }

    @Override
    public void start(StartContext context) throws StartException {
        mgmntOperationExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable run) {
                Thread thread = new Thread(run);
                thread.setName("ConfigAdmin Management Thread");
                thread.setDaemon(true);
                return thread;
            }
        });
        asyncListnersExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable run) {
                Thread thread = new Thread(run);
                thread.setName("ConfigAdmin Listener Thread");
                thread.setDaemon(true);
                return thread;
            }
        });
        controllerClient = injectedModelController.getValue().createClient(mgmntOperationExecutor);
    }

    @Override
    public void stop(StopContext context) {
        mgmntOperationExecutor.shutdown();
        asyncListnersExecutor.shutdown();
    }

    @Override
    public ConfigAdminInternal getValue() throws IllegalStateException {
        return this;
    }

    @Override
    public Set<String> getConfigurations() {
        synchronized (configurations) {
            Set<String> result = configurations.keySet();
            LOGGER.debugf("Get configurations: %s", result);
            return result;
        }
    }

    @Override
    public boolean hasConfiguration(String pid) {
        synchronized (configurations) {
            return configurations.get(pid) != null;
        }
    }

    @Override
    public Dictionary<String, String> getConfiguration(String pid) {
        synchronized (configurations) {
            Dictionary<String, String> result = configurations.get(pid);
            LOGGER.debugf("Get configuration: %s => %s", pid, result);
            return result;
        }
    }

    @Override
    public Dictionary<String, String> putConfiguration(final String pid, final Dictionary<String, String> dictionary) {
        LOGGER.debugf("Put configuration: %s => %s", pid, dictionary);
        synchronized (configurations) {
            final Dictionary<String, String> previous = configurations.get(pid);
            String skipMgmnt = dictionary.get(TRANSIENT_PROPERTY_SKIP_MANAGEMENT_OPERATION);
            if (Boolean.parseBoolean(skipMgmnt)) {
                LOGGER.debugf("Put skip management operation: %s => %s", pid, dictionary);
                return previous;
            }

            Dictionary<String, String> modifiable = getModifiableDictionary(dictionary);
            configurations.put(pid, modifiable, false);

            ModelNode op;
            ModelNode address = getSubsystemAddress();
            address.add(new ModelNode().set(ModelConstants.CONFIGURATION, pid));
            if (previous != null) {
                op = Util.getEmptyOperation(ModelConstants.UPDATE, address);
            } else {
                op = Util.getEmptyOperation(ModelDescriptionConstants.ADD, address);
            }

            ModelNode entries = new ModelNode();
            Enumeration<String> keys = modifiable.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                String value = modifiable.get(key);
                entries.get(key).set(value);
            }
            op.get(ModelConstants.ENTRIES).set(entries);

            Callable<Object> errorHandler = new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return configurations.put(pid, previous, true);
                }
            };
            new ConfigAdminOperationTask(pid, op, errorHandler).submit();
            return previous;
        }
    }

    @Override
    public void putConfigurationInternal(String pid, Dictionary<String, String> dictionary) {
        LOGGER.debugf("Put configuration internal: %s => %s", pid, dictionary);
        synchronized (configurations) {
            Dictionary<String, String> modifiable = getModifiableDictionary(dictionary);
            if (configurations.put(pid, modifiable, false)) {
                new ConfigAdminListenerTask(pid, modifiable).submit();
            }
        }
    }

    @Override
    public Dictionary<String, String> removeConfiguration(final String pid) {
        LOGGER.debugf("Remove configuration: %s", pid);
        synchronized (configurations) {
            final Dictionary<String, String> previous = configurations.get(pid);
            if (previous != null) {
                ModelNode address = getSubsystemAddress();
                address.add(new ModelNode().set(ModelConstants.CONFIGURATION, pid));
                ModelNode op = Util.getEmptyOperation(ModelDescriptionConstants.REMOVE, address);

                configurations.remove(pid);
                Callable<Object> errorHandler = new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        return configurations.put(pid, previous, true);
                    }
                };
                new ConfigAdminOperationTask(pid, op, errorHandler).submit();
            }
            return previous;
        }
    }

    @Override
    public void removeConfigurationInternal(String pid) {
        LOGGER.debugf("Remove configuration internal: %s", pid);
        synchronized (configurations) {
            if (configurations.remove(pid)) {
                new ConfigAdminListenerTask(pid, null).submit();
            }
        }
    }

    @Override
    public void addListener(ConfigAdminListener listener) {
        LOGGER.debugf("Add listener: %s", listener);
        synchronized (configurations) {
            listeners.add(listener);

            // Call the newly registered listener with a potentially null dictionary for every registered pid
            Set<String> pids = listener.getPIDs();
            if (pids != null) {
                for (String pid : pids) {
                    Dictionary<String, String> props = configurations.get(pid);
                    listener.configurationModified(pid, props);
                }
            } else {
                Set<String> configpids = configurations.keySet();
                for (String pid : configpids) {
                    Dictionary<String, String> props = configurations.get(pid);
                    listener.configurationModified(pid, props);
                }
            }
        }
    }

    @Override
    public void removeListener(ConfigAdminListener listener) {
        LOGGER.debugf("Remove listener: %s", listener);
        listeners.remove(listener);
    }

    private ModelNode getSubsystemAddress() {
        ModelNode address = new ModelNode();
        return address.add(new ModelNode().set(ModelDescriptionConstants.SUBSYSTEM, ConfigAdminExtension.SUBSYSTEM_NAME));
    }

    private Dictionary<String, String> getModifiableDictionary(Dictionary<String, String> source) {
        Dictionary<String, String> result = new Hashtable<String, String>();
        if (source != null) {
            Enumeration<String> keys = source.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                result.put(key, source.get(key));
            }
        }
        return result;
    }

    class ConfigAdminOperationTask implements Runnable {

        private final String pid;
        private final ModelNode operation;
        private final Callable<Object> errorHandler;

        private ConfigAdminOperationTask(String pid, ModelNode operation, Callable<Object> errorHandler) {
            this.errorHandler = errorHandler;
            this.operation = operation;
            this.pid = pid;
        }

        private void submit() {
            mgmntOperationExecutor.submit(this);
        }

        @Override
        public void run() {
            boolean success = true;
            try {
                ModelNode node = controllerClient.execute(operation);
                ModelNode outcome = node.get(ModelDescriptionConstants.OUTCOME);
                success = ModelDescriptionConstants.SUCCESS.equals(outcome.asString());
                if (!success) {
                    ModelNode failure = node.get(ModelDescriptionConstants.FAILURE_DESCRIPTION);
                    LOGGER.cannotUpdateConfiguration(new IllegalStateException(failure.asString()), pid);
                }
            } catch (Throwable ex) {
                LOGGER.cannotUpdateConfiguration(ex, pid);
            }
            if (!success) {
                try {
                    errorHandler.call();
                } catch (Exception ex) {
                    LOGGER.cannotRestoreConfiguration(ex, pid);
                }
            }
        }
    }

    class ConfigAdminListenerTask implements Runnable {

        private final String pid;
        private final Dictionary<String, String> dictionary;
        private final Set<ConfigAdminListener> snapshot;

        private ConfigAdminListenerTask(String pid, Dictionary<String, String> dictionary) {
            this(pid, dictionary, new HashSet<ConfigAdminListener>(listeners));
        }

        private ConfigAdminListenerTask(String pid, Dictionary<String, String> dictionary, Set<ConfigAdminListener> snapshot) {
            this.pid = pid;
            this.snapshot = snapshot;
            if (dictionary != null) {
                dictionary.put(TRANSIENT_PROPERTY_SKIP_MANAGEMENT_OPERATION, "true");
                this.dictionary = new UnmodifiableDictionary<String, String>(dictionary);
            } else {
                this.dictionary = null;
            }
        }

        private void submit() {
            asyncListnersExecutor.submit(this);
        }

        @Override
        public void run() {
            for (ConfigAdminListener aux : snapshot) {
                Set<String> pids = aux.getPIDs();
                if (pids == null || pids.contains(pid)) {
                    try {
                        aux.configurationModified(pid, dictionary);
                    } catch (Exception ex) {
                        LOGGER.configurationListenerError(ex, aux);
                    }
                }
            }
        }
    }
}
