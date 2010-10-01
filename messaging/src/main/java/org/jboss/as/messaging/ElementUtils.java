package org.jboss.as.messaging;

import org.hornetq.api.core.Pair;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.impl.Validators;
import org.hornetq.core.security.Role;
import org.hornetq.core.settings.impl.AddressFullMessagePolicy;
import org.hornetq.core.settings.impl.AddressSettings;
import org.jboss.as.model.ParseUtils;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author scott.stark@jboss.org
 * @version $Revision:$
 */
public class ElementUtils {
    static Logger log = Logger.getLogger("org.jboss.as.messaging");

    enum StaxEvent {
        START_ELEMENT(1),
        END_ELEMENT(2),
        PROCESSING_INSTRUCTION(3),
        CHARACTERS(4),
        COMMENT(5),
        SPACE(6),
        START_DOCUMENT(7),
        END_DOCUMENT(8),
        ENTITY_REFERENCE(9),
        ATTRIBUTE(10),
        DTD(11),
        CDATA(12),
        NAMESPACE(13),
        NOTATION_DECLARATION(14),
        ENTITY_DECLARATION(15);

        /** Stash the values for use as an array indexed by StaxEvent.tag-1 */
        private static StaxEvent[] EVENTS = values();
        private final int tag;

        StaxEvent(int tag) {
            this.tag = tag;
        }

        static StaxEvent tagToEvent(int tag) {
            return EVENTS[tag - 1];
        }
    }

    static XMLStreamException unexpectedAttribute(final XMLExtendedStreamReader reader, final int index) {
        return new XMLStreamException("Unexpected attribute '" + reader.getAttributeName(index) + "' encountered",
                reader.getLocation());
    }

    static XMLStreamException unexpectedElement(final XMLExtendedStreamReader reader) {
        return new XMLStreamException("Unexpected element '" + reader.getName() + "' encountered", reader.getLocation());
    }

    static TransportConfiguration parseTransportConfiguration(final XMLExtendedStreamReader reader, String name,
            Element parentElement) throws XMLStreamException {

        Map<String, Object> params = new HashMap<String, Object>();

        int tag = reader.getEventType();
        String localName = null;
        String clazz = null;
        do {
            tag = reader.nextTag();
            localName = reader.getLocalName();
            Element element = Element.forName(localName);
            if (localName.equals(parentElement.getLocalName()) == true)
                break;

            switch (element) {
                case FACTORY_CLASS:
                    clazz = reader.getElementText().trim();
                    break;
                case PARAM:
                    int count = reader.getAttributeCount();
                    String key = null,
                    value = null;
                    for (int n = 0; n < count; n++) {
                        String attrName = reader.getAttributeLocalName(n);
                        Attribute attribute = Attribute.forName(attrName);
                        switch (attribute) {
                            case KEY:
                                key = reader.getAttributeValue(n);
                                break;
                            case VALUE:
                                value = reader.getAttributeValue(n);
                                break;
                            default:
                                throw unexpectedAttribute(reader, n);
                        }
                    }
                    reader.discardRemainder();
                    params.put(key, value);
                    break;
            }
            // Scan to element end
        } while (reader.hasNext());

        return new TransportConfiguration(clazz, params, name);
    }

