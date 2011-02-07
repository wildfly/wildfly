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
package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ADMIN_OBJECTS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ARCHIVE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BACKGROUNDVALIDATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BACKGROUNDVALIDATIONMINUTES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BEANVALIDATIONGROUPS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BOOTSTRAPCONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CLASS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONFIG_PROPERTIES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONNECTIONDEFINITIONS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.IDLETIMEOUTMINUTES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.JNDI_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.MAX_POOL_SIZE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.MIN_POOL_SIZE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.PASSWORD;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.POOLNAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.POOL_PREFILL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTER;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTERS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.TRANSACTIONSUPPORT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USERNAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_FAST_FAIL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.XA_RESOURCE_TIMEOUT;
import static org.jboss.as.connector.subsystems.resourceadapters.NewResourceAdaptersSubsystemProviders.SUBSYSTEM;
import static org.jboss.as.connector.subsystems.resourceadapters.NewResourceAdaptersSubsystemProviders.SUBSYSTEM_ADD_DESC;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.NewExtension;
import org.jboss.as.controller.NewExtensionContext;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.common.CommonAdminObject;
import org.jboss.jca.common.api.metadata.common.CommonConnDef;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapter;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapters;
import org.jboss.jca.common.metadata.resourceadapter.ResourceAdapterParser;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
public class NewResourceAdaptersExtension implements NewExtension {

    private static final Logger log = Logger.getLogger("org.jboss.as.datasources");

