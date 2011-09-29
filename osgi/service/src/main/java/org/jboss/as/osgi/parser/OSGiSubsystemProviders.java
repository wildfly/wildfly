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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.registry.AttributeAccess.AccessType.READ_WRITE;
import static org.jboss.as.controller.registry.AttributeAccess.Flag.RESTART_JVM;
import static org.jboss.as.osgi.parser.SubsystemState.DEFAULT_ACTIVATION;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.osgi.parser.Namespace11.Constants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author David Bosschaert
 */
class OSGiSubsystemProviders {
    static final String RESOURCE_NAME = OSGiSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {
        public ModelNode getModelDescription(final Locale locale) {

            ModelNode subsystem = new ModelNode();
            ResourceBundle resbundle = OSGiSubsystemProviders.getResourceBundle(locale);
            subsystem.get(ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("subsystem"));
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(Namespace.OSGI_1_0.getUriString());

            subsystem.get(ATTRIBUTES, Constants.ACTIVATION, ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("subsystem.activation"));
            subsystem.get(ATTRIBUTES, Constants.ACTIVATION, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, Constants.ACTIVATION, ModelDescriptionConstants.RESTART_REQUIRED).set(RESTART_JVM.toString());
            subsystem.get(ATTRIBUTES, Constants.ACTIVATION, ModelDescriptionConstants.ACCESS_TYPE).set(READ_WRITE.toString());
            subsystem.get(ATTRIBUTES, Constants.ACTIVATION, ModelDescriptionConstants.DEFAULT).set(DEFAULT_ACTIVATION.toString());

            subsystem.get(CHILDREN, Constants.CONFIGURATION, ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("configuration"));
            subsystem.get(CHILDREN, Constants.FRAMEWORK_PROPERTY, ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("framework.property"));
            subsystem.get(CHILDREN, Constants.CAPABILITY, ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("capability"));

            return subsystem;
        }
    };

    static final DescriptionProvider CONFIGURATION_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            final ModelNode node = new ModelNode();
            ResourceBundle resbundle = OSGiSubsystemProviders.getResourceBundle(locale);
            node.get(ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("configuration"));
            node.get(ATTRIBUTES, Constants.ENTRIES, ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("configuration.entries"));
            node.get(ATTRIBUTES, Constants.ENTRIES, ModelDescriptionConstants.REQUIRED).set(true);
            node.get(ATTRIBUTES, Constants.ENTRIES, ModelDescriptionConstants.TYPE).set(ModelType.OBJECT);
            node.get(ATTRIBUTES, Constants.ENTRIES, ModelDescriptionConstants.VALUE_TYPE).set(ModelType.OBJECT);
            return node;
        }
    };

    static final DescriptionProvider FRAMEWORK_PROPERTY_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            final ModelNode node = new ModelNode();
            ResourceBundle resbundle = OSGiSubsystemProviders.getResourceBundle(locale);
            node.get(ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("framework.property"));
            node.get(ModelDescriptionConstants.ATTRIBUTES, Constants.VALUE, ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("framework.property.value"));
            node.get(ModelDescriptionConstants.ATTRIBUTES, Constants.VALUE, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
            node.get(ModelDescriptionConstants.ATTRIBUTES, Constants.VALUE, ModelDescriptionConstants.REQUIRED).set(true);
            return node;
        }
    };


    static final DescriptionProvider CAPABILITY_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            final ModelNode node = new ModelNode();
            ResourceBundle resbundle = OSGiSubsystemProviders.getResourceBundle(locale);
            node.get(ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("capability"));
            node.get(ModelDescriptionConstants.ATTRIBUTES, Constants.STARTLEVEL, ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("capability.startlevel"));
            node.get(ModelDescriptionConstants.ATTRIBUTES, Constants.STARTLEVEL, ModelDescriptionConstants.TYPE).set(ModelType.INT);
            node.get(ModelDescriptionConstants.ATTRIBUTES, Constants.STARTLEVEL, ModelDescriptionConstants.REQUIRED).set(false);
            return node;
        }
    };

    static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
