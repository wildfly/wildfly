/**
 * 
 */
package org.jboss.as.model.socket;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.as.model.Attribute;
import org.jboss.as.model.Element;
import org.jboss.as.model.Namespace;
import org.jboss.as.net.NetworkInterfaceService;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A named interface definition, with an optional specification of what address
 * to use for the interface or an
 * optional set of {@link AbstractInterfaceCriteriaElement criteria} for
 * determining at runtime what address to use for the interface.
 * 
 * @author Brian Stansberry
 */
public class InterfaceElement extends AbstractModelElement<InterfaceElement> implements ServiceActivator {

    private static final long serialVersionUID = -5256526713311518506L;
    private static final Logger log = Logger.getLogger("org.jboss.as.socket");

    private final String name;
    private String address;
    private final NavigableMap<Element, AbstractInterfaceCriteriaElement<?>> interfaceCriteria = 
            new TreeMap<Element, AbstractInterfaceCriteriaElement<?>>();
    
    /**
     * Creates a new InterfaceElement by parsing an xml stream
     * 
     * @param reader stream reader used to read the xml
     * @throws XMLStreamException if an error occurs
     */
    public InterfaceElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        this(reader, false);
    }
    
    /**
     * Creates a new NonCriteriaElement by parsing an xml stream
     * 
     * @param reader stream reader used to the xml
     * @param criteriaRequired <code>true</code> if the element content must
     *         include criteria to identify the IP address to use for the
     *         interface; <code>false</code> if that is not required
     *         
     * @throws XMLStreamException if an error occurs
     */
    protected InterfaceElement(XMLExtendedStreamReader reader, boolean criteriaRequired) throws XMLStreamException {
        super(reader);
        // Handle attributes
        String name = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = value;
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        this.name = name;
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case INET_ADDRESS: {
                            if (address != null) {
                                throw new XMLStreamException("inet-address already set", reader.getLocation());
                            }
                            else if (! interfaceCriteria.isEmpty()) {
                                throw new XMLStreamException("Cannot use element inet-address in combination with other interface criteria elements", reader.getLocation());
                            }
                            address = readStringAttributeElement(reader, Attribute.VALUE.getLocalName());
                            break;
                        }
                        case ANY:
                        case NOT:{
                            requireNoAddress(reader, element);
                            CompoundCriteriaElement criteria = new CompoundCriteriaElement(reader, element == Element.ANY);
                            interfaceCriteria.put(criteria.getElement(), criteria);
                            break;
                        }
                        default: {
                            requireNoAddress(reader, element);
                            AbstractInterfaceCriteriaElement<?> criteria = ParsingUtil.parseSimpleInterfaceCriteria(reader, element);
                            interfaceCriteria.put(criteria.getElement(), criteria);
                            break;
                        }
                    }
                    break;
                }
                default: throw unexpectedElement(reader);
            }
        }
        if (criteriaRequired && address == null && interfaceCriteria.isEmpty()) {
            throw new XMLStreamException("Either an inet-address element or some interface criteria element is required", reader.getLocation());
        }
    }

    private void requireNoAddress(XMLExtendedStreamReader reader, final Element element) throws XMLStreamException {
        if (address != null) {
            throw new XMLStreamException("Cannot use interface criteria element " + element.getLocalName() + " since inet-address is already set", reader.getLocation());
        }
    }
    
    /**
     * Gets the name of the interface
     * 
     * @return the interface name. Will not be <code>null</code>
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the IP address or host name to use for this interface.
     * 
     * @return an InetAddress in string form, or a host name, or <code>null</code>
     */
    public String getAddress() {
        return address;
    }

    /**
     * Gets the IP address or host name to use for this interface.
     * 
     * @param address an InetAddress in string form, or a host name, or <code>null</code>
     */
    void setAddress(String address) {
        this.address = address;
    }

    public InterfaceCriteria getInterfaceCriteria() {
        if (address != null) {
            throw new IllegalStateException(InterfaceCriteria.class.getSimpleName() + 
                    " is not available when an IP address is configured; call " +
                    "getIpAddress() before asking for an " + InterfaceCriteria.class.getSimpleName());
        }
        return new OverallInterfaceCriteria();
    }
    
    /**
     * Gets whether this element is configured with necessary information needed
     * to determine an IP address for the interface; either via a directly
     * specified {@link #getAddress() address} or via at least one address
     * selection criteria.
     * 
     * @return <code>true</code> if the necessary information is available, <code>false</code>
     *         otherwise
     */
    public boolean isFullySpecified() {
        return address != null || interfaceCriteria.size() > 0;
    }

    @Override
    protected void appendDifference(Collection<AbstractModelUpdate<InterfaceElement>> target, InterfaceElement other) {
        // FIXME implement appendDifference
        throw new UnsupportedOperationException("implement me");
    }

    @Override
    public long elementHash() {
        long hash = name.hashCode()  & 0xFFFFFFFF;
        if (address != null) hash = Long.rotateLeft(hash, 1) ^ address.hashCode() & 0xffffffffL;
        hash = calculateElementHashOf(interfaceCriteria.values(), hash);
        return hash;
    }

    @Override
    protected Class<InterfaceElement> getElementClass() {
        return InterfaceElement.class;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);
        if (address != null) {
            streamWriter.writeStartElement(Element.INET_ADDRESS.getLocalName());
            streamWriter.writeAttribute(Attribute.VALUE.getLocalName(), address);
            streamWriter.writeEndElement();
        }
        else if (! interfaceCriteria.isEmpty()) {
            for (AbstractInterfaceCriteriaElement<?> criteria : interfaceCriteria.values()) {
                streamWriter.writeStartElement(criteria.getElement().getLocalName());
                criteria.writeContent(streamWriter);
            }
        }

        streamWriter.writeEndElement();
    }

    @Override
    public void activate(ServiceActivatorContext context) {
        log.info("Activating interface element:" + name);
        context.getBatchBuilder().addService(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(getName()),
        		new NetworkInterfaceService(this)).setInitialMode(Mode.ON_DEMAND);
    }

    private class OverallInterfaceCriteria implements InterfaceCriteria {

        private static final long serialVersionUID = 2784447904647077246L;

        @Override
        public boolean isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {
            
            for (AbstractInterfaceCriteriaElement<?> criteria : interfaceCriteria.values()) {
                if (! criteria.getInterfaceCriteria().isAcceptable(networkInterface, address))
                    return false;
            }
            return true;
        }
        
    }

}
