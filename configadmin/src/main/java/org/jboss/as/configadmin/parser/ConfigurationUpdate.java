/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.configadmin.service.ConfigAdminServiceImpl;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Process a Configuration Update.
 *
 * @author David Bosschaert
 */
public class ConfigurationUpdate implements OperationStepHandler {
    static final ConfigurationUpdate INSTANCE = new ConfigurationUpdate();

    private ConfigurationUpdate() {
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        // Remove the resource from the model
        context.removeResource(PathAddress.EMPTY_ADDRESS);
        // Add the new resource with the updated information
        Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
        resource.getModel().get(ModelConstants.ENTRIES).set(operation.get(ModelConstants.ENTRIES));

        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                ModelNode entries = operation.get(ModelConstants.ENTRIES);
                String pid = operation.get(ModelDescriptionConstants.OP_ADDR).asObject().get(ModelConstants.CONFIGURATION).asString();

                Dictionary<String, String> dictionary = new Hashtable<String, String>();
                for (String key : entries.keys()) {
                    dictionary.put(key, entries.get(key).asString());
                }

                ConfigAdminServiceImpl configAdmin = ConfigAdminExtension.getConfigAdminService(context);
                if (configAdmin != null) {
                    configAdmin.putConfigurationFromDMR(pid, dictionary);
                }

                context.completeStep();
            }
        }, OperationContext.Stage.RUNTIME);

        context.completeStep();
    }

    static DescriptionProvider DESCRIPTION = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ModelNode node = new ModelNode();
            ResourceBundle resbundle = ConfigAdminProviders.getResourceBundle(locale);
            node.get(ModelDescriptionConstants.OPERATION_NAME).set(ModelConstants.UPDATE);
            node.get(ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("configuration.update"));
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