    static Pair<String, AddressSettings> parseAddressSettings(final XMLExtendedStreamReader reader, String match)
            throws XMLStreamException {
        AddressSettings addressSettings = new AddressSettings();

        Pair<String, AddressSettings> setting = new Pair<String, AddressSettings>(match, addressSettings);

        int tag = reader.getEventType();
        String localName = null;
        do {
            tag = reader.nextTag();
            localName = reader.getLocalName();
            final Element element = Element.forName(reader.getLocalName());

            switch (element) {
                case DEAD_LETTER_ADDRESS_NODE_NAME: {
                    SimpleString queueName = new SimpleString(reader.getElementText().trim());
                    addressSettings.setDeadLetterAddress(queueName);
                }
                    break;
                case EXPIRY_ADDRESS_NODE_NAME: {
                    SimpleString queueName = new SimpleString(reader.getElementText().trim());
                    addressSettings.setExpiryAddress(queueName);
                }
                    break;
                case REDELIVERY_DELAY_NODE_NAME: {
                    addressSettings.setRedeliveryDelay(Long.valueOf(reader.getElementText().trim()));
                }
                    break;
                case MAX_SIZE_BYTES_NODE_NAME: {
                    addressSettings.setMaxSizeBytes(Long.valueOf(reader.getElementText().trim()));
                }
                    break;
                case PAGE_SIZE_BYTES_NODE_NAME: {
                    addressSettings.setPageSizeBytes(Long.valueOf(reader.getElementText().trim()));
                }
                    break;
                case MESSAGE_COUNTER_HISTORY_DAY_LIMIT_NODE_NAME: {
                    addressSettings.setMessageCounterHistoryDayLimit(Integer.valueOf(reader.getElementText().trim()));
                }
                    break;
                case ADDRESS_FULL_MESSAGE_POLICY_NODE_NAME: {
                    String value = reader.getElementText().trim();
                    Validators.ADDRESS_FULL_MESSAGE_POLICY_TYPE.validate(
                            Element.ADDRESS_FULL_MESSAGE_POLICY_NODE_NAME.getLocalName(), value);
                    AddressFullMessagePolicy policy = null;
                    if (value.equals(AddressFullMessagePolicy.BLOCK.toString())) {
                        policy = AddressFullMessagePolicy.BLOCK;
                    } else if (value.equals(AddressFullMessagePolicy.DROP.toString())) {
                        policy = AddressFullMessagePolicy.DROP;
                    } else if (value.equals(AddressFullMessagePolicy.PAGE.toString())) {
                        policy = AddressFullMessagePolicy.PAGE;
                    }
                    addressSettings.setAddressFullMessagePolicy(policy);
                }
                    break;
                case LVQ_NODE_NAME: {
                    addressSettings.setLastValueQueue(Boolean.valueOf(reader.getElementText().trim()));
                }
                    break;
                case MAX_DELIVERY_ATTEMPTS: {
                    addressSettings.setMaxDeliveryAttempts(Integer.valueOf(reader.getElementText().trim()));
                }
                    break;
                case REDISTRIBUTION_DELAY_NODE_NAME: {
                    addressSettings.setRedistributionDelay(Long.valueOf(reader.getElementText().trim()));
                }
                    break;
                case SEND_TO_DLA_ON_NO_ROUTE: {
                    addressSettings.setSendToDLAOnNoRoute(Boolean.valueOf(reader.getElementText().trim()));
                }
                    break;
                default:
                    break;
            }

            reader.discardRemainder();
        } while (!reader.getLocalName().equals(Element.ADDRESS_SETTING.getLocalName())
                && reader.getEventType() != XMLExtendedStreamReader.END_ELEMENT);

        return setting;
    }

