package org.jboss.as.clustering.jgroups.subsystem;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jgroups.protocols.TP;
import org.jgroups.stack.Protocol;

public class JGroupsSubsystemParser_1_0 implements XMLElementReader<List<ModelNode>> {

    private static final JGroupsSubsystemParser_1_0 INSTANCE = new JGroupsSubsystemParser_1_0();

    public static JGroupsSubsystemParser_1_0 getInstance() {
        return INSTANCE;
    }
    /**
     * {@inheritDoc}
     * @see org.jboss.staxmapper.XMLElementReader#readElement(org.jboss.staxmapper.XMLExtendedStreamReader, java.lang.Object)
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.SUBSYSTEM, JGroupsExtension.SUBSYSTEM_NAME);
        address.protect();

        ModelNode subsystem = Util.getEmptyOperation(ModelDescriptionConstants.ADD, address);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DEFAULT_STACK: {
                    subsystem.get(ModelKeys.DEFAULT_STACK).set(value);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (!subsystem.hasDefined(ModelKeys.DEFAULT_STACK)) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.DEFAULT_STACK));
        }

        operations.add(subsystem);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case JGROUPS_1_0: {
                    Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case STACK: {
                            operations.add(this.parseStack(reader, address));
                            break;
                        }
                        default: {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private ModelNode parseStack(XMLExtendedStreamReader reader, ModelNode address) throws XMLStreamException {

        String name = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (name == null) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.NAME));
        }

        final ModelNode stack = Util.getEmptyOperation(ModelDescriptionConstants.ADD, null);
        stack.get(ModelDescriptionConstants.OP_ADDR).set(address).add(ModelKeys.STACK, name);

        if (!reader.hasNext() || (reader.nextTag() == XMLStreamConstants.END_ELEMENT) || Element.forName(reader.getLocalName()) != Element.TRANSPORT) {
            throw ParseUtils.missingRequiredElement(reader, Collections.singleton(Element.TRANSPORT));
        }

        this.parseProtocol(reader, stack.get(ModelKeys.TRANSPORT), TP.class);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROTOCOL: {
                    this.parseProtocol(reader, stack.get(ModelKeys.PROTOCOL).add(), Protocol.class);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        return stack;
    }

    private void parseProtocol(XMLExtendedStreamReader reader, ModelNode protocol, Class<? extends Protocol> targetClass) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case TYPE: {
                    try {
                        targetClass.getClassLoader().loadClass(org.jgroups.conf.ProtocolConfiguration.protocol_prefix + '.' + value).asSubclass(targetClass).newInstance();
                        protocol.get(ModelKeys.TYPE).set(value);
                    } catch (Exception e) {
                        throw ParseUtils.invalidAttributeValue(reader, i);
                    }
                    break;
                }
                case SHARED: {
                    protocol.get(ModelKeys.SHARED).set(Boolean.parseBoolean(value));
                    break;
                }
                case SOCKET_BINDING: {
                    protocol.get(ModelKeys.SOCKET_BINDING).set(value);
                    break;
                }
                case DIAGNOSTICS_SOCKET_BINDING: {
                    protocol.get(ModelKeys.DIAGNOSTICS_SOCKET_BINDING).set(value);
                    break;
                }
                case DEFAULT_EXECUTOR: {
                    protocol.get(ModelKeys.DEFAULT_EXECUTOR).set(value);
                    break;
                }
                case OOB_EXECUTOR: {
                    protocol.get(ModelKeys.OOB_EXECUTOR).set(value);
                    break;
                }
                case TIMER_EXECUTOR: {
                    protocol.get(ModelKeys.TIMER_EXECUTOR).set(value);
                    break;
                }
                case THREAD_FACTORY: {
                    protocol.get(ModelKeys.THREAD_FACTORY).set(value);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (!protocol.hasDefined(ModelKeys.TYPE)) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.TYPE));
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            if (Element.forName(reader.getLocalName()) != Element.PROPERTY) {
                throw ParseUtils.unexpectedElement(reader);
            }
            int attributes = reader.getAttributeCount();
            String property = null;
            for (int i = 0; i < attributes; i++) {
                String value = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        property = value;
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            if (property == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
            }
            String value = reader.getElementText();
            protocol.get(ModelKeys.PROPERTY).add(property, value);
        }
    }
}
