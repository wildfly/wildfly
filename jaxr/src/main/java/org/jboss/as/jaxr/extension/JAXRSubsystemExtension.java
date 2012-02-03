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

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.AttributeAccess.AccessType;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.jaxr.JAXRConfiguration;
import org.jboss.as.jaxr.JAXRConstants;
import org.jboss.as.jaxr.JAXRConstants.Namespace;
import org.jboss.as.jaxr.ModelConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;


/**
 * @author Thomas.Diesler@jboss.com
 * @author Kurt Stam
 * @since 26-Oct-2011
 */
public class JAXRSubsystemExtension implements Extension {

    private final JAXRSubsystemParser parser = new JAXRSubsystemParser();
    private final JAXRConfiguration config = new JAXRConfiguration();

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(JAXRConstants.SUBSYSTEM_NAME, Namespace.CURRENT.getUriString(), parser);
    }

    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration subsystem = context.registerSubsystem(JAXRConstants.SUBSYSTEM_NAME, 1, 0);
        ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new JAXRSubsystemRootResource(config));
        registration.registerOperationHandler(DESCRIBE, SubsystemDescribeHandler.INSTANCE, SubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        // JAXR Properties
        ManagementResourceRegistration properties = registration.registerSubModel(PathElement.pathElement(ModelConstants.PROPERTY), PROPERTY_DESCRIPTION);
        properties.registerOperationHandler(ModelDescriptionConstants.ADD, new JAXRPropertyAdd(config), JAXRPropertyAdd.DESCRIPTION, false);
        properties.registerOperationHandler(ModelDescriptionConstants.REMOVE, JAXRPropertyRemove.INSTANCE, JAXRPropertyRemove.DESCRIPTION, false);
        properties.registerReadWriteAttribute(ModelConstants.VALUE, null, new JAXRPropertyWrite(config), Storage.CONFIGURATION);

        subsystem.registerXMLElementWriter(JAXRSubsystemWriter.INSTANCE);
    }

    /**
     * Recreate the steps to put the subsystem in the same state it was in.
     * This is used in domain mode to query the profile being used, in order to
     * get the steps needed to create the servers
     */
    private static class SubsystemDescribeHandler extends GenericSubsystemDescribeHandler {
        static final SubsystemDescribeHandler INSTANCE = new SubsystemDescribeHandler();

        // Hide ctor
        private SubsystemDescribeHandler() {
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            super.execute(context, operation);
            context.completeStep();
        }
    }

    static final DescriptionProvider PROPERTY_DESCRIPTION = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            final ModelNode node = new ModelNode();
            ResourceBundle resbundle = JAXRConfiguration.getResourceBundle(locale);
            node.get(DESCRIPTION).set(resbundle.getString("jaxr.property"));
            node.get(ATTRIBUTES, ModelConstants.VALUE, ModelDescriptionConstants.DESCRIPTION).set(resbundle.getString("jaxr.property.value"));
            node.get(ATTRIBUTES, ModelConstants.VALUE, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, ModelConstants.VALUE, ModelDescriptionConstants.REQUIRED).set(true);
            node.get(ATTRIBUTES, ModelConstants.VALUE, ModelDescriptionConstants.ACCESS_TYPE).set(AccessType.READ_WRITE.toString());
            //node.get(ATTRIBUTES, ModelConstants.VALUE, ModelDescriptionConstants.RESTART_REQUIRED).set("all-services");
            return node;
        }
    };

}
