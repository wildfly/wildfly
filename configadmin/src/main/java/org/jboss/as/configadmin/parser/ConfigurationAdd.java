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

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author Thomas.Diesler@jboss.com
 */
public class ConfigurationAdd extends AbstractAddStepHandler {
    static final ConfigurationAdd INSTANCE = new ConfigurationAdd();

    private ConfigurationAdd() {
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return context.isNormalServer() || context.getProcessType() == ProcessType.HOST_CONTROLLER;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        model.get(ModelConstants.ENTRIES).set(operation.get(ModelConstants.ENTRIES));
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler,
            List<ServiceController<?>> newControllers) throws OperationFailedException {

        ModelNode entries = operation.get(ModelConstants.ENTRIES);
        String pid = operation.get(ModelDescriptionConstants.OP_ADDR).asObject().get(ModelConstants.CONFIGURATION).asString();
        Dictionary<String, String> dictionary = new Hashtable<String, String>();
        for (String key : entries.keys()) {
            dictionary.put(key, entries.get(key).asString());
        }

        ConfigAdminState subsystemState = ConfigAdminState.getSubsystemState(context);
        if (subsystemState != null) {
            subsystemState.putConfiguration(pid, dictionary);
        }
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, ModelNode model, List<ServiceController<?>> controllers) {
        String pid = operation.get(ModelDescriptionConstants.OP_ADDR).asObject().get(ModelConstants.CONFIGURATION).asString();
        ConfigAdminState subsystemState = ConfigAdminState.getSubsystemState(context);
        if (subsystemState != null) {
            subsystemState.removeConfiguration(pid);
        }
    }

    static DescriptionProvider DESCRIPTION = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(Locale locale) {
            ModelNode node = new ModelNode();
            ResourceBundle resbundle = ConfigAdminProviders.getResourceBundle(locale);
            node.get(ModelDescriptionConstants.OPERATION_NAME).set(ModelDescriptionConstants.ADD);
            node.get(ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("configuration.add"));
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.ENTRIES, ModelDescriptionConstants.DESCRIPTION).set(
                    resbundle.getString("configuration.entries"));
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.ENTRIES, ModelDescriptionConstants.TYPE).set(ModelType.LIST);
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.ENTRIES, ModelDescriptionConstants.REQUIRED).set(true);
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelConstants.ENTRIES, ModelDescriptionConstants.VALUE_TYPE).set(ModelType.PROPERTY);
            node.get(ModelDescriptionConstants.REPLY_PROPERTIES).setEmptyObject();
            return node;
        }
    };
}