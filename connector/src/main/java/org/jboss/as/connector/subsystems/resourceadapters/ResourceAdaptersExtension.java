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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import static org.jboss.as.connector.pool.Constants.BACKGROUNDVALIDATION;
import static org.jboss.as.connector.pool.Constants.BACKGROUNDVALIDATIONMINUTES;
import static org.jboss.as.connector.pool.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS;
import static org.jboss.as.connector.pool.Constants.IDLETIMEOUTMINUTES;
import static org.jboss.as.connector.pool.Constants.MAX_POOL_SIZE;
import static org.jboss.as.connector.pool.Constants.MIN_POOL_SIZE;
import static org.jboss.as.connector.pool.Constants.POOL_PREFILL;
import static org.jboss.as.connector.pool.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.pool.Constants.USE_FAST_FAIL;
import org.jboss.as.connector.pool.PoolConfigurationRWHandler;
import org.jboss.as.connector.pool.PoolConfigurationRWHandler.PoolConfigurationReadHandler;
import org.jboss.as.connector.pool.PoolConfigurationRWHandler.RaPoolConfigurationWriteHandler;
import org.jboss.as.connector.pool.PoolMetrics;
import org.jboss.as.connector.pool.PoolOperations;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ADMIN_OBJECTS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ARCHIVE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BEANVALIDATIONGROUPS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BOOTSTRAPCONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CLASS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONFIG_PROPERTIES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONNECTIONDEFINITIONS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.FLUSH_STRATEGY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.INTERLIVING;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.JNDI_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.NOTXSEPARATEPOOL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.NO_RECOVERY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.PAD_XID;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.POOL_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERLUGIN_CLASSNAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERLUGIN_PROPERTIES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_PASSWORD;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_USERNAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTER;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTERS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SAME_RM_OVERRIDE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SECURITY_DOMAIN_AND_APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.TRANSACTIONSUPPORT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_CCM;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WRAP_XA_DATASOURCE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.XA_RESOURCE_TIMEOUT;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.ADD_RESOURCEADAPTER_DESC;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.FLUSH_ALL_CONNECTION_DESC;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.FLUSH_IDLE_CONNECTION_DESC;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.REMOVE_RESOURCEADAPTER_DESC;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.RESOURCEADAPTER_DESC;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.SUBSYSTEM;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.SUBSYSTEM_ADD_DESC;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.TEST_CONNECTION_DESC;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.jca.common.api.metadata.common.CommonAdminObject;
import org.jboss.jca.common.api.metadata.common.CommonConnDef;
import org.jboss.jca.common.api.metadata.common.CommonPool;
import org.jboss.jca.common.api.metadata.common.CommonSecurity;
import org.jboss.jca.common.api.metadata.common.CommonTimeOut;
import org.jboss.jca.common.api.metadata.common.CommonValidation;
import org.jboss.jca.common.api.metadata.common.CommonXaPool;
import org.jboss.jca.common.api.metadata.common.Credential;
import org.jboss.jca.common.api.metadata.common.Recovery;
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
public class ResourceAdaptersExtension implements Extension {

    private static final Logger log = Logger.getLogger("org.jboss.as.datasources");

