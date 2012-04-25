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
package org.jboss.as.osgi.parser;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STORAGE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.osgi.parser.SubsystemState.DEFAULT_ACTIVATION;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.AttributeAccess.AccessType;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author David Bosschaert
 * @author Thomas.Diesler@jboss.com
 */
class OSGiSubsystemProviders {
    static final String RESOURCE_NAME = OSGiSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {

            ModelNode subsystem = new ModelNode();
            ResourceBundle resbundle = getResourceBundle(locale);
            subsystem.get(DESCRIPTION).set(resbundle.getString("subsystem"));
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(Namespace.CURRENT.getUriString());

            subsystem.get(ATTRIBUTES, ModelConstants.ACTIVATION, ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("subsystem.activation"));
            subsystem.get(ATTRIBUTES, ModelConstants.ACTIVATION, TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, ModelConstants.ACTIVATION, ModelDescriptionConstants.DEFAULT).set(DEFAULT_ACTIVATION.toString());
            subsystem.get(ATTRIBUTES, ModelConstants.ACTIVATION, ACCESS_TYPE).set(AccessType.READ_WRITE.toString());
            subsystem.get(ATTRIBUTES, ModelConstants.ACTIVATION, ModelDescriptionConstants.RESTART_REQUIRED).set(Flag.RESTART_JVM.toString());

            subsystem.get(ATTRIBUTES, ModelConstants.STARTLEVEL, ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("subsystem.startlevel"));
            subsystem.get(ATTRIBUTES, ModelConstants.STARTLEVEL, TYPE).set(ModelType.INT);
            subsystem.get(ATTRIBUTES, ModelConstants.STARTLEVEL, ACCESS_TYPE).set(AccessType.READ_WRITE.toString());
            subsystem.get(ATTRIBUTES, ModelConstants.STARTLEVEL, ModelDescriptionConstants.RESTART_REQUIRED).set(Flag.RESTART_NONE.toString());
            subsystem.get(ATTRIBUTES, ModelConstants.STARTLEVEL, STORAGE).set(Flag.STORAGE_RUNTIME.toString());

            subsystem.get(CHILDREN, ModelConstants.PROPERTY, ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("framework.property"));
            subsystem.get(CHILDREN, ModelConstants.CAPABILITY, ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("capability"));
            subsystem.get(CHILDREN, ModelConstants.BUNDLE, ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("bundle"));

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

    static final DescriptionProvider PROPERTY_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            final ModelNode node = new ModelNode();
            ResourceBundle resbundle = getResourceBundle(locale);
            node.get(DESCRIPTION).set(resbundle.getString("framework.property"));
            node.get(ATTRIBUTES, ModelConstants.VALUE, ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("framework.property.value"));
            node.get(ATTRIBUTES, ModelConstants.VALUE, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, ModelConstants.VALUE, ModelDescriptionConstants.REQUIRED).set(true);
            node.get(ATTRIBUTES, ModelConstants.VALUE, ACCESS_TYPE).set(AccessType.READ_WRITE.toString());
            node.get(ATTRIBUTES, ModelConstants.VALUE, ModelDescriptionConstants.RESTART_REQUIRED).set("all-services");
            return node;
        }
    };


    static final DescriptionProvider CAPABILITY_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            final ModelNode node = new ModelNode();
            ResourceBundle resbundle = getResourceBundle(locale);
            node.get(DESCRIPTION).set(resbundle.getString("capability"));
            node.get(ATTRIBUTES, ModelConstants.STARTLEVEL, ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("capability.startlevel"));
            node.get(ATTRIBUTES, ModelConstants.STARTLEVEL, TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, ModelConstants.STARTLEVEL, ModelDescriptionConstants.REQUIRED).set(false);
            node.get(ATTRIBUTES, ModelConstants.STARTLEVEL, ACCESS_TYPE).set(AccessType.READ_ONLY.toString());
            node.get(ATTRIBUTES, ModelConstants.STARTLEVEL, ModelDescriptionConstants.RESTART_REQUIRED).set("all-services");
            return node;
        }
    };

    static final DescriptionProvider BUNDLE_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            final ModelNode node = new ModelNode();
            ResourceBundle resbundle = getResourceBundle(locale);
            node.get(DESCRIPTION).set(resbundle.getString("bundle"));

            String storageRuntime = AttributeAccess.Storage.RUNTIME.toString();

            ModelNode idNode = new ModelNode();
            idNode.get(TYPE).set(ModelType.LONG);
            idNode.get(DESCRIPTION).set(resbundle.getString("bundle.id"));
            idNode.get(ACCESS_TYPE).set(ModelDescriptionConstants.READ_ONLY);
            idNode.get(STORAGE).set(storageRuntime);

            ModelNode startLevelNode = new ModelNode();
            startLevelNode.get(TYPE).set(ModelType.INT);
            startLevelNode.get(DESCRIPTION).set(resbundle.getString("bundle.startlevel"));
            startLevelNode.get(ACCESS_TYPE).set(ModelDescriptionConstants.READ_ONLY);
            startLevelNode.get(ModelDescriptionConstants.REQUIRED).set(false);
            startLevelNode.get(STORAGE).set(storageRuntime);

            ModelNode symbolicNameNode = new ModelNode();
            symbolicNameNode.get(TYPE).set(ModelType.STRING);
            symbolicNameNode.get(DESCRIPTION).set(resbundle.getString("bundle.symbolic-name"));
            symbolicNameNode.get(ACCESS_TYPE).set(ModelDescriptionConstants.READ_ONLY);
            symbolicNameNode.get(STORAGE).set(storageRuntime);

            ModelNode versionNode = new ModelNode();
            versionNode.get(TYPE).set(ModelType.STRING);
            versionNode.get(DESCRIPTION).set(resbundle.getString("bundle.version"));
            versionNode.get(ACCESS_TYPE).set(ModelDescriptionConstants.READ_ONLY);
            versionNode.get(STORAGE).set(storageRuntime);

            ModelNode stateNode = new ModelNode();
            stateNode.get(TYPE).set(ModelType.STRING);
            stateNode.get(DESCRIPTION).set(resbundle.getString("bundle.state"));
            stateNode.get(ACCESS_TYPE).set(ModelDescriptionConstants.READ_ONLY);
            stateNode.get(STORAGE).set(storageRuntime);

            ModelNode typeNode = new ModelNode();
            typeNode.get(TYPE).set(ModelType.STRING);
            typeNode.get(DESCRIPTION).set(resbundle.getString("bundle.type"));
            typeNode.get(ACCESS_TYPE).set(ModelDescriptionConstants.READ_ONLY);
            typeNode.get(STORAGE).set(storageRuntime);

            node.get(ATTRIBUTES).get(ModelConstants.ID).set(idNode);
            node.get(ATTRIBUTES).get(ModelConstants.STARTLEVEL).set(startLevelNode);
            node.get(ATTRIBUTES).get(ModelConstants.SYMBOLIC_NAME).set(symbolicNameNode);
            node.get(ATTRIBUTES).get(ModelConstants.VERSION).set(versionNode);
            node.get(ATTRIBUTES).get(ModelConstants.STATE).set(stateNode);
            node.get(ATTRIBUTES).get(ModelConstants.TYPE).set(typeNode);
            return node;
        }
    };

    static ResourceBundle getResourceBundle(Locale locale) {
        return ResourceBundle.getBundle(RESOURCE_NAME, locale != null ? locale : Locale.getDefault());
    }
}
