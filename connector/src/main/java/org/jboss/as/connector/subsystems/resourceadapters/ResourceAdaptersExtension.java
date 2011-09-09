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

import static org.jboss.as.connector.ConnectorLogger.SUBSYSTEM_RA_LOGGER;
import static org.jboss.as.connector.pool.Constants.BACKGROUNDVALIDATION;
import static org.jboss.as.connector.pool.Constants.BACKGROUNDVALIDATIONMILLIS;
import static org.jboss.as.connector.pool.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS;
import static org.jboss.as.connector.pool.Constants.IDLETIMEOUTMINUTES;
import static org.jboss.as.connector.pool.Constants.MAX_POOL_SIZE;
import static org.jboss.as.connector.pool.Constants.MIN_POOL_SIZE;
import static org.jboss.as.connector.pool.Constants.POOL_FLUSH_STRATEGY;
import static org.jboss.as.connector.pool.Constants.POOL_PREFILL;
import static org.jboss.as.connector.pool.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.pool.Constants.USE_FAST_FAIL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ADMIN_OBJECTS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ARCHIVE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BEANVALIDATIONGROUPS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BOOTSTRAPCONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CLASS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONFIG_PROPERTIES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONNECTIONDEFINITIONS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.INTERLEAVING;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.JNDINAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.NOTXSEPARATEPOOL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.NO_RECOVERY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.PAD_XID;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.POOL_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERLUGIN_CLASSNAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERLUGIN_PROPERTIES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_PASSWORD;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_USERNAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTERS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTER_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SAME_RM_OVERRIDE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SECURITY_DOMAIN_AND_APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.TRANSACTIONSUPPORT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_CCM;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WRAP_XA_RESOURCE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.XA_RESOURCE_TIMEOUT;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.ADD_RESOURCEADAPTER_DESC;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.FLUSH_ALL_CONNECTION_DESC;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.FLUSH_IDLE_CONNECTION_DESC;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.REMOVE_RESOURCEADAPTER_DESC;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.RESOURCEADAPTER_DESC;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.SUBSYSTEM;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.SUBSYSTEM_ADD_DESC;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.TEST_CONNECTION_DESC;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.connector.pool.PoolConfigurationRWHandler;
import org.jboss.as.connector.pool.PoolConfigurationRWHandler.PoolConfigurationReadHandler;
import org.jboss.as.connector.pool.PoolConfigurationRWHandler.RaPoolConfigurationWriteHandler;
import org.jboss.as.connector.pool.PoolMetrics;
import org.jboss.as.connector.pool.PoolOperations;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.jca.common.api.metadata.common.CommonConnDef;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapter;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapters;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jeff.zhang@jboss.org">Jeff Zhang</a>
 */
public class ResourceAdaptersExtension implements Extension {

    public static final String SUBSYSTEM_NAME = RESOURCEADAPTERS_NAME;

    @Override
    public void initialize(final ExtensionContext context) {
        SUBSYSTEM_RA_LOGGER.debugf("Initializing ResourceAdapters Extension");
        // Register the remoting subsystem
        final SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME);

        registration.registerXMLElementWriter(ResourceAdapterSubsystemParser.INSTANCE);

