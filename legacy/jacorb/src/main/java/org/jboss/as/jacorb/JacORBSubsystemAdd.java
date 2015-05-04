/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.jacorb.logging.JacORBLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.wildfly.iiop.openjdk.Constants;
import org.wildfly.iiop.openjdk.IIOPSubsystemAdd;

import com.sun.corba.se.impl.orbutil.ORBConstants;

/**
 *
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */

public class JacORBSubsystemAdd extends IIOPSubsystemAdd {

    static final JacORBSubsystemAdd INSTANCE = new JacORBSubsystemAdd();

    private JacORBSubsystemAdd() {
        super(JacORBSubsystemDefinitions.SUBSYSTEM_ATTRIBUTES);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {
        printJacORBEmulationWarningMessage();
        super.performRuntime(context, operation, model);
    }

    private void printJacORBEmulationWarningMessage() {
        JacORBLogger.ROOT_LOGGER.jacorbEmulationWarning();
    }

    @Override
    protected void populateModel(final OperationContext context, final ModelNode operation, final Resource resource)
            throws OperationFailedException {
        super.populateModel(context, operation, resource);
        final ModelNode model = resource.getModel();
        if (!context.getProcessType().equals(ProcessType.HOST_CONTROLLER)) {
            final List<String> propertiesToReject = new LinkedList<String>();
            for (final AttributeDefinition attribute : JacORBSubsystemDefinitions.ON_OFF_ATTRIBUTES_TO_REJECT) {
                if (model.hasDefined(attribute.getName())
                        && model.get(attribute.getName()).equals(JacORBSubsystemDefinitions.DEFAULT_ENABLED_PROPERTY)) {
                    propertiesToReject.add(attribute.getName());
                }
            }
            for (final AttributeDefinition attribute : JacORBSubsystemDefinitions.ATTRIBUTES_TO_REJECT) {
                if (model.hasDefined(attribute.getName())) {
                    propertiesToReject.add(attribute.getName());
                }
            }
            if (!propertiesToReject.isEmpty()) {
                throw JacORBLogger.ROOT_LOGGER.cannotEmulateProperties(propertiesToReject);
            }
        }
    }

    @Override
    protected Properties getConfigurationProperties(OperationContext context, ModelNode model) throws OperationFailedException {
        Properties props = new Properties();

        // get the configuration properties from the attribute definitions.
        for (AttributeDefinition attrDefinition : JacORBSubsystemDefinitions.SUBSYSTEM_ATTRIBUTES) {
            if (JacORBSubsystemDefinitions.ON_OFF_ATTRIBUTES_TO_REJECT.contains(attrDefinition)
                    || JacORBSubsystemDefinitions.ATTRIBUTES_TO_REJECT.contains(attrDefinition)) {
                continue;
            }
            ModelNode resolvedModelAttribute = attrDefinition.resolveModelAttribute(context, model);
            if (resolvedModelAttribute.isDefined()) {
                String name = attrDefinition.getName();
                String value = resolvedModelAttribute.asString();
                transformAndSetProperty(props, name, value);
            }
        }

        // check if the node contains a list of generic properties.
        if (model.hasDefined(JacORBSubsystemConstants.PROPERTIES)) {
            ModelNode propertiesNode = model.get(JacORBSubsystemConstants.PROPERTIES);

            for (Property property : propertiesNode.asPropertyList()) {
                String name = property.getName();
                ModelNode value = property.getValue();
                props.setProperty(name, value.asString());
            }
        }
        return props;
    }

    private void transformAndSetProperty(final Properties props, final String name, final String value) {
        switch (name) {
            case Constants.ORB_PERSISTENT_SERVER_ID:
                props.setProperty(ORBConstants.ORB_SERVER_ID_PROPERTY, value);
                break;
            case JacORBSubsystemConstants.ORB_GIOP_MINOR_VERSION:
                props.setProperty(ORBConstants.GIOP_VERSION, new StringBuilder().append("1.").append(value).toString());
                break;
            case JacORBSubsystemConstants.ORB_INIT_TRANSACTIONS:
                if (value.equals(JacORBSubsystemConstants.ON)) {
                    props.setProperty(name, Constants.FULL);
                } else if (value.equals(JacORBSubsystemConstants.OFF)) {
                    props.setProperty(name, Constants.NONE);
                } else {
                    props.setProperty(name, value);
                }
                break;
            case JacORBSubsystemConstants.SECURITY_SUPPORT_SSL:
                if (value.equals(JacORBSubsystemConstants.ON)) {
                    props.setProperty(name, Boolean.TRUE.toString());
                } else {
                    props.setProperty(name, Boolean.FALSE.toString());
                }
                break;
            case JacORBSubsystemConstants.SECURITY_ADD_COMP_VIA_INTERCEPTOR:
                if (value.equals(JacORBSubsystemConstants.ON)) {
                    props.setProperty(name, Boolean.TRUE.toString());
                } else {
                    props.setProperty(name, Boolean.FALSE.toString());
                }
                break;
            case JacORBSubsystemConstants.NAMING_EXPORT_CORBALOC:
                if (value.equals(JacORBSubsystemConstants.ON)) {
                    props.setProperty(name, Boolean.TRUE.toString());
                } else {
                    props.setProperty(name, Boolean.FALSE.toString());
                }
                break;
            default:
                props.setProperty(name, value);
        }
    }
}