    @Override
    public void initialize(final NewExtensionContext context) {
        // Register the remoting subsystem
        final SubsystemRegistration registration = context.registerSubsystem(RESOURCEADAPTER);

        registration.registerXMLElementWriter(NewResourceAdapterSubsystemParser.INSTANCE);

        // Remoting subsystem description and operation handlers
        final ModelNodeRegistration subsystem = registration.registerSubsystemModel(SUBSYSTEM);
        subsystem.registerOperationHandler("add", NewResourceAdaptersSubsystemAdd.INSTANCE, SUBSYSTEM_ADD_DESC, false);

    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), NewResourceAdapterSubsystemParser.INSTANCE);
    }

    static final class NewResourceAdapterSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
            XMLElementWriter<SubsystemMarshallingContext> {

        static final NewResourceAdapterSubsystemParser INSTANCE = new NewResourceAdapterSubsystemParser();

        /** {@inheritDoc} */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
            ModelNode node = context.getModelNode();
            // FIXME write out the details
            writer.writeEndElement();
        }

        @Override
        public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {

            // FIXME this should come from somewhere
            final ModelNode address = new ModelNode();
            address.add(ModelDescriptionConstants.SUBSYSTEM, RESOURCEADAPTER);
            address.protect();

            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).set(address);
            list.add(subsystem);

            ResourceAdapters ras = null;
            try {
                String localName = null;
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case RESOURCEADAPTERS_1_0: {
                        localName = reader.getLocalName();
                        final Element element = Element.forName(reader.getLocalName());
                        log.tracef("%s -> %s", localName, element);
                        switch (element) {
                            case SUBSYSTEM: {
                                ResourceAdapterParser parser = new ResourceAdapterParser();
                                ras = parser.parse(reader);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new XMLStreamException(e);
            }

            if (ras != null) {
                ModelNode rasNode = subsystem.get(RESOURCEADAPTERS);
                for (ResourceAdapter ra : ras.getResourceAdapters()) {
                    ModelNode raModel = new ModelNode();
                    for (Entry<String, String> entry : ra.getConfigProperties().entrySet()) {
                        raModel.get(CONFIG_PROPERTIES, entry.getKey()).set(entry.getValue());
                    }
                    raModel.get(ARCHIVE).set(ra.getArchive());
                    raModel.get(TRANSACTIONSUPPORT).set(ra.getTransactionSupport().name());
                    raModel.get(BOOTSTRAPCONTEXT).set(ra.getBootstrapContext());
                    for (String beanValidationGroup : ra.getBeanValidationGroups()) {
                        raModel.get(BEANVALIDATIONGROUPS).add(beanValidationGroup);
                    }

                    for (CommonConnDef conDef : ra.getConnectionDefinitions()) {
                        raModel.get(CONNECTIONDEFINITIONS).add(createConnectionDefinitionModel(conDef));

                    }

                    for (CommonAdminObject adminObject : ra.getAdminObjects()) {
                        raModel.get(ADMIN_OBJECTS).add(createAdminObjectModel(adminObject));

                    }

                    rasNode.add(raModel);
                }
            }

        }

        private ModelNode createAdminObjectModel(CommonAdminObject adminObject) {
            ModelNode adminObjectModel = new ModelNode();
            for (Entry<String, String> entry : adminObject.getConfigProperties().entrySet()) {
                adminObjectModel.get(CONFIG_PROPERTIES, entry.getKey()).set(entry.getValue());
            }
            adminObjectModel.get(CLASS_NAME).set(adminObject.getClassName());
            adminObjectModel.get(JNDI_NAME).set(adminObject.getJndiName());
            adminObjectModel.get(POOLNAME).set(adminObject.getPoolName());
            adminObjectModel.get(ENABLED).set(adminObject.isEnabled());
            adminObjectModel.get(USE_JAVA_CONTEXT).set(adminObject.isUseJavaContext());

            return adminObjectModel;
        }

        private ModelNode createConnectionDefinitionModel(CommonConnDef conDef) {
            ModelNode condefModel = new ModelNode();
            for (Entry<String, String> entry : conDef.getConfigProperties().entrySet()) {
                condefModel.get(CONFIG_PROPERTIES, entry.getKey()).set(entry.getValue());
            }
            condefModel.get(CLASS_NAME).set(conDef.getClassName());
            condefModel.get(JNDI_NAME).set(conDef.getJndiName());
            condefModel.get(POOLNAME).set(conDef.getPoolName());
            condefModel.get(ENABLED).set(conDef.isEnabled());
            condefModel.get(USE_JAVA_CONTEXT).set(conDef.isUseJavaContext());

            if (conDef.getPool() != null) {
                condefModel.get(MAX_POOL_SIZE).set(conDef.getPool().getMaxPoolSize());
                condefModel.get(MIN_POOL_SIZE).set(conDef.getPool().getMinPoolSize());
                condefModel.get(POOL_PREFILL).set(conDef.getPool().isPrefill());
                condefModel.get(POOL_USE_STRICT_MIN).set(conDef.getPool().isUseStrictMin());
            }

            if (conDef.getTimeOut() != null) {
                condefModel.get(ALLOCATION_RETRY).set(conDef.getTimeOut().getAllocationRetry());
                condefModel.get(ALLOCATION_RETRY_WAIT_MILLIS).set(conDef.getTimeOut().getAllocationRetryWaitMillis());
                condefModel.get(BLOCKING_TIMEOUT_WAIT_MILLIS).set(conDef.getTimeOut().getBlockingTimeoutMillis());
                condefModel.get(IDLETIMEOUTMINUTES).set(conDef.getTimeOut().getIdleTimeoutMinutes());
                condefModel.get(XA_RESOURCE_TIMEOUT).set(conDef.getTimeOut().getXaResourceTimeout());
            }

            if (conDef.getSecurity() != null) {
                condefModel.get(USERNAME).set(conDef.getSecurity().getUserName());
                condefModel.get(PASSWORD).set(conDef.getSecurity().getPassword());
            }

            if (conDef.getValidation() != null) {
                condefModel.get(BACKGROUNDVALIDATIONMINUTES).set(conDef.getValidation().getBackgroundValidationMinutes());
                condefModel.get(BACKGROUNDVALIDATION).set(conDef.getValidation().isBackgroundValidation());
                condefModel.get(USE_FAST_FAIL).set(conDef.getValidation().isUseFastFail());
            }

            return condefModel;
        }
    }
}
