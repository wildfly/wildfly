/**
 *
 */
package org.jboss.as.model.socket;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.Attribute;
import org.jboss.as.model.Element;
import org.jboss.as.services.net.NetworkInterfaceService;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
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

    private final String name;
    private boolean anyLocalV4;
    private boolean anyLocalV6;
    private boolean anyLocal;
    private final NavigableMap<Element, AbstractInterfaceCriteriaElement<?>> interfaceCriteria =
            new TreeMap<Element, AbstractInterfaceCriteriaElement<?>>();

    /**
     * Creates a new InterfaceElement.
     *
     * @param name the interface name
     */
    public InterfaceElement(final String name) {
        this.name = name;
    }

    /**
     * Creates a new InterfaceElement from an existing
     * {@link InterfaceElement#isFullySpecified() fully specified}
     * {@link InterfaceElement}.
     *
     * @param fullySpecified the element to use as a base
     *
     * @throws NullPointerException if {@code fullySpecified} is {@code null}
     * @throws IllegalArgumentException if {@code fullySpecified.isFullySpecified()}
     *             returns {@code false}.
     */
    public InterfaceElement(InterfaceElement fullySpecified) {
        this(fullySpecified.getName(), fullySpecified.getCriteriaElements());
        if (!fullySpecified.isFullySpecified()) {
            throw new IllegalArgumentException(fullySpecified + " is not fully specified");
        }
        setAnyLocal(fullySpecified.isAnyLocalAddress());
        setAnyLocalV4(fullySpecified.isAnyLocalV4Address());
        setAnyLocalV6(fullySpecified.isAnyLocalV6Address());
    }

    /**
     * Creates a new InterfaceElmenet.
     *
     * @param name the interface name
     * @param criteriaList the interface criteria
     */
    protected InterfaceElement(String name, List<AbstractInterfaceCriteriaElement<?>> criteriaList) {
        if (name == null)
            throw new IllegalArgumentException("name is null");
        if (criteriaList == null)
            throw new IllegalArgumentException("criteria is null");
        this.name = name;
        for (AbstractInterfaceCriteriaElement<?> criteria : criteriaList) {
            interfaceCriteria.put(criteria.getElement(), criteria);
        }
    }

    @Override
    protected Class<InterfaceElement> getElementClass() {
        return InterfaceElement.class;
    }

    /**
     * Gets the name of the interface
     *
     * @return the interface name. Will not be <code>null</code>
     */
    public String getName() {
        return name;
    }

    public InterfaceCriteria getInterfaceCriteria() {
        return new OverallInterfaceCriteria();
    }

    public boolean isAnyLocalAddress() {
        return anyLocal;
    }

    void setAnyLocal(boolean anyLocal) {
        if (anyLocal) {
            validateAnyLocalAllowed(Element.ANY_ADDRESS);
        }
        this.anyLocal = anyLocal;
    }

    public boolean isAnyLocalV4Address() {
        return anyLocalV4;
    }

    void setAnyLocalV4(boolean anyLocalV4) {
        if (anyLocalV4) {
            validateAnyLocalAllowed(Element.ANY_IPV4_ADDRESS);
        }
        this.anyLocalV4 = anyLocalV4;
    }

    public boolean isAnyLocalV6Address() {
        return anyLocalV6;
    }

    void setAnyLocalV6(boolean anyLocalV6) {
        if (anyLocalV6) {
            validateAnyLocalAllowed(Element.ANY_IPV6_ADDRESS);
        }
        this.anyLocalV6 = anyLocalV6;
    }

    protected void addCriteria(AbstractInterfaceCriteriaElement<?> criteria) {
        this.interfaceCriteria.put(criteria.getElement(), criteria);
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
        return anyLocal || anyLocalV4 || anyLocalV6 || interfaceCriteria.size() > 0;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);
        if (anyLocal) {
            streamWriter.writeStartElement(Element.ANY_ADDRESS.getLocalName());
            streamWriter.writeEndElement();
        }
        else if (anyLocalV4) {
            streamWriter.writeStartElement(Element.ANY_IPV4_ADDRESS.getLocalName());
            streamWriter.writeEndElement();

        }
        else if (anyLocalV6) {
            streamWriter.writeStartElement(Element.ANY_IPV6_ADDRESS.getLocalName());
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

    /** {@inheritDoc} */
    public void activate(ServiceActivatorContext context) {
        context.getBatchBuilder().addService(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(name),
                new NetworkInterfaceService(name, anyLocalV4, anyLocalV6, anyLocal, getInterfaceCriteria()));
    }

    List<AbstractInterfaceCriteriaElement<?>> getCriteriaElements() {
        synchronized (interfaceCriteria) {
            return new ArrayList<AbstractInterfaceCriteriaElement<?>>(interfaceCriteria.values());
        }
    }

    protected void validateAnyLocalAllowed(Element element) throws IllegalStateException {
        if (interfaceCriteria.size() > 0) {
            throw new IllegalStateException(element + " cannot be combined with " + interfaceCriteria.keySet().iterator().next());
        }
        validateNotAnyLocal(element);
    }

    private void validateNotAnyLocal(Element element) throws IllegalStateException {
        if (anyLocal)
            throw new IllegalStateException(element + " cannot be combined with " + Element.ANY_ADDRESS.getLocalName());
        if (anyLocalV4)
            throw new IllegalStateException(element + " cannot be combined with " + Element.ANY_IPV4_ADDRESS.getLocalName());
        if (anyLocalV6)
            throw new IllegalStateException(element + " cannot be combined with " + Element.ANY_IPV6_ADDRESS.getLocalName());
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
