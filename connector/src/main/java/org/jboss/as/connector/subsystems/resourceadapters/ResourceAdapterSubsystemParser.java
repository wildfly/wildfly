package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.logging.ConnectorLogger.SUBSYSTEM_RA_LOGGER;
import static org.jboss.as.connector.subsystems.common.pool.Constants.BACKGROUNDVALIDATION;
import static org.jboss.as.connector.subsystems.common.pool.Constants.BACKGROUNDVALIDATIONMILLIS;
import static org.jboss.as.connector.subsystems.common.pool.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.common.pool.Constants.IDLETIMEOUTMINUTES;
import static org.jboss.as.connector.subsystems.common.pool.Constants.MAX_POOL_SIZE;
import static org.jboss.as.connector.subsystems.common.pool.Constants.MIN_POOL_SIZE;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_FLUSH_STRATEGY;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_PREFILL;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.subsystems.common.pool.Constants.USE_FAST_FAIL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ADMIN_OBJECTS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ARCHIVE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BEANVALIDATION_GROUPS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.BOOTSTRAP_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CLASS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONFIG_PROPERTIES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONNECTIONDEFINITIONS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.INTERLEAVING;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.JNDINAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.NOTXSEPARATEPOOL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.NO_RECOVERY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.PAD_XID;
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
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.TRANSACTION_SUPPORT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_CCM;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WRAP_XA_RESOURCE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.XA_RESOURCE_TIMEOUT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.common.TransactionSupportEnum;
import org.jboss.jca.common.api.metadata.common.v10.CommonConnDef;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapters;
import org.jboss.jca.common.api.metadata.resourceadapter.v10.ResourceAdapter;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
* @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
*/
public final class ResourceAdapterSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
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
        if (ra.hasDefined(ARCHIVE.getName())) {
            streamWriter.writeStartElement(ARCHIVE.getXmlName());
            String archive = ra.get(ARCHIVE.getName()).asString();
            if (archive.contains(ConnectorServices.RA_SERVICE_NAME_SEPARATOR)) {
                streamWriter.writeCharacters(archive.substring(0, archive.indexOf(ConnectorServices.RA_SERVICE_NAME_SEPARATOR)));
            } else {
                streamWriter.writeCharacters(archive);
            }

            streamWriter.writeEndElement();
        }
        BOOTSTRAP_CONTEXT.marshallAsElement(ra, streamWriter);

        if (ra.hasDefined(BEANVALIDATION_GROUPS.getName())) {
            streamWriter.writeStartElement(ResourceAdapter.Tag.BEAN_VALIDATION_GROUPS.getLocalName());
            for (ModelNode bvg : ra.get(BEANVALIDATION_GROUPS.getName()).asList()) {
                streamWriter.writeStartElement(BEANVALIDATION_GROUPS.getXmlName());
                streamWriter.writeCharacters(bvg.asString());
                streamWriter.writeEndElement();
            }
            streamWriter.writeEndElement();
        }

        TRANSACTION_SUPPORT.marshallAsElement(ra, streamWriter);
        writeNewConfigProperties(streamWriter, ra);
        TransactionSupportEnum transactionSupport = ra.hasDefined(TRANSACTION_SUPPORT.getName()) ? TransactionSupportEnum
            .valueOf(ra.get(TRANSACTION_SUPPORT.getName()).asString()) : null;
        boolean isXa = false;
        if (transactionSupport == TransactionSupportEnum.XATransaction) {
            isXa = true;
        }
        if (ra.hasDefined(CONNECTIONDEFINITIONS_NAME)) {
            streamWriter.writeStartElement(ResourceAdapter.Tag.CONNECTION_DEFINITIONS.getLocalName());
            for (Property conDef : ra.get(CONNECTIONDEFINITIONS_NAME).asPropertyList()) {
                writeConDef(streamWriter, conDef.getValue(), conDef.getName(), isXa);
            }
            streamWriter.writeEndElement();
        }

        if (ra.hasDefined(ADMIN_OBJECTS_NAME)) {
            streamWriter.writeStartElement(ResourceAdapter.Tag.ADMIN_OBJECTS.getLocalName());
            for (Property adminObject : ra.get(ADMIN_OBJECTS_NAME).asPropertyList()) {
                writeAdminObject(streamWriter, adminObject.getValue(), adminObject.getName());
            }
            streamWriter.writeEndElement();
        }
        streamWriter.writeEndElement();

    }

    private void writeNewConfigProperties(XMLExtendedStreamWriter streamWriter, ModelNode ra) throws XMLStreamException {
        if (ra.hasDefined(CONFIG_PROPERTIES.getName())) {
            for (Property connectionProperty : ra.get(CONFIG_PROPERTIES.getName()).asPropertyList()) {
                writeProperty(streamWriter, ra, connectionProperty.getName(), connectionProperty
                        .getValue().get("value").asString(), ResourceAdapter.Tag.CONFIG_PROPERTY.getLocalName());
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


    private void writeAdminObject(XMLExtendedStreamWriter streamWriter, ModelNode adminObject, final String poolName) throws XMLStreamException {
        streamWriter.writeStartElement(ResourceAdapter.Tag.ADMIN_OBJECT.getLocalName());
        CLASS_NAME.marshallAsAttribute(adminObject, streamWriter);
        JNDINAME.marshallAsAttribute(adminObject, streamWriter);
        ENABLED.marshallAsAttribute(adminObject, streamWriter);
        USE_JAVA_CONTEXT.marshallAsAttribute(adminObject, streamWriter);
        streamWriter.writeAttribute("pool-name", poolName);

        writeNewConfigProperties(streamWriter, adminObject);
        streamWriter.writeEndElement();

    }

    private void writeConDef(XMLExtendedStreamWriter streamWriter, ModelNode conDef, final String poolName, final boolean isXa) throws XMLStreamException {
        streamWriter.writeStartElement(ResourceAdapter.Tag.CONNECTION_DEFINITION.getLocalName());
        CLASS_NAME.marshallAsAttribute(conDef, streamWriter);
        JNDINAME.marshallAsAttribute(conDef, streamWriter);
        ENABLED.marshallAsAttribute(conDef, streamWriter);
        USE_JAVA_CONTEXT.marshallAsAttribute(conDef, streamWriter);
        streamWriter.writeAttribute("pool-name", poolName);
        USE_CCM.marshallAsAttribute(conDef, streamWriter);


        writeNewConfigProperties(streamWriter, conDef);

        if (conDef.hasDefined(MAX_POOL_SIZE.getName()) || conDef.hasDefined(MIN_POOL_SIZE.getName()) ||
            conDef.hasDefined(POOL_USE_STRICT_MIN.getName()) || conDef.hasDefined(POOL_PREFILL.getName()) ||
            conDef.hasDefined(POOL_FLUSH_STRATEGY.getName())) {
            if (isXa) {

                streamWriter.writeStartElement(CommonConnDef.Tag.XA_POOL.getLocalName());
                MIN_POOL_SIZE.marshallAsElement(conDef, streamWriter);
                MAX_POOL_SIZE.marshallAsElement(conDef, streamWriter);
                POOL_PREFILL.marshallAsElement(conDef, streamWriter);
                POOL_USE_STRICT_MIN.marshallAsElement(conDef, streamWriter);
                POOL_FLUSH_STRATEGY.marshallAsElement(conDef, streamWriter);

                SAME_RM_OVERRIDE.marshallAsElement(conDef, streamWriter);
                if (conDef.hasDefined(INTERLEAVING.getName()) && conDef.get(INTERLEAVING.getName()).asBoolean()) {
                    streamWriter.writeEmptyElement(INTERLEAVING.getXmlName());
                } else {
                    INTERLEAVING.marshallAsElement(conDef, streamWriter);
                }
                if (conDef.hasDefined(NOTXSEPARATEPOOL.getName()) && conDef.get(NOTXSEPARATEPOOL.getName()).asBoolean()) {
                    streamWriter.writeEmptyElement(NOTXSEPARATEPOOL.getXmlName());
                } else {
                    NOTXSEPARATEPOOL.marshallAsElement(conDef, streamWriter);
                }
                PAD_XID.marshallAsElement(conDef, streamWriter);
                WRAP_XA_RESOURCE.marshallAsElement(conDef, streamWriter);

                streamWriter.writeEndElement();
            } else {
                streamWriter.writeStartElement(CommonConnDef.Tag.POOL.getLocalName());
                MIN_POOL_SIZE.marshallAsElement(conDef, streamWriter);
                MAX_POOL_SIZE.marshallAsElement(conDef, streamWriter);
                POOL_PREFILL.marshallAsElement(conDef, streamWriter);
                POOL_USE_STRICT_MIN.marshallAsElement(conDef, streamWriter);
                POOL_FLUSH_STRATEGY.marshallAsElement(conDef, streamWriter);
                streamWriter.writeEndElement();
            }
        }

        if (conDef.hasDefined(APPLICATION.getName()) || conDef.hasDefined(SECURITY_DOMAIN.getName())
                || conDef.hasDefined(SECURITY_DOMAIN_AND_APPLICATION.getName())) {
            streamWriter.writeStartElement(CommonConnDef.Tag.SECURITY.getLocalName());
            if (conDef.hasDefined(APPLICATION.getName()) && conDef.get(APPLICATION.getName()).asBoolean()) {
                streamWriter.writeEmptyElement(APPLICATION.getXmlName());
            } else {
                APPLICATION.marshallAsElement(conDef, streamWriter);
            }
            SECURITY_DOMAIN.marshallAsElement(conDef, streamWriter);
            SECURITY_DOMAIN_AND_APPLICATION.marshallAsElement(conDef, streamWriter);

            streamWriter.writeEndElement();
        }

        if (conDef.hasDefined(BLOCKING_TIMEOUT_WAIT_MILLIS.getName()) || conDef.hasDefined(IDLETIMEOUTMINUTES.getName()) || conDef.hasDefined(ALLOCATION_RETRY.getName())
                || conDef.hasDefined(ALLOCATION_RETRY_WAIT_MILLIS.getName()) || conDef.hasDefined(XA_RESOURCE_TIMEOUT.getName())) {
            streamWriter.writeStartElement(CommonConnDef.Tag.TIMEOUT.getLocalName());
            BLOCKING_TIMEOUT_WAIT_MILLIS.marshallAsElement(conDef, streamWriter);
            IDLETIMEOUTMINUTES.marshallAsElement(conDef, streamWriter);
            ALLOCATION_RETRY.marshallAsElement(conDef, streamWriter);
            ALLOCATION_RETRY_WAIT_MILLIS.marshallAsElement(conDef, streamWriter);
            XA_RESOURCE_TIMEOUT.marshallAsElement(conDef, streamWriter);
            streamWriter.writeEndElement();
        }

        if (conDef.hasDefined(BACKGROUNDVALIDATION.getName()) || conDef.hasDefined(BACKGROUNDVALIDATIONMILLIS.getName()) || conDef.hasDefined(USE_FAST_FAIL.getName()) ) {
            streamWriter.writeStartElement(CommonConnDef.Tag.VALIDATION.getLocalName());
            BACKGROUNDVALIDATION.marshallAsElement(conDef, streamWriter);
            BACKGROUNDVALIDATIONMILLIS.marshallAsElement(conDef, streamWriter);
            USE_FAST_FAIL.marshallAsElement(conDef, streamWriter);
            streamWriter.writeEndElement();
        }

        if (conDef.hasDefined(RECOVERY_USERNAME.getName()) || conDef.hasDefined(RECOVERY_PASSWORD.getName())
                || conDef.hasDefined(RECOVERY_SECURITY_DOMAIN.getName()) || conDef.hasDefined(RECOVERLUGIN_CLASSNAME.getName())
                || conDef.hasDefined(RECOVERLUGIN_PROPERTIES.getName()) || conDef.hasDefined(NO_RECOVERY.getName())) {

            streamWriter.writeStartElement(CommonConnDef.Tag.RECOVERY.getLocalName());
            NO_RECOVERY.marshallAsAttribute(conDef, streamWriter);

            if (conDef.hasDefined(RECOVERY_USERNAME.getName()) || conDef.hasDefined(RECOVERY_PASSWORD.getName())
                    || conDef.hasDefined(RECOVERY_SECURITY_DOMAIN.getName())) {
                streamWriter.writeStartElement(Recovery.Tag.RECOVER_CREDENTIAL.getLocalName());
                RECOVERY_USERNAME.marshallAsElement(conDef, streamWriter);
                RECOVERY_PASSWORD.marshallAsElement(conDef, streamWriter);
                RECOVERY_SECURITY_DOMAIN.marshallAsElement(conDef, streamWriter);
                streamWriter.writeEndElement();
            }
            if (conDef.hasDefined(RECOVERLUGIN_CLASSNAME.getName()) || conDef.hasDefined(RECOVERLUGIN_PROPERTIES.getName())) {
                streamWriter.writeStartElement(Recovery.Tag.RECOVER_PLUGIN.getLocalName());
                RECOVERLUGIN_CLASSNAME.marshallAsAttribute(conDef, streamWriter);
                if (conDef.hasDefined(RECOVERLUGIN_PROPERTIES.getName())) {
                    for (Property property : conDef.get(RECOVERLUGIN_PROPERTIES.getName()).asPropertyList()) {
                        writeProperty(streamWriter, conDef, property.getName(), property
                                    .getValue().asString(), org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY.getLocalName());
                    }
                }
                streamWriter.writeEndElement();
            }
            streamWriter.writeEndElement();

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
