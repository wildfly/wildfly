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

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess.AccessType;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;

/**
 * @author Thomas.Diesler@jboss.com
 */
class ConfigAdminProviders {

    static final String RESOURCE_NAME = ConfigAdminProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {

            ModelNode subsystem = new ModelNode();
            ResourceBundle resbundle = getResourceBundle(locale);
            subsystem.get(DESCRIPTION).set(resbundle.getString("subsystem"));
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(Namespace.CURRENT.getUriString());

            subsystem.get(CHILDREN, ModelConstants.CONFIGURATION, ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("configuration"));

            return subsystem;
        }
    };

    static final DescriptionProvider SUBSYSTEM_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set(REMOVE);
            op.get(DESCRIPTION).set(bundle.getString("subsystem.remove"));
            op.get(REPLY_PROPERTIES).setEmptyObject();
            op.get(REQUEST_PROPERTIES).setEmptyObject();
            return op;
        }
    };

    static final DescriptionProvider CONFIGURATION_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            final ModelNode node = new ModelNode();
            ResourceBundle resbundle = getResourceBundle(locale);
            node.get(DESCRIPTION).set(resbundle.getString("configuration"));
            node.get(ATTRIBUTES, ModelConstants.ENTRIES, ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("configuration.entries"));
            node.get(ATTRIBUTES, ModelConstants.ENTRIES, ModelDescriptionConstants.REQUIRED).set(true);
            node.get(ATTRIBUTES, ModelConstants.ENTRIES, ModelDescriptionConstants.TYPE).set(ModelType.LIST);
            node.get(ATTRIBUTES, ModelConstants.ENTRIES, ModelDescriptionConstants.VALUE_TYPE).set(ModelType.PROPERTY);
            node.get(ATTRIBUTES, ModelConstants.ENTRIES, ModelDescriptionConstants.ACCESS_TYPE).set(AccessType.READ_WRITE.toString());
            node.get(ATTRIBUTES, ModelConstants.ENTRIES, ModelDescriptionConstants.RESTART_REQUIRED).set(Flag.RESTART_NONE.toString());
            return node;
        }
    };

    static ResourceBundle getResourceBundle(Locale locale) {
        return ResourceBundle.getBundle(RESOURCE_NAME, locale != null ? locale : Locale.getDefault());
    }
}