        // Remoting subsystem description and operation handlers
        final ManagementResourceRegistration subsystem = registration.registerSubsystemModel(SUBSYSTEM);
        subsystem.registerOperationHandler(ADD, ResourceAdaptersSubSystemAdd.INSTANCE, SUBSYSTEM_ADD_DESC, false);
        subsystem.registerOperationHandler(DESCRIBE, ResourceAdaptersSubsystemDescribeHandler.INSTANCE,
                ResourceAdaptersSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        final ManagementResourceRegistration resourceadapter = subsystem.registerSubModel(PathElement.pathElement(RESOURCEADAPTER_NAME),
                RESOURCEADAPTER_DESC);
        resourceadapter.registerOperationHandler(ADD, RaAdd.INSTANCE, ADD_RESOURCEADAPTER_DESC, false);
        resourceadapter.registerOperationHandler(REMOVE, RaRemove.INSTANCE, REMOVE_RESOURCEADAPTER_DESC, false);

        resourceadapter.registerOperationHandler("flush-idle-connection-in-pool",
                PoolOperations.FlushIdleConnectionInPool.DS_INSTANCE, FLUSH_IDLE_CONNECTION_DESC, false);
        resourceadapter.registerOperationHandler("flush-all-connection-in-pool",
                PoolOperations.FlushAllConnectionInPool.RA_INSTANCE, FLUSH_ALL_CONNECTION_DESC, false);
        resourceadapter.registerOperationHandler("test-connection-in-pool", PoolOperations.TestConnectionInPool.RA_INSTANCE,
                TEST_CONNECTION_DESC, false);

        for (final String attributeName : PoolMetrics.ATTRIBUTES) {
            resourceadapter.registerMetric(attributeName, PoolMetrics.RaPoolMetricsHandler.INSTANCE);

        }

        for (final String attributeName : PoolConfigurationRWHandler.ATTRIBUTES) {
            resourceadapter.registerReadWriteAttribute(attributeName, PoolConfigurationReadHandler.INSTANCE,
                    RaPoolConfigurationWriteHandler.INSTANCE, Storage.CONFIGURATION);
        }

    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), ResourceAdapterSubsystemParser.INSTANCE);
    }

    static final class ResourceAdapterSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
            XMLElementWriter<SubsystemMarshallingContext> {

        static final ResourceAdapterSubsystemParser INSTANCE = new ResourceAdapterSubsystemParser();

        /** {@inheritDoc} */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            ModelNode node = context.getModelNode();
            boolean hasChildren = node.hasDefined(RESOURCEADAPTER_NAME) && node.get(RESOURCEADAPTER_NAME).asPropertyList().size() > 0;

            context.startSubsystemElement(Namespace.CURRENT.getUriString(), !hasChildren);

            if (hasChildren) {
                writer.writeStartElement(Element.RESOURCE_ADAPTERS.getLocalName());
                for (Property property : node.get(RESOURCEADAPTER_NAME).asPropertyList()) {
                    final ModelNode ra = property.getValue();

                    writeRaElement(writer, ra);
                }
                writer.writeEndElement();
                // Close the subsystem element
                writer.writeEndElement();
            }
        }

        private void writeRaElement(XMLExtendedStreamWriter streamWriter, ModelNode ra) throws XMLStreamException {
            streamWriter.writeStartElement(ResourceAdapters.Tag.RESOURCE_ADAPTER.getLocalName());

            ARCHIVE.marshallAsElement(ra, false, streamWriter);

            if (ra.hasDefined(BEANVALIDATIONGROUPS.getName())) {
                for (ModelNode bvg : ra.get(BEANVALIDATIONGROUPS.getName()).asList()) {
                    BEANVALIDATIONGROUPS.marshallAsElement(bvg, false, streamWriter);
                }
            }

            BOOTSTRAPCONTEXT.marshallAsElement(ra, false, streamWriter);
            TRANSACTIONSUPPORT.marshallAsElement(ra, false, streamWriter);
            writeConfigProperties(streamWriter, ra);

            if (ra.hasDefined(CONNECTIONDEFINITIONS_NAME)) {
                streamWriter.writeStartElement(ResourceAdapter.Tag.CONNECTION_DEFINITIONS.getLocalName());
                for (ModelNode conDef : ra.get(CONNECTIONDEFINITIONS_NAME).asList()) {
                    writeConDef(streamWriter, conDef);
                }
                streamWriter.writeEndElement();
            }

            if (ra.hasDefined(ADMIN_OBJECTS_NAME)) {
                streamWriter.writeStartElement(ResourceAdapter.Tag.ADMIN_OBJECTS.getLocalName());
                for (ModelNode adminObject : ra.get(ADMIN_OBJECTS_NAME).asList()) {
                    writeAdminObject(streamWriter, adminObject);
                }
                streamWriter.writeEndElement();
            }
            streamWriter.writeEndElement();

        }

        private void writeConfigProperties(XMLExtendedStreamWriter streamWriter, ModelNode ra) throws XMLStreamException {
            if (ra.hasDefined(CONFIG_PROPERTIES.getName())) {
                for (Property connectionProperty : ra.get(CONFIG_PROPERTIES.getName()).asPropertyList()) {
                    writeProperty(streamWriter, ra, connectionProperty.getName(), connectionProperty
                            .getValue().asString(), ResourceAdapter.Tag.CONFIG_PROPERTY.getLocalName());
                }

            }
        }

        private void writeProperty(XMLExtendedStreamWriter writer, ModelNode node, String name, String value, String localName)
                throws XMLStreamException {

            writer.writeStartElement(localName);
            writer.writeAttribute("name", name);
            writer.writeCharacters(value);
            writer.writeEndElement();

        }


        private void writeAdminObject(XMLExtendedStreamWriter streamWriter, ModelNode adminObject) throws XMLStreamException {
            streamWriter.writeStartElement(ResourceAdapter.Tag.ADMIN_OBJECT.getLocalName());
            CLASS_NAME.marshallAsAttribute(adminObject, false, streamWriter);
            JNDINAME.marshallAsAttribute(adminObject, false, streamWriter);
            ENABLED.marshallAsAttribute(adminObject, false, streamWriter);
            USE_JAVA_CONTEXT.marshallAsAttribute(adminObject, false, streamWriter);
            POOL_NAME.marshallAsAttribute(adminObject, false, streamWriter);

            writeConfigProperties(streamWriter, adminObject);
            streamWriter.writeEndElement();

        }

        private void writeConDef(XMLExtendedStreamWriter streamWriter, ModelNode conDef) throws XMLStreamException {
            streamWriter.writeStartElement(ResourceAdapter.Tag.CONNECTION_DEFINITION.getLocalName());
            CLASS_NAME.marshallAsAttribute(conDef, false, streamWriter);
            JNDINAME.marshallAsAttribute(conDef, false, streamWriter);
            ENABLED.marshallAsAttribute(conDef, false, streamWriter);
            USE_JAVA_CONTEXT.marshallAsAttribute(conDef, false, streamWriter);
            POOL_NAME.marshallAsAttribute(conDef, false, streamWriter);
            USE_CCM.marshallAsAttribute(conDef, false, streamWriter);


            writeConfigProperties(streamWriter, conDef);

            if (conDef.hasDefined(MAX_POOL_SIZE.getName()) || conDef.hasDefined(MIN_POOL_SIZE.getName()) || conDef.hasDefined(POOL_USE_STRICT_MIN.getName())
                    || conDef.hasDefined(POOL_PREFILL.getName())) {
                if (conDef.hasDefined(INTERLEAVING.getName()) || conDef.hasDefined(WRAP_XA_RESOURCE.getName()) || conDef.hasDefined(NOTXSEPARATEPOOL.getName())
                        || conDef.hasDefined(PAD_XID.getName()) || conDef.hasDefined(SAME_RM_OVERRIDE.getName())) {
                    streamWriter.writeStartElement(CommonConnDef.Tag.XA_POOL.getLocalName());
                    MIN_POOL_SIZE.marshallAsElement(conDef, false, streamWriter);
                    MAX_POOL_SIZE.marshallAsElement(conDef, false, streamWriter);
                    POOL_PREFILL.marshallAsElement(conDef, false, streamWriter);
                    POOL_FLUSH_STRATEGY.marshallAsElement(conDef, false, streamWriter);
                    POOL_USE_STRICT_MIN.marshallAsElement(conDef, false, streamWriter);

                    SAME_RM_OVERRIDE.marshallAsElement(conDef, false, streamWriter);
                    INTERLEAVING.marshallAsElement(conDef, false, streamWriter);
                    NOTXSEPARATEPOOL.marshallAsElement(conDef, false, streamWriter);
                    PAD_XID.marshallAsElement(conDef, false, streamWriter);
                    WRAP_XA_RESOURCE.marshallAsElement(conDef, false, streamWriter);

                    streamWriter.writeEndElement();
                } else {
                    streamWriter.writeStartElement(CommonConnDef.Tag.POOL.getLocalName());
                    MIN_POOL_SIZE.marshallAsElement(conDef, false, streamWriter);
                    MAX_POOL_SIZE.marshallAsElement(conDef, false, streamWriter);
                    POOL_PREFILL.marshallAsElement(conDef, false, streamWriter);
                    POOL_USE_STRICT_MIN.marshallAsElement(conDef, false, streamWriter);
                    streamWriter.writeEndElement();
                }

            }

            if (conDef.hasDefined(APPLICATION.getName()) || conDef.hasDefined(SECURITY_DOMAIN.getName())
                    || conDef.hasDefined(SECURITY_DOMAIN_AND_APPLICATION.getName())) {
                streamWriter.writeStartElement(CommonConnDef.Tag.SECURITY.getLocalName());
                APPLICATION.marshallAsElement(conDef, false, streamWriter);
                SECURITY_DOMAIN.marshallAsElement(conDef, false, streamWriter);
                SECURITY_DOMAIN_AND_APPLICATION.marshallAsElement(conDef, false, streamWriter);

                streamWriter.writeEndElement();
            }

            if (conDef.hasDefined(BLOCKING_TIMEOUT_WAIT_MILLIS.getName()) || conDef.hasDefined(IDLETIMEOUTMINUTES.getName()) || conDef.hasDefined(ALLOCATION_RETRY.getName())
                    || conDef.hasDefined(ALLOCATION_RETRY_WAIT_MILLIS.getName()) || conDef.hasDefined(XA_RESOURCE_TIMEOUT.getName())) {
                streamWriter.writeStartElement(CommonConnDef.Tag.TIMEOUT.getLocalName());
                BLOCKING_TIMEOUT_WAIT_MILLIS.marshallAsElement(conDef, false, streamWriter);
                IDLETIMEOUTMINUTES.marshallAsElement(conDef, false, streamWriter);
                ALLOCATION_RETRY.marshallAsElement(conDef, false, streamWriter);
                ALLOCATION_RETRY_WAIT_MILLIS.marshallAsElement(conDef, false, streamWriter);
                XA_RESOURCE_TIMEOUT.marshallAsElement(conDef, false, streamWriter);
                streamWriter.writeEndElement();
            }

            if (conDef.hasDefined(BACKGROUNDVALIDATION.getName()) || conDef.hasDefined(BACKGROUNDVALIDATIONMILLIS.getName()) || conDef.hasDefined(USE_FAST_FAIL.getName()) ) {
                streamWriter.writeStartElement(CommonConnDef.Tag.VALIDATION.getLocalName());
                BACKGROUNDVALIDATION.marshallAsElement(conDef, false, streamWriter);
                BACKGROUNDVALIDATIONMILLIS.marshallAsElement(conDef, false, streamWriter);
                USE_FAST_FAIL.marshallAsElement(conDef, false, streamWriter);
                streamWriter.writeEndElement();
            }

            if (conDef.hasDefined(RECOVERY_USERNAME.getName()) || conDef.hasDefined(RECOVERY_PASSWORD.getName())
                    || conDef.hasDefined(RECOVERY_SECURITY_DOMAIN.getName()) || conDef.hasDefined(RECOVERLUGIN_CLASSNAME.getName())
                    || conDef.hasDefined(RECOVERLUGIN_PROPERTIES.getName()) || conDef.hasDefined(NO_RECOVERY.getName())) {

                streamWriter.writeStartElement(CommonConnDef.Tag.RECOVERY.getLocalName());
                if (conDef.hasDefined(RECOVERY_USERNAME.getName()) || conDef.hasDefined(RECOVERY_PASSWORD.getName())
                        || conDef.hasDefined(RECOVERY_SECURITY_DOMAIN.getName())) {
                    streamWriter.writeStartElement(Recovery.Tag.RECOVER_CREDENTIAL.getLocalName());
                    RECOVERY_USERNAME.marshallAsElement(conDef, false, streamWriter);
                    RECOVERY_PASSWORD.marshallAsElement(conDef, false, streamWriter);
                    RECOVERY_SECURITY_DOMAIN.marshallAsElement(conDef, false, streamWriter);
                    streamWriter.writeEndElement();
                }
                if (conDef.hasDefined(RECOVERLUGIN_CLASSNAME.getName()) || conDef.hasDefined(RECOVERLUGIN_PROPERTIES.getName())) {
                    streamWriter.writeStartElement(Recovery.Tag.RECOVER_PLUGIN.getLocalName());
                    RECOVERLUGIN_CLASSNAME.marshallAsAttribute(conDef, false, streamWriter);
                    if (conDef.hasDefined(RECOVERLUGIN_PROPERTIES.getName())) {
                        for (Property property : conDef.get(RECOVERLUGIN_PROPERTIES.getName()).asPropertyList()) {
                            writeProperty(streamWriter, conDef, property.getName(), property
                                        .getValue().asString(), org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY.getLocalName());
                        }
                    }
                    streamWriter.writeEndElement();
                }
                NO_RECOVERY.marshallAsAttribute(conDef, false, streamWriter);

            }

            streamWriter.writeEndElement();

        }

        @Override
        public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {

            final ModelNode address = new ModelNode();
            address.add(ModelDescriptionConstants.SUBSYSTEM, RESOURCEADAPTERS_NAME);
            address.protect();

            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).set(address);

            list.add(subsystem);

            try {
                String localName = null;
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case RESOURCEADAPTERS_1_0: {
                        localName = reader.getLocalName();
                        final Element element = Element.forName(reader.getLocalName());
                        SUBSYSTEM_RA_LOGGER.tracef("%s -> %s", localName, element);
                        switch (element) {
                            case SUBSYSTEM: {
                                ResourceAdapterParser parser = new ResourceAdapterParser();
                                parser.parse(reader, list, address);
                                ParseUtils.requireNoContent(reader);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new XMLStreamException(e);
            }

        }

    }

    private static void setStringIfNotNull(final ModelNode node, final String identifier, final String value) {
        if (value != null) {
            node.get(identifier).set(value);
        }
    }

    private static void setExtensionIfNotNull(final ModelNode dsModel, final String extensionclassname,
            final String extensionProperties, final org.jboss.jca.common.api.metadata.common.Extension extension) {
        if (extension != null) {
            setStringIfNotNull(dsModel, extensionclassname, extension.getClassName());
            if (extension.getConfigPropertiesMap() != null) {
                for (Map.Entry<String, String> entry : extension.getConfigPropertiesMap().entrySet()) {
                    dsModel.get(extensionProperties, entry.getKey()).set(entry.getValue());
                }
            }
        }
    }

    private static void setBooleanIfNotNull(final ModelNode node, final String identifier, final Boolean value) {
        if (value != null) {
            node.get(identifier).set(value);
        }
    }

    private static void setBooleanIfNotNull(final ModelNode node, final String identifier,
                                            final Boolean value, final Boolean defaultValue) {
        if (value != null) {
            node.get(identifier).set(value);
        } else if (defaultValue != null) {
            node.get(identifier).set(defaultValue);
        }
    }

    private static void setIntegerIfNotNull(final ModelNode node, final String identifier, final Integer value) {
        if (value != null) {
            node.get(identifier).set(value);
        }
    }

    private static void setLongIfNotNull(final ModelNode node, final String identifier, final Long value) {
        if (value != null) {
            node.get(identifier).set(value);
        }
    }



    private static class ResourceAdaptersSubsystemDescribeHandler implements OperationStepHandler, DescriptionProvider {
        static final ResourceAdaptersSubsystemDescribeHandler INSTANCE = new ResourceAdaptersSubsystemDescribeHandler();

        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final ModelNode address = new ModelNode();
            address.add(ModelDescriptionConstants.SUBSYSTEM, RESOURCEADAPTERS_NAME);
            address.protect();

            final ModelNode add = new ModelNode();
            add.get(OP).set(ADD);
            add.get(OP_ADDR).set(address);

            ModelNode model = context.readModel(PathAddress.EMPTY_ADDRESS);

            // FIXME remove when equivalent workaround in
            // ResourceAdaptersSubsystemAdd is gone
            boolean workaround = true;

            if (workaround) {
                if (model.hasDefined(RESOURCEADAPTERS_NAME)) {
                    ModelNode ras = model.get(RESOURCEADAPTERS_NAME);
                    add.get(RESOURCEADAPTERS_NAME).set(ras);
                }
            } else {
                // TODO Fill in the details
            }

            context.getResult().add(add);
            context.completeStep();
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }
    }

}