    static void writeTransportConfiguration(TransportConfiguration config, XMLExtendedStreamWriter streamWriter)
            throws XMLStreamException {

        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), config.getName());

        writeSimpleElement(Element.FACTORY_CLASS, config.getFactoryClassName(), streamWriter);
        Map<String, Object> params = config.getParams();
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                streamWriter.writeEmptyElement(Element.PARAM.getLocalName());
                streamWriter.writeAttribute(Attribute.KEY.getLocalName(), entry.getKey());
                streamWriter.writeAttribute(Attribute.VALUE.getLocalName(), entry.getValue().toString());
            }
        }
    }

    static void writeSimpleElement(final Element element, final String content, final XMLExtendedStreamWriter streamWriter)
            throws XMLStreamException {
        if (content != null && content.length() > 0) {
            streamWriter.writeStartElement(element.getLocalName());
            streamWriter.writeCharacters(content);
            streamWriter.writeEndElement();
        }
    }

    static void writeSimpleElement(final Element element, final SimpleString content, final XMLExtendedStreamWriter streamWriter)
            throws XMLStreamException {
        if (content != null) {
            writeSimpleElement(element, content.toString(), streamWriter);
        }
    }

    static void writeRoles(final Set<Role> roles, final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        Map<Attribute, String> rolesByType = new HashMap<Attribute, String>();

        for (Role role : roles) {
            String name = role.getName();
            if (role.isConsume()) {
                storeRoleToType(Attribute.CONSUME_NAME, name, rolesByType);
            }
            if (role.isCreateDurableQueue()) {
                storeRoleToType(Attribute.CREATEDURABLEQUEUE_NAME, name, rolesByType);
            }
            if (role.isCreateNonDurableQueue()) {
                storeRoleToType(Attribute.CREATE_NON_DURABLE_QUEUE_NAME, name, rolesByType);
            }
            if (role.isDeleteDurableQueue()) {
                storeRoleToType(Attribute.DELETEDURABLEQUEUE_NAME, name, rolesByType);
            }
            if (role.isDeleteNonDurableQueue()) {
                storeRoleToType(Attribute.DELETE_NON_DURABLE_QUEUE_NAME, name, rolesByType);
            }
            if (role.isManage()) {
                storeRoleToType(Attribute.MANAGE_NAME, name, rolesByType);
            }
            if (role.isSend()) {
                storeRoleToType(Attribute.SEND_NAME, name, rolesByType);
            }
        }

        for (Map.Entry<Attribute, String> entry : rolesByType.entrySet()) {
            streamWriter.writeStartElement(Element.PERMISSION_ELEMENT_NAME.getLocalName());
            streamWriter.writeAttribute(Attribute.TYPE_ATTR_NAME.getLocalName(), entry.getKey().getLocalName());
            streamWriter.writeAttribute(Attribute.ROLES_ATTR_NAME.getLocalName(), entry.getValue());
            streamWriter.writeEndElement();
        }
    }

    static Pair<String, Set<Role>> parseSecurityRoles(final XMLExtendedStreamReader reader, String match)
            throws XMLStreamException {

        HashSet<Role> securityRoles = new HashSet<Role>();

        Pair<String, Set<Role>> securityMatch = new Pair<String, Set<Role>>(match, securityRoles);

        ArrayList<String> send = new ArrayList<String>();
        ArrayList<String> consume = new ArrayList<String>();
        ArrayList<String> createDurableQueue = new ArrayList<String>();
        ArrayList<String> deleteDurableQueue = new ArrayList<String>();
        ArrayList<String> createNonDurableQueue = new ArrayList<String>();
        ArrayList<String> deleteNonDurableQueue = new ArrayList<String>();
        ArrayList<String> manageRoles = new ArrayList<String>();
        ArrayList<String> allRoles = new ArrayList<String>();

        int tag = reader.getEventType();
        String localName = null;
        do {
            tag = reader.nextTag();
            localName = reader.getLocalName();
            if (localName.equals(Element.PERMISSION_ELEMENT_NAME.getLocalName()) == false)
                break;
            final Element element = Element.forName(reader.getLocalName());

            List<String> roles = null;
            String type = null;
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                if (reader.getAttributeNamespace(i) != null) {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                } else {
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    switch (attribute) {
                        case ROLES_ATTR_NAME:
                            roles = reader.getListAttributeValue(i);
                            break;
                        case TYPE_ATTR_NAME:
                            type = reader.getAttributeValue(i);
                            break;
                        default:
                            throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }

            for (String role : roles) {
                if (Attribute.SEND_NAME.getLocalName().equals(type)) {
                    send.add(role.trim());
                } else if (Attribute.CONSUME_NAME.getLocalName().equals(type)) {
                    consume.add(role.trim());
                } else if (Attribute.CREATEDURABLEQUEUE_NAME.getLocalName().equals(type)) {
                    createDurableQueue.add(role);
                } else if (Attribute.DELETEDURABLEQUEUE_NAME.getLocalName().equals(type)) {
                    deleteDurableQueue.add(role);
                } else if (Attribute.CREATE_NON_DURABLE_QUEUE_NAME.getLocalName().equals(type)) {
                    createNonDurableQueue.add(role);
                } else if (Attribute.DELETE_NON_DURABLE_QUEUE_NAME.getLocalName().equals(type)) {
                    deleteNonDurableQueue.add(role);
                } else if (Attribute.CREATETEMPQUEUE_NAME.getLocalName().equals(type)) {
                    createNonDurableQueue.add(role);
                } else if (Attribute.DELETETEMPQUEUE_NAME.getLocalName().equals(type)) {
                    deleteNonDurableQueue.add(role);
                } else if (Attribute.MANAGE_NAME.getLocalName().equals(type)) {
                    manageRoles.add(role);
                }
                if (!allRoles.contains(role.trim())) {
                    allRoles.add(role.trim());
                }
            }
            // Scan to element end
            reader.discardRemainder();
        } while (reader.hasNext());

        for (String role : allRoles) {
            securityRoles.add(new Role(role, send.contains(role), consume.contains(role), createDurableQueue.contains(role),
                    deleteDurableQueue.contains(role), createNonDurableQueue.contains(role), deleteNonDurableQueue
                            .contains(role), manageRoles.contains(role)));
        }

        return securityMatch;
    }

    private static void storeRoleToType(Attribute type, String role, Map<Attribute, String> rolesByType) {
        String roleList = rolesByType.get(type);
        if (roleList == null) {
            roleList = role;
        } else {
            roleList += ", " + role;
        }
        rolesByType.put(type, roleList);
    }
}
