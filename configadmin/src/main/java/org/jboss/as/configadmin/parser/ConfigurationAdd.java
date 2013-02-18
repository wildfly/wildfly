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
package org.jboss.as.configadmin.parser;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.as.configadmin.ConfigAdmin;
import org.jboss.as.configadmin.service.ConfigAdminInternal;
import org.jboss.as.configadmin.service.ConfigAdminServiceImpl;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Thomas.Diesler@jboss.com
 * @author David Bosschaert
 */
public class ConfigurationAdd extends AbstractAddStepHandler {
    static final ConfigurationAdd INSTANCE = new ConfigurationAdd();

    private InitializeConfigAdminService initializationService;

    private ConfigurationAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        ConfigurationResource.ENTRIES.validateAndSet(operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler,
            List<ServiceController<?>> newControllers) throws OperationFailedException {

        String pid = operation.get(ModelDescriptionConstants.OP_ADDR).asObject().get(ModelConstants.CONFIGURATION).asString();
        Dictionary<String, String> dictionary = new Hashtable<String, String>(ConfigurationResource.ENTRIES.unwrap(context, model));

        ConfigAdminInternal configAdmin = ConfigAdminExtension.getConfigAdminService(context);
        if (configAdmin != null) {
            configAdmin.putConfigurationInternal(pid, dictionary);
        } else {
            synchronized (this) {
                if (initializationService == null) {
                    initializationService = new InitializeConfigAdminService();
                    ServiceBuilder<Object> builder = context.getServiceTarget().addService(ServiceName.JBOSS.append("configadmin", "data_initialization"), initializationService);
                    builder.addDependency(ConfigAdmin.SERVICE_NAME, ConfigAdmin.class, initializationService.injectedConfigAdminService);
                    builder.install();
                }
            }
            initializationService.putConfiguration(pid, dictionary);
        }
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, ModelNode model, List<ServiceController<?>> controllers) {
        String pid = operation.get(ModelDescriptionConstants.OP_ADDR).asObject().get(ModelConstants.CONFIGURATION).asString();

        ConfigAdminInternal configAdmin = ConfigAdminExtension.getConfigAdminService(context);
        if (configAdmin != null) {
            configAdmin.removeConfigurationInternal(pid);
        }
    }

    static class InitializeConfigAdminService implements Service<Object> {
        private final Map<String, Dictionary<String, String>> configs = new ConcurrentHashMap<String, Dictionary<String,String>>();
        private final InjectedValue<ConfigAdmin> injectedConfigAdminService = new InjectedValue<ConfigAdmin>();

        @Override
        public Object getValue() throws IllegalStateException, IllegalArgumentException {
            return null;
        }

        public void putConfiguration(String pid, Dictionary<String, String> dictionary) {
            configs.put(pid, dictionary);
        }

        @Override
        public void start(StartContext context) throws StartException {
            for (Map.Entry<String, Dictionary<String, String>> entry : configs.entrySet()) {
                ConfigAdminServiceImpl configAdminService = (ConfigAdminServiceImpl) injectedConfigAdminService.getValue();
                configAdminService.putConfigurationInternal(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public void stop(StopContext context) {
        }
    }
}
