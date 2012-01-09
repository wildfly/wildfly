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

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author Thomas.Diesler@jboss.com
 */
public class ConfigurationRemove extends AbstractRemoveStepHandler {
    static final ConfigurationRemove INSTANCE = new ConfigurationRemove();

    private ConfigurationRemove() {
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        String pid = operation.get(ModelDescriptionConstants.OP_ADDR).asObject().get(ModelConstants.CONFIGURATION).asString();
        ConfigAdminState subsystemState = ConfigAdminState.getSubsystemState(context);
        if (subsystemState != null && context.completeStep() == OperationContext.ResultAction.KEEP) {
            subsystemState.removeConfiguration(pid);
        }
    }

    static DescriptionProvider DESCRIPTION = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(Locale locale) {
            ModelNode node = new ModelNode();
            ResourceBundle resbundle = ConfigAdminProviders.getResourceBundle(locale);
            node.get(ModelDescriptionConstants.OPERATION_NAME).set(ModelDescriptionConstants.REMOVE);
            node.get(ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("configuration.remove"));
            node.get(ModelDescriptionConstants.REQUEST_PROPERTIES).setEmptyObject();
            node.get(ModelDescriptionConstants.REPLY_PROPERTIES).setEmptyObject();
            return node;
        }
    };
}
