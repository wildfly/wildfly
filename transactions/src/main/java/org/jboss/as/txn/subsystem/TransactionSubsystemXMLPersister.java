/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.txn.subsystem;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The {@link XMLElementWriter} that handles the Transaction subsystem. As we only write out the most recent version of
 * a subsystem we only need to keep the latest version around.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
class TransactionSubsystemXMLPersister implements XMLElementWriter<SubsystemMarshallingContext> {

    public static final TransactionSubsystemXMLPersister INSTANCE = new TransactionSubsystemXMLPersister();

    private TransactionSubsystemXMLPersister() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {

        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

        ModelNode node = context.getModelNode();


        writer.writeStartElement(Element.CORE_ENVIRONMENT.getLocalName());

        TransactionSubsystemRootResourceDefinition.NODE_IDENTIFIER.marshallAsAttribute(node, writer);

        writeProcessId(writer, node);

        writer.writeEndElement();

        if (TransactionSubsystemRootResourceDefinition.BINDING.isMarshallable(node) ||
                TransactionSubsystemRootResourceDefinition.STATUS_BINDING.isMarshallable(node) ||
                TransactionSubsystemRootResourceDefinition.RECOVERY_LISTENER.isMarshallable(node)) {
            writer.writeStartElement(Element.RECOVERY_ENVIRONMENT.getLocalName());
            TransactionSubsystemRootResourceDefinition.BINDING.marshallAsAttribute(node, writer);

            TransactionSubsystemRootResourceDefinition.STATUS_BINDING.marshallAsAttribute(node, writer);

            TransactionSubsystemRootResourceDefinition.RECOVERY_LISTENER.marshallAsAttribute(node, writer);

            writer.writeEndElement();
        }
        if (TransactionSubsystemRootResourceDefinition.STATISTICS_ENABLED.isMarshallable(node)
                || TransactionSubsystemRootResourceDefinition.ENABLE_TSM_STATUS.isMarshallable(node)
                || TransactionSubsystemRootResourceDefinition.DEFAULT_TIMEOUT.isMarshallable(node)
                || TransactionSubsystemRootResourceDefinition.MAXIMUM_TIMEOUT.isMarshallable(node)) {

            writer.writeStartElement(Element.COORDINATOR_ENVIRONMENT.getLocalName());

            TransactionSubsystemRootResourceDefinition.STATISTICS_ENABLED.marshallAsAttribute(node, writer);
            TransactionSubsystemRootResourceDefinition.ENABLE_TSM_STATUS.marshallAsAttribute(node, writer);
            TransactionSubsystemRootResourceDefinition.DEFAULT_TIMEOUT.marshallAsAttribute(node, writer);
            TransactionSubsystemRootResourceDefinition.MAXIMUM_TIMEOUT.marshallAsAttribute(node, writer);

            writer.writeEndElement();
        }

        if (TransactionSubsystemRootResourceDefinition.OBJECT_STORE_RELATIVE_TO.isMarshallable(node)
                || TransactionSubsystemRootResourceDefinition.OBJECT_STORE_PATH.isMarshallable(node)) {
            writer.writeStartElement(Element.OBJECT_STORE.getLocalName());
            TransactionSubsystemRootResourceDefinition.OBJECT_STORE_PATH.marshallAsAttribute(node, writer);
            TransactionSubsystemRootResourceDefinition.OBJECT_STORE_RELATIVE_TO.marshallAsAttribute(node, writer);
            writer.writeEndElement();
        }

        if(node.hasDefined(CommonAttributes.JTS) && node.get(CommonAttributes.JTS).asBoolean()) {
            writer.writeStartElement(Element.JTS.getLocalName());
            writer.writeEndElement();
        }

        if(node.hasDefined(CommonAttributes.USE_JOURNAL_STORE) && node.get(CommonAttributes.USE_JOURNAL_STORE).asBoolean()) {
            writer.writeStartElement(Element.USE_JOURNAL_STORE.getLocalName());
            TransactionSubsystemRootResourceDefinition.JOURNAL_STORE_ENABLE_ASYNC_IO.marshallAsAttribute(node, writer);
            writer.writeEndElement();
        }

        if (node.hasDefined(CommonAttributes.USE_JDBC_STORE) && node.get(CommonAttributes.USE_JDBC_STORE).asBoolean()) {
            writer.writeStartElement(Element.JDBC_STORE.getLocalName());
            TransactionSubsystemRootResourceDefinition.JDBC_STORE_DATASOURCE.marshallAsAttribute(node, writer);
            if (TransactionSubsystemRootResourceDefinition.JDBC_ACTION_STORE_TABLE_PREFIX.isMarshallable(node)
                    || TransactionSubsystemRootResourceDefinition.JDBC_ACTION_STORE_DROP_TABLE.isMarshallable(node)) {
                writer.writeEmptyElement(Element.JDBC_ACTION_STORE.getLocalName());
                TransactionSubsystemRootResourceDefinition.JDBC_ACTION_STORE_TABLE_PREFIX.marshallAsAttribute(node, writer);
                TransactionSubsystemRootResourceDefinition.JDBC_ACTION_STORE_DROP_TABLE.marshallAsAttribute(node, writer);
            }
            if (TransactionSubsystemRootResourceDefinition.JDBC_COMMUNICATION_STORE_TABLE_PREFIX.isMarshallable(node)
                    || TransactionSubsystemRootResourceDefinition.JDBC_COMMUNICATION_STORE_DROP_TABLE.isMarshallable(node)) {
                writer.writeEmptyElement(Element.JDBC_COMMUNICATION_STORE.getLocalName());
                TransactionSubsystemRootResourceDefinition.JDBC_COMMUNICATION_STORE_TABLE_PREFIX.marshallAsAttribute(node, writer);
                TransactionSubsystemRootResourceDefinition.JDBC_COMMUNICATION_STORE_DROP_TABLE.marshallAsAttribute(node, writer);
            }
            if (TransactionSubsystemRootResourceDefinition.JDBC_STATE_STORE_TABLE_PREFIX.isMarshallable(node)
                    || TransactionSubsystemRootResourceDefinition.JDBC_STATE_STORE_DROP_TABLE.isMarshallable(node)) {
                writer.writeEmptyElement(Element.JDBC_STATE_STORE.getLocalName());
                TransactionSubsystemRootResourceDefinition.JDBC_STATE_STORE_TABLE_PREFIX.marshallAsAttribute(node, writer);
                TransactionSubsystemRootResourceDefinition.JDBC_STATE_STORE_DROP_TABLE.marshallAsAttribute(node, writer);
            }
            writer.writeEndElement();
        }

        if (node.hasDefined(CommonAttributes.CM_RESOURCE) && node.get(CommonAttributes.CM_RESOURCE).asList().size() > 0) {
            writer.writeStartElement(Element.CM_RESOURCES.getLocalName());
            for (Property cmr : node.get(CommonAttributes.CM_RESOURCE).asPropertyList()) {
                writer.writeStartElement(CommonAttributes.CM_RESOURCE);
                writer.writeAttribute(Attribute.JNDI_NAME.getLocalName(), cmr.getName());
                if (cmr.getValue().hasDefined(CMResourceResourceDefinition.CM_TABLE_NAME.getName()) ||
                        cmr.getValue().hasDefined(CMResourceResourceDefinition.CM_TABLE_BATCH_SIZE.getName()) ||
                        cmr.getValue().hasDefined(CMResourceResourceDefinition.CM_TABLE_IMMEDIATE_CLEANUP.getName())) {
                    writer.writeStartElement(Element.CM_TABLE.getLocalName());
                    CMResourceResourceDefinition.CM_TABLE_NAME.marshallAsAttribute(cmr.getValue(), writer);
                    CMResourceResourceDefinition.CM_TABLE_BATCH_SIZE.marshallAsAttribute(cmr.getValue(), writer);
                    CMResourceResourceDefinition.CM_TABLE_IMMEDIATE_CLEANUP.marshallAsAttribute(cmr.getValue(), writer);
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeProcessId(final XMLExtendedStreamWriter writer, final ModelNode value) throws XMLStreamException {
        writer.writeStartElement(Element.PROCESS_ID.getLocalName());
        if (value.get(TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.getName()).asBoolean(false)) {
            writer.writeEmptyElement(Element.UUID.getLocalName());
        } else {
            writer.writeStartElement(Element.SOCKET.getLocalName());
            TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.marshallAsAttribute(value, writer);
            TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_MAX_PORTS.marshallAsAttribute(value, writer);
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }
}