    @Override
    public void initialize(final ExtensionContext context) {
        log.debugf("Initializing ResourceAdapters Extension");
        // Register the remoting subsystem
        final SubsystemRegistration registration = context.registerSubsystem(RESOURCEADAPTERS);

        registration.registerXMLElementWriter(ResourceAdapterSubsystemParser.INSTANCE);

        // Remoting subsystem description and operation handlers
        final ManagementResourceRegistration subsystem = registration.registerSubsystemModel(SUBSYSTEM);
        subsystem.registerOperationHandler(ADD, ResourceAdaptersSubSystemAdd.INSTANCE, SUBSYSTEM_ADD_DESC, false);
        subsystem.registerOperationHandler(DESCRIBE, ResourceAdaptersSubsystemDescribeHandler.INSTANCE,
                ResourceAdaptersSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        final ManagementResourceRegistration resourceadapter = subsystem.registerSubModel(PathElement.pathElement(RESOURCEADAPTER),
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
            boolean hasChildren = node.hasDefined(RESOURCEADAPTER) && node.get(RESOURCEADAPTER).asInt() > 0;

            context.startSubsystemElement(Namespace.CURRENT.getUriString(), !hasChildren);

            if (hasChildren) {
                writer.writeStartElement(Element.RESOURCE_ADAPTERS.getLocalName());
                for (Property property : node.get(RESOURCEADAPTER).asPropertyList()) {
                    final ModelNode ra = property.getValue();

                    writeRaElement(writer, ra);
                }
                writer.writeEndElement();
                // Close the subsystem element
                writer.writeEndElement();
            }
        }

        private void writeRaElement(XMLExtendedStreamWriter streamWriter, ModelNode ra) throws XMLStreamException {
            streamWriter.writeStartElement(ResourceAdapters.Tag.RESOURCE_ADPTER.getLocalName());

            writeElementIfHas(streamWriter, ra, ResourceAdapter.Tag.ARCHIVE, ARCHIVE);

            if (ra.has(BEANVALIDATIONGROUPS)) {
                for (ModelNode bvg : ra.get(BEANVALIDATIONGROUPS).asList()) {
                    writeElementIfHas(streamWriter, bvg, ResourceAdapter.Tag.BEAN_VALIDATION_GROUP, BEANVALIDATIONGROUPS);
                }
            }

            writeElementIfHas(streamWriter, ra, ResourceAdapter.Tag.BOOTSTRAP_CONTEXT, BOOTSTRAPCONTEXT);
            writeElementIfHas(streamWriter, ra, ResourceAdapter.Tag.TRANSACTION_SUPPORT, TRANSACTIONSUPPORT);
            writeConfigProperties(streamWriter, ra);

            if (ra.has(CONNECTIONDEFINITIONS)) {
                streamWriter.writeStartElement(ResourceAdapter.Tag.CONNECTION_DEFINITIONS.getLocalName());
                for (ModelNode conDef : ra.get(CONNECTIONDEFINITIONS).asList()) {
                    writeConDef(streamWriter, conDef);
                }
                streamWriter.writeEndElement();
            }

            if (ra.has(ADMIN_OBJECTS)) {
                streamWriter.writeStartElement(ResourceAdapter.Tag.ADMIN_OBJECTS.getLocalName());
                for (ModelNode adminObject : ra.get(ADMIN_OBJECTS).asList()) {
                    writeAdminObject(streamWriter, adminObject);
                }
                streamWriter.writeEndElement();
            }
            streamWriter.writeEndElement();

        }

        private void writeConfigProperties(XMLExtendedStreamWriter streamWriter, ModelNode ra) throws XMLStreamException {
            if (ra.has(CONFIG_PROPERTIES)) {
                for (ModelNode property : ra.get(CONFIG_PROPERTIES).asList()) {
                    streamWriter.writeStartElement(ResourceAdapter.Tag.CONFIG_PROPERTY.getLocalName());
                    streamWriter.writeCharacters(property.asString());
                    streamWriter.writeEndElement();
                }
            }
        }

        private void writeAdminObject(XMLExtendedStreamWriter streamWriter, ModelNode adminObject) throws XMLStreamException {
            streamWriter.writeStartElement(ResourceAdapter.Tag.ADMIN_OBJECT.getLocalName());
            writeAttributeIfHas(streamWriter, adminObject, CommonAdminObject.Attribute.CLASS_NAME, CLASS_NAME);
            writeAttributeIfHas(streamWriter, adminObject, CommonAdminObject.Attribute.JNDINAME, JNDI_NAME);
            writeAttributeIfHas(streamWriter, adminObject, CommonAdminObject.Attribute.ENABLED, ENABLED);
            writeAttributeIfHas(streamWriter, adminObject, CommonAdminObject.Attribute.USEJAVACONTEXT, USE_JAVA_CONTEXT);
            writeAttributeIfHas(streamWriter, adminObject, CommonAdminObject.Attribute.POOL_NAME, POOL_NAME);

            writeConfigProperties(streamWriter, adminObject);
            streamWriter.writeEndElement();

        }

        private void writeConDef(XMLExtendedStreamWriter streamWriter, ModelNode conDef) throws XMLStreamException {
            streamWriter.writeStartElement(ResourceAdapter.Tag.CONNECTION_DEFINITION.getLocalName());
            writeAttributeIfHas(streamWriter, conDef, CommonConnDef.Attribute.CLASS_NAME, CLASS_NAME);
            writeAttributeIfHas(streamWriter, conDef, CommonConnDef.Attribute.JNDINAME, JNDI_NAME);
            writeAttributeIfHas(streamWriter, conDef, CommonConnDef.Attribute.ENABLED, ENABLED);
            writeAttributeIfHas(streamWriter, conDef, CommonConnDef.Attribute.USEJAVACONTEXT, USE_JAVA_CONTEXT);
            writeAttributeIfHas(streamWriter, conDef, CommonConnDef.Attribute.POOL_NAME, POOL_NAME);
            writeAttributeIfHas(streamWriter, conDef, CommonConnDef.Attribute.USECCM, USE_CCM);

            writeConfigProperties(streamWriter, conDef);

            if (conDef.has(MAX_POOL_SIZE) || conDef.has(MIN_POOL_SIZE) || conDef.has(POOL_USE_STRICT_MIN)
                    || conDef.has(POOL_PREFILL)) {
                if (conDef.has(INTERLIVING) || conDef.has(WRAP_XA_DATASOURCE) || conDef.has(NOTXSEPARATEPOOL)
                        || conDef.has(PAD_XID) || conDef.has(SAME_RM_OVERRIDE)) {
                    streamWriter.writeStartElement(CommonConnDef.Tag.XA_POOL.getLocalName());
                    writeElementIfHas(streamWriter, conDef, CommonPool.Tag.MIN_POOL_SIZE, MIN_POOL_SIZE);
                    writeElementIfHas(streamWriter, conDef, CommonPool.Tag.MAXPOOLSIZE, MAX_POOL_SIZE);
                    writeElementIfHas(streamWriter, conDef, CommonPool.Tag.PREFILL, POOL_PREFILL);
                    writeElementIfHas(streamWriter, conDef, CommonPool.Tag.FLUSH_STRATEGY, FLUSH_STRATEGY);
                    writeElementIfHas(streamWriter, conDef, CommonPool.Tag.USE_STRICT_MIN, POOL_USE_STRICT_MIN);

                    writeElementIfHas(streamWriter, conDef, CommonXaPool.Tag.ISSAMERMOVERRIDEVALUE, SAME_RM_OVERRIDE);
                    writeElementIfHas(streamWriter, conDef, CommonXaPool.Tag.INTERLEAVING, INTERLIVING);
                    writeElementIfHas(streamWriter, conDef, CommonXaPool.Tag.NO_TX_SEPARATE_POOLS, NOTXSEPARATEPOOL);
                    writeElementIfHas(streamWriter, conDef, CommonXaPool.Tag.PAD_XID, PAD_XID);
                    writeElementIfHas(streamWriter, conDef, CommonXaPool.Tag.WRAP_XA_RESOURCE, WRAP_XA_DATASOURCE);

                    streamWriter.writeEndElement();
                } else {
                    streamWriter.writeStartElement(CommonConnDef.Tag.POOL.getLocalName());
                    writeElementIfHas(streamWriter, conDef, CommonPool.Tag.MIN_POOL_SIZE, MIN_POOL_SIZE);
                    writeElementIfHas(streamWriter, conDef, CommonPool.Tag.MAXPOOLSIZE, MAX_POOL_SIZE);
                    writeElementIfHas(streamWriter, conDef, CommonPool.Tag.PREFILL, POOL_PREFILL);
                    writeElementIfHas(streamWriter, conDef, CommonPool.Tag.USE_STRICT_MIN, POOL_USE_STRICT_MIN);
                    streamWriter.writeEndElement();
                }

            }

            if (conDef.hasDefined(APPLICATION) || conDef.hasDefined(SECURITY_DOMAIN)
                    || conDef.hasDefined(SECURITY_DOMAIN_AND_APPLICATION)) {
                streamWriter.writeStartElement(CommonConnDef.Tag.SECURITY.getLocalName());
                writeElementIfHas(streamWriter, conDef, CommonSecurity.Tag.APPLICATION, APPLICATION);
                writeElementIfHas(streamWriter, conDef, CommonSecurity.Tag.SECURITY_DOMAIN, SECURITY_DOMAIN);
                writeElementIfHas(streamWriter, conDef, CommonSecurity.Tag.SECURITY_DOMAIN_AND_APPLICATION,
                        SECURITY_DOMAIN_AND_APPLICATION);

                streamWriter.writeEndElement();
            }

            if (conDef.has(BLOCKING_TIMEOUT_WAIT_MILLIS) || conDef.has(IDLETIMEOUTMINUTES) || conDef.has(ALLOCATION_RETRY)
                    || conDef.has(ALLOCATION_RETRY_WAIT_MILLIS) || conDef.has(XA_RESOURCE_TIMEOUT)) {
                streamWriter.writeStartElement(CommonConnDef.Tag.TIMEOUT.getLocalName());
                writeElementIfHas(streamWriter, conDef, CommonTimeOut.Tag.BLOCKINGTIMEOUTMILLIS, BLOCKING_TIMEOUT_WAIT_MILLIS);
                writeElementIfHas(streamWriter, conDef, CommonTimeOut.Tag.IDLETIMEOUTMINUTES, IDLETIMEOUTMINUTES);
                writeElementIfHas(streamWriter, conDef, CommonTimeOut.Tag.ALLOCATIONRETRY, ALLOCATION_RETRY);
                writeElementIfHas(streamWriter, conDef, CommonTimeOut.Tag.ALLOCATIONRETRYWAITMILLIS,
                        ALLOCATION_RETRY_WAIT_MILLIS);
                writeElementIfHas(streamWriter, conDef, CommonTimeOut.Tag.XARESOURCETIMEOUT, XA_RESOURCE_TIMEOUT);
                streamWriter.writeEndElement();
            }

            if (conDef.has(BACKGROUNDVALIDATION) || conDef.has(BACKGROUNDVALIDATIONMINUTES) || conDef.has(USE_FAST_FAIL)) {
                streamWriter.writeStartElement(CommonConnDef.Tag.VALIDATION.getLocalName());
                writeElementIfHas(streamWriter, conDef, CommonValidation.Tag.BACKGROUNDVALIDATION, BACKGROUNDVALIDATION);
                writeElementIfHas(streamWriter, conDef, CommonValidation.Tag.BACKGROUNDVALIDATIONMINUTES,
                        BACKGROUNDVALIDATIONMINUTES);
                writeElementIfHas(streamWriter, conDef, CommonValidation.Tag.USEFASTFAIL, USE_FAST_FAIL);
                streamWriter.writeEndElement();
            }

            if (conDef.hasDefined(RECOVERY_USERNAME) || conDef.hasDefined(RECOVERY_PASSWORD)
                    || conDef.hasDefined(RECOVERY_SECURITY_DOMAIN) || conDef.hasDefined(RECOVERLUGIN_CLASSNAME)
                    || conDef.hasDefined(RECOVERLUGIN_PROPERTIES) || conDef.hasDefined(NO_RECOVERY)) {

                streamWriter.writeStartElement(CommonConnDef.Tag.RECOVERY.getLocalName());
                if (conDef.hasDefined(RECOVERY_USERNAME) || conDef.hasDefined(RECOVERY_PASSWORD)
                        || conDef.hasDefined(RECOVERY_SECURITY_DOMAIN)) {
                    streamWriter.writeStartElement(Recovery.Tag.RECOVER_CREDENTIAL.getLocalName());
                    writeElementIfHas(streamWriter, conDef, Credential.Tag.USERNAME.getLocalName(), RECOVERY_USERNAME);
                    writeElementIfHas(streamWriter, conDef, Credential.Tag.PASSWORD.getLocalName(), RECOVERY_PASSWORD);
                    writeElementIfHas(streamWriter, conDef, Credential.Tag.SECURITY_DOMAIN.getLocalName(),
                            RECOVERY_SECURITY_DOMAIN);
                    streamWriter.writeEndElement();
                }
                if (conDef.hasDefined(RECOVERLUGIN_CLASSNAME) || conDef.hasDefined(RECOVERLUGIN_PROPERTIES)) {
                    streamWriter.writeStartElement(Recovery.Tag.RECOVER_PLUGIN.getLocalName());
                    writeAttributeIfHas(streamWriter, conDef,
                            org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(),
                            RECOVERLUGIN_CLASSNAME);
                    if (conDef.hasDefined(RECOVERLUGIN_PROPERTIES)) {
                        for (ModelNode property : conDef.get(RECOVERLUGIN_PROPERTIES).asList()) {
                            streamWriter
                                    .writeStartElement(org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY
                                            .getLocalName());
                            streamWriter.writeCharacters(property.asString());
                            streamWriter.writeEndElement();
                        }
                    }
                    streamWriter.writeEndElement();
                }
                writeAttributeIfHas(streamWriter, conDef, Recovery.Attribute.NO_RECOVERY.getLocalName(), NO_RECOVERY);

            }

            streamWriter.writeEndElement();

        }

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, String localName, String identifier)
                throws XMLStreamException {
            if (has(node, identifier)) {

                writer.writeStartElement(localName);
                writer.writeCharacters(node.get(identifier).asString());
                writer.writeEndElement();
            }
        }

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, ResourceAdapter.Tag element,
                String identifier) throws XMLStreamException {
            writeElementIfHas(writer, node, element.getLocalName(), identifier);
        }

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, CommonPool.Tag element, String identifier)
                throws XMLStreamException {
            writeElementIfHas(writer, node, element.getLocalName(), identifier);
        }

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, CommonXaPool.Tag element,
                String identifier) throws XMLStreamException {
            writeElementIfHas(writer, node, element.getLocalName(), identifier);
        }

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, CommonSecurity.Tag element,
                String identifier) throws XMLStreamException {
            writeElementIfHas(writer, node, element.getLocalName(), identifier);
        }

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, CommonTimeOut.Tag element,
                String identifier) throws XMLStreamException {
            writeElementIfHas(writer, node, element.getLocalName(), identifier);
        }

        private void writeElementIfHas(XMLExtendedStreamWriter writer, ModelNode node, CommonValidation.Tag element,
                String identifier) throws XMLStreamException {
            writeElementIfHas(writer, node, element.getLocalName(), identifier);
        }

        private boolean has(ModelNode node, String name) {

            return node.has(name) && node.get(name).isDefined();
        }

        private void writeAttributeIfHas(final XMLExtendedStreamWriter writer, final ModelNode node,
                final CommonAdminObject.Attribute attr, final String identifier) throws XMLStreamException {
            if (has(node, identifier)) {
                writer.writeAttribute(attr.getLocalName(), node.get(identifier).asString());
            }
        }

        private void writeAttributeIfHas(final XMLExtendedStreamWriter writer, final ModelNode node, final String attrName,
                final String identifier) throws XMLStreamException {
            if (has(node, identifier)) {
                writer.writeAttribute(attrName, node.get(identifier).asString());
            }
        }

        private void writeAttributeIfHas(final XMLExtendedStreamWriter writer, final ModelNode node,
                final CommonConnDef.Attribute attr, final String identifier) throws XMLStreamException {
            if (has(node, identifier)) {
                writer.writeAttribute(attr.getLocalName(), node.get(identifier).asString());
            }
        }

        @Override
        public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {

            final ModelNode address = new ModelNode();
            address.add(ModelDescriptionConstants.SUBSYSTEM, RESOURCEADAPTERS);
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
                                ParseUtils.requireNoContent(reader);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new XMLStreamException(e);
            }

            if (ras != null && ras.getResourceAdapters() != null) {
                for (ResourceAdapter ra : ras.getResourceAdapters()) {
                    final ModelNode raAddress = address.clone();
                    raAddress.add(RESOURCEADAPTER, ra.getArchive());
                    raAddress.protect();

                    final ModelNode operation = new ModelNode();
                    operation.get(OP_ADDR).set(raAddress);
                    operation.get(OP).set(ADD);
                    if (ra.getConfigProperties() != null) {
                        for (Entry<String, String> entry : ra.getConfigProperties().entrySet()) {
                            operation.get(CONFIG_PROPERTIES, entry.getKey()).set(entry.getValue());
                        }
                    }
                    setStringIfNotNull(operation, ARCHIVE, ra.getArchive());
                    setStringIfNotNull(operation, TRANSACTIONSUPPORT, ra.getTransactionSupport() != null ? ra
                            .getTransactionSupport().name() : null);
                    setStringIfNotNull(operation, BOOTSTRAPCONTEXT, ra.getBootstrapContext());

                    if (ra.getBeanValidationGroups() != null) {
                        for (String beanValidationGroup : ra.getBeanValidationGroups()) {
                            operation.get(BEANVALIDATIONGROUPS).add(beanValidationGroup);
                        }
                    }

                    if (ra.getConnectionDefinitions() != null) {
                        for (CommonConnDef conDef : ra.getConnectionDefinitions()) {
                            operation.get(CONNECTIONDEFINITIONS).add(createConnectionDefinitionModel(conDef));

                        }
                    }

                    if (ra.getAdminObjects() != null) {
                        for (CommonAdminObject adminObject : ra.getAdminObjects()) {
                            operation.get(ADMIN_OBJECTS).add(createAdminObjectModel(adminObject));

                        }
                    }

                    list.add(operation);
                }
            }

        }

        private ModelNode createAdminObjectModel(CommonAdminObject adminObject) {
            ModelNode adminObjectModel = new ModelNode();
            for (Entry<String, String> entry : adminObject.getConfigProperties().entrySet()) {
                adminObjectModel.get(CONFIG_PROPERTIES, entry.getKey()).set(entry.getValue());
            }
            setStringIfNotNull(adminObjectModel, CLASS_NAME, adminObject.getClassName());
            setStringIfNotNull(adminObjectModel, JNDI_NAME, adminObject.getJndiName());
            setStringIfNotNull(adminObjectModel, POOL_NAME, adminObject.getPoolName());
            setBooleanIfNotNull(adminObjectModel, ENABLED, adminObject.isEnabled());
            setBooleanIfNotNull(adminObjectModel, USE_JAVA_CONTEXT, adminObject.isUseJavaContext());

            return adminObjectModel;
        }

        private ModelNode createConnectionDefinitionModel(CommonConnDef conDef) {
            ModelNode condefModel = new ModelNode();
            for (Entry<String, String> entry : conDef.getConfigProperties().entrySet()) {
                condefModel.get(CONFIG_PROPERTIES, entry.getKey()).set(entry.getValue());
            }
            setStringIfNotNull(condefModel, CLASS_NAME, conDef.getClassName());
            setStringIfNotNull(condefModel, JNDI_NAME, conDef.getJndiName());
            setStringIfNotNull(condefModel, POOL_NAME, conDef.getPoolName());
            setBooleanIfNotNull(condefModel, ENABLED, conDef.isEnabled() != null ? conDef.isEnabled() : true);
            setBooleanIfNotNull(condefModel, USE_JAVA_CONTEXT, conDef.isUseJavaContext() != null ? conDef.isUseJavaContext()
                    : true);
            setBooleanIfNotNull(condefModel, USE_CCM, conDef.isUseCcm());

            if (conDef.getPool() != null) {
                setIntegerIfNotNull(condefModel, MAX_POOL_SIZE, conDef.getPool().getMaxPoolSize());
                setIntegerIfNotNull(condefModel, MIN_POOL_SIZE, conDef.getPool().getMinPoolSize());
                setBooleanIfNotNull(condefModel, POOL_PREFILL, conDef.getPool().isPrefill());
                setBooleanIfNotNull(condefModel, POOL_USE_STRICT_MIN, conDef.getPool().isUseStrictMin());
                if (conDef.getPool().getFlushStrategy() != null) {
                    setStringIfNotNull(condefModel, FLUSH_STRATEGY, conDef.getPool().getFlushStrategy().name());
                }
                if (conDef.isXa()) {
                    CommonXaPool xaPool = (CommonXaPool) conDef.getPool();
                    setBooleanIfNotNull(condefModel, INTERLIVING, xaPool.isInterleaving());
                    setBooleanIfNotNull(condefModel, PAD_XID, xaPool.isPadXid());
                    setBooleanIfNotNull(condefModel, SAME_RM_OVERRIDE, xaPool.isSameRmOverride());
                    setBooleanIfNotNull(condefModel, NOTXSEPARATEPOOL, xaPool.isNoTxSeparatePool());
                    setBooleanIfNotNull(condefModel, WRAP_XA_DATASOURCE, xaPool.isWrapXaDataSource());

                }
            }

            if (conDef.getTimeOut() != null) {
                setIntegerIfNotNull(condefModel, ALLOCATION_RETRY, conDef.getTimeOut().getAllocationRetry());
                setLongIfNotNull(condefModel, ALLOCATION_RETRY_WAIT_MILLIS, conDef.getTimeOut().getAllocationRetryWaitMillis());
                setLongIfNotNull(condefModel, BLOCKING_TIMEOUT_WAIT_MILLIS, conDef.getTimeOut().getBlockingTimeoutMillis());
                setLongIfNotNull(condefModel, IDLETIMEOUTMINUTES, conDef.getTimeOut().getIdleTimeoutMinutes());
                setIntegerIfNotNull(condefModel, XA_RESOURCE_TIMEOUT, conDef.getTimeOut().getXaResourceTimeout());
            }

            if (conDef.getSecurity() != null) {
                setBooleanIfNotNull(condefModel, APPLICATION, conDef.getSecurity().isApplication());
                setStringIfNotNull(condefModel, SECURITY_DOMAIN, conDef.getSecurity().getSecurityDomain());
                setStringIfNotNull(condefModel, SECURITY_DOMAIN_AND_APPLICATION, conDef.getSecurity()
                        .getSecurityDomainAndApplication());

            }

            if (conDef.getValidation() != null) {
                setLongIfNotNull(condefModel, BACKGROUNDVALIDATIONMINUTES, conDef.getValidation()
                        .getBackgroundValidationMinutes());
                setBooleanIfNotNull(condefModel, BACKGROUNDVALIDATION, conDef.getValidation().isBackgroundValidation());
                setBooleanIfNotNull(condefModel, USE_FAST_FAIL, conDef.getValidation().isUseFastFail());
            }

            if (conDef.getRecovery() != null) {
                final Recovery recovery = conDef.getRecovery();
                setStringIfNotNull(condefModel, RECOVERY_USERNAME, recovery.getCredential() != null ? recovery.getCredential()
                        .getUserName() : null);
                setStringIfNotNull(condefModel, RECOVERY_PASSWORD, recovery.getCredential() != null ? recovery.getCredential()
                        .getPassword() : null);
                setStringIfNotNull(condefModel, RECOVERY_SECURITY_DOMAIN, recovery.getCredential() != null ? recovery
                        .getCredential().getSecurityDomain() : null);
                setExtensionIfNotNull(condefModel, RECOVERLUGIN_CLASSNAME, RECOVERLUGIN_PROPERTIES, recovery.getRecoverPlugin());
                setBooleanIfNotNull(condefModel, NO_RECOVERY, recovery.getNoRecovery());
            }

            return condefModel;
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
            address.add(ModelDescriptionConstants.SUBSYSTEM, RESOURCEADAPTERS);
            address.protect();

            final ModelNode add = new ModelNode();
            add.get(OP).set(ADD);
            add.get(OP_ADDR).set(address);

            ModelNode model = context.readModel(PathAddress.EMPTY_ADDRESS);

            // FIXME remove when equivalent workaround in
            // ResourceAdaptersSubsystemAdd is gone
            boolean workaround = true;

            if (workaround) {
                if (model.hasDefined(RESOURCEADAPTERS)) {
                    ModelNode datasources = model.get(RESOURCEADAPTERS);
                    add.get(RESOURCEADAPTERS).set(datasources);
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
