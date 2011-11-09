/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jaxr.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.jaxr.extension.JAXRConstants.Namespace;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Contains the description providers. The description providers are what print out the
 * information when you execute the {@code read-resource-description} operation.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 26-Oct-2011
 */
class JAXRSubsystemProviders {

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            final ModelNode subsystem = new ModelNode();
            subsystem.get(DESCRIPTION).set("The JAXR subsystem");
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(Namespace.CURRENT.getUriString());

            subsystem.get(ATTRIBUTES, ModelConstants.CONNECTION, ModelDescriptionConstants.DESCRIPTION).set("The JNDI name for the ConnectionFactory");
            subsystem.get(ATTRIBUTES, ModelConstants.CONNECTION, ModelDescriptionConstants.REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, ModelConstants.CONNECTION, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, ModelConstants.CONNECTION, ModelDescriptionConstants.ACCESS_TYPE).set(AttributeAccess.AccessType.READ_WRITE.toString());
            subsystem.get(ATTRIBUTES, ModelConstants.CONNECTION, ModelDescriptionConstants.RESTART_REQUIRED).set(AttributeAccess.Flag.RESTART_ALL_SERVICES.toString());

            subsystem.get(ATTRIBUTES, ModelConstants.DATASOURCE, ModelDescriptionConstants.DESCRIPTION).set("The JNDI name for the DataSource");
            subsystem.get(ATTRIBUTES, ModelConstants.DATASOURCE, ModelDescriptionConstants.REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, ModelConstants.DATASOURCE, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, ModelConstants.DATASOURCE, ModelDescriptionConstants.ACCESS_TYPE).set(AttributeAccess.AccessType.READ_WRITE.toString());
            subsystem.get(ATTRIBUTES, ModelConstants.DATASOURCE, ModelDescriptionConstants.RESTART_REQUIRED).set(AttributeAccess.Flag.RESTART_ALL_SERVICES.toString());

            subsystem.get(ATTRIBUTES, ModelConstants.DROPONSTART, ModelDescriptionConstants.DESCRIPTION).set("Should tables be dropped on Start");
            subsystem.get(ATTRIBUTES, ModelConstants.DROPONSTART, ModelDescriptionConstants.REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, ModelConstants.DROPONSTART, ModelDescriptionConstants.TYPE).set(ModelType.BOOLEAN);
            subsystem.get(ATTRIBUTES, ModelConstants.DROPONSTART, ModelDescriptionConstants.ACCESS_TYPE).set(AttributeAccess.AccessType.READ_WRITE.toString());
            subsystem.get(ATTRIBUTES, ModelConstants.DROPONSTART, ModelDescriptionConstants.RESTART_REQUIRED).set(AttributeAccess.Flag.RESTART_ALL_SERVICES.toString());

            subsystem.get(ATTRIBUTES, ModelConstants.CREATEONSTART, ModelDescriptionConstants.DESCRIPTION).set("Should tables be created on Start");
            subsystem.get(ATTRIBUTES, ModelConstants.CREATEONSTART, ModelDescriptionConstants.REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, ModelConstants.CREATEONSTART, ModelDescriptionConstants.TYPE).set(ModelType.BOOLEAN);
            subsystem.get(ATTRIBUTES, ModelConstants.CREATEONSTART, ModelDescriptionConstants.ACCESS_TYPE).set(AttributeAccess.AccessType.READ_WRITE.toString());
            subsystem.get(ATTRIBUTES, ModelConstants.CREATEONSTART, ModelDescriptionConstants.RESTART_REQUIRED).set(AttributeAccess.Flag.RESTART_ALL_SERVICES.toString());

            subsystem.get(ATTRIBUTES, ModelConstants.DROPONSTOP, ModelDescriptionConstants.DESCRIPTION).set("Should tables be dropped on Stop");
            subsystem.get(ATTRIBUTES, ModelConstants.DROPONSTOP, ModelDescriptionConstants.REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, ModelConstants.DROPONSTOP, ModelDescriptionConstants.TYPE).set(ModelType.BOOLEAN);
            subsystem.get(ATTRIBUTES, ModelConstants.DROPONSTOP, ModelDescriptionConstants.ACCESS_TYPE).set(AttributeAccess.AccessType.READ_WRITE.toString());
            subsystem.get(ATTRIBUTES, ModelConstants.DROPONSTOP, ModelDescriptionConstants.RESTART_REQUIRED).set(AttributeAccess.Flag.RESTART_ALL_SERVICES.toString());

            return subsystem;
        }
    };
}
