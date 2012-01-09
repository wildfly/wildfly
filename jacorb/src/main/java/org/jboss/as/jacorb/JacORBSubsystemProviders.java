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

package org.jboss.as.jacorb;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;


/**
 * <p>
 * Collection of description providers for the JacORB subsystem.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@Deprecated
public class JacORBSubsystemProviders {

    static final String RESOURCE_NAME = JacORBSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getSubsystem(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getSubsystemAdd(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return Descriptions.getSubsystemRemove(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_DESCRIBE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }
    };

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    private static class Descriptions {

        static ModelNode getSubsystem(Locale locale) {

            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode subsystem = new ModelNode();

            subsystem.get(DESCRIPTION).set(bundle.getString("jacorb"));
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(JacORBSubsystemParser.Namespace.CURRENT.getUriString());

            // add the orb attribute descriptions.
            for (SimpleAttributeDefinition attrDefinition : JacORBSubsystemDefinitions.ORB_ATTRIBUTES)
                attrDefinition.addResourceAttributeDescription(bundle, "orb", subsystem);
            // add the orb connection attribute descriptions.
            for (SimpleAttributeDefinition attrDefinition : JacORBSubsystemDefinitions.ORB_CONN_ATTRIBUTES)
                attrDefinition.addResourceAttributeDescription(bundle, "orb.conn", subsystem);
            // add the orb initializers attribute descriptions.
            for (SimpleAttributeDefinition attrDefinition : JacORBSubsystemDefinitions.ORB_INIT_ATTRIBUTES)
                attrDefinition.addResourceAttributeDescription(bundle, "orb.init", subsystem);
            // add the poa attribute descriptions.
            for (SimpleAttributeDefinition attrDefinition : JacORBSubsystemDefinitions.POA_ATTRIBUTES)
                attrDefinition.addResourceAttributeDescription(bundle, "poa", subsystem);
            // add the poa request processor attribute descriptions.
            for (SimpleAttributeDefinition attrDefinition : JacORBSubsystemDefinitions.POA_RP_ATTRIBUTES)
                attrDefinition.addResourceAttributeDescription(bundle, "poa.request-processors", subsystem);
            // add the naming attribute descriptions.
            for (SimpleAttributeDefinition attrDefinition : JacORBSubsystemDefinitions.NAMING_ATTRIBUTES)
                attrDefinition.addResourceAttributeDescription(bundle, "naming", subsystem);
            // add the interoperability attribute descriptions.
            for (SimpleAttributeDefinition attrDefinition : JacORBSubsystemDefinitions.INTEROP_ATTRIBUTES)
                attrDefinition.addResourceAttributeDescription(bundle, "interop", subsystem);
            // add the security attribute descriptions.
            for (SimpleAttributeDefinition attrDefinition : JacORBSubsystemDefinitions.SECURITY_ATTRIBUTES)
                attrDefinition.addResourceAttributeDescription(bundle, "security", subsystem);

            // add the generic properties descriptions.
            addPropertiesDescriptions(subsystem.get(ATTRIBUTES, JacORBSubsystemConstants.PROPERTIES), locale);

            return subsystem;
        }

        static ModelNode getSubsystemAdd(Locale locale) {

            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode op = new ModelNode();

            op.get(OPERATION_NAME).set(ADD);
            op.get(DESCRIPTION).set(bundle.getString("jacorb.add"));

            // add the orb parameter descriptions.
            for (SimpleAttributeDefinition attrDefinition : JacORBSubsystemDefinitions.ORB_ATTRIBUTES)
                attrDefinition.addOperationParameterDescription(bundle, "orb", op);
            // add the orb connection parameter descriptions.
            for (SimpleAttributeDefinition attrDefinition : JacORBSubsystemDefinitions.ORB_CONN_ATTRIBUTES)
                attrDefinition.addOperationParameterDescription(bundle, "orb.conn", op);
            // add the orb initializers parameter descriptions.
            for (SimpleAttributeDefinition attrDefinition : JacORBSubsystemDefinitions.ORB_INIT_ATTRIBUTES)
                attrDefinition.addOperationParameterDescription(bundle, "orb.init", op);
            // add the poa parameter descriptions.
            for (SimpleAttributeDefinition attrDefinition : JacORBSubsystemDefinitions.POA_ATTRIBUTES)
                attrDefinition.addOperationParameterDescription(bundle, "poa", op);
            // add the poa request processor parameter descriptions.
            for (SimpleAttributeDefinition attrDefinition : JacORBSubsystemDefinitions.POA_RP_ATTRIBUTES)
                attrDefinition.addOperationParameterDescription(bundle, "poa.request-processors", op);
            // add the naming parameter descriptions.
            for (SimpleAttributeDefinition attrDefinition : JacORBSubsystemDefinitions.NAMING_ATTRIBUTES)
                attrDefinition.addOperationParameterDescription(bundle, "naming", op);
            // add the interoperability parameter descriptions.
            for (SimpleAttributeDefinition attrDefinition : JacORBSubsystemDefinitions.INTEROP_ATTRIBUTES)
                attrDefinition.addOperationParameterDescription(bundle, "interop", op);
            // add the security parameter descriptions.
            for (SimpleAttributeDefinition attrDefinition : JacORBSubsystemDefinitions.SECURITY_ATTRIBUTES)
                attrDefinition.addOperationParameterDescription(bundle, "security", op);

            // add the generic properties descriptions.
            addPropertiesDescriptions(op.get(REQUEST_PROPERTIES, JacORBSubsystemConstants.PROPERTIES), locale);

            op.get(REPLY_PROPERTIES).setEmptyObject();

            return op;
        }

        static ModelNode getSubsystemRemove(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode op = new ModelNode();

            op.get(OPERATION_NAME).set(REMOVE);
            op.get(DESCRIPTION).set(bundle.getString("jacorb.remove"));
            op.get(REQUEST_PROPERTIES).setEmptyObject();
            op.get(REPLY_PROPERTIES).setEmptyObject();

            return op;
        }

        static void addPropertiesDescriptions(ModelNode node, Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            node.get(DESCRIPTION).set(bundle.getString("properties"));
            node.get(TYPE).set(ModelType.LIST);
            node.get(REQUIRED).set(false);
            node.get(VALUE_TYPE, JacORBSubsystemConstants.NAME, DESCRIPTION).set(bundle.getString("properties.name"));
            node.get(VALUE_TYPE, JacORBSubsystemConstants.NAME, TYPE).set(ModelType.STRING);
            node.get(VALUE_TYPE, JacORBSubsystemConstants.PROPERTY_VALUE, DESCRIPTION).set(bundle.getString("properties.value"));
            node.get(VALUE_TYPE, JacORBSubsystemConstants.PROPERTY_VALUE, TYPE).set(ModelType.STRING);
        }
    }
}
