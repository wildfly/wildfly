/**
 *
 */
package org.jboss.as.model.socket;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.interfaces.InterfaceCriteria;
import org.jboss.as.controller.interfaces.LinkLocalInterfaceCriteria;
import org.jboss.as.controller.interfaces.LoopbackInterfaceCriteria;
import org.jboss.as.controller.interfaces.PointToPointInterfaceCriteria;
import org.jboss.as.controller.interfaces.PublicAddressInterfaceCriteria;
import org.jboss.as.controller.interfaces.SiteLocalInterfaceCriteria;
import org.jboss.as.controller.interfaces.SupportsMulticastInterfaceCriteria;
import org.jboss.as.controller.interfaces.UpInterfaceCriteria;
import org.jboss.as.controller.interfaces.VirtualInterfaceCriteria;
import org.jboss.as.model.Attribute;
import org.jboss.as.model.Element;
import org.jboss.as.model.Namespace;
import org.jboss.as.model.ParseUtils;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parsing utility methods.
 *
 * @author Brian Stansberry
 */
public class InterfaceParsingUtils implements XMLStreamConstants {


    public static final Set<Element> SIMPLE_CRITERIA = EnumSet.of(Element.INET_ADDRESS, Element.LOOPBACK, Element.LINK_LOCAL_ADDRESS,
            Element.MULTICAST, Element.NIC, Element.NIC_MATCH, Element.POINT_TO_POINT, Element.SITE_LOCAL_ADDRESS,
            Element.PUBLIC_ADDRESS, Element.SUBNET_MATCH, Element.UP, Element.VIRTUAL);
    public static final String SIMPLE_CRITERIA_STRING;
    public static final Set<Element> ALL_CRITERIA = EnumSet.copyOf(SIMPLE_CRITERIA);
    public static final String ALL_CRITERIA_STRING;
    static {
        ALL_CRITERIA.add(Element.ANY);
        ALL_CRITERIA.add(Element.NOT);

        SIMPLE_CRITERIA_STRING = criteriaToString(SIMPLE_CRITERIA);
        ALL_CRITERIA_STRING = criteriaToString(ALL_CRITERIA);
    }

    private static String criteriaToString(Set<Element> criteria) {
        final StringBuilder b = new StringBuilder();
        Iterator<Element> iterator = criteria.iterator();
        while (iterator.hasNext()) {
            final Element element = iterator.next();
            b.append(element.getLocalName());
            if (iterator.hasNext()) {
                b.append(", ");
            }
        }
        return b.toString();
    }

    /**
     * Creates the appropriate AbstractInterfaceCriteriaElement for an element
     * of a type included in {@link InterfaceParsingUtils#SIMPLE_CRITERIA}.
     *
     * @return the criteria element
     *
     * @throws XMLStreamException if an error occurs
     */
    public static AbstractInterfaceCriteriaElement<?> parseSimpleInterfaceCriteria(XMLExtendedStreamReader reader, Element element) throws XMLStreamException {
        AbstractInterfaceCriteriaElement<?> result = null;
        switch (element) {
            case INET_ADDRESS: {
                result = new InetAddressMatchCriteriaElement(reader);
                break;
            }
            case LINK_LOCAL_ADDRESS: {
                result = createSimpleCriteria(reader, element, LinkLocalInterfaceCriteria.INSTANCE);
                break;
            }
            case LOOPBACK: {
                result = createSimpleCriteria(reader, element, LoopbackInterfaceCriteria.INSTANCE);
                break;
            }
            case MULTICAST: {
                result = createSimpleCriteria(reader, element, SupportsMulticastInterfaceCriteria.INSTANCE);
                break;
            }
            case POINT_TO_POINT: {
                result = createSimpleCriteria(reader, element, PointToPointInterfaceCriteria.INSTANCE);
                break;
            }
            case PUBLIC_ADDRESS: {
                result = createSimpleCriteria(reader, element, PublicAddressInterfaceCriteria.INSTANCE);
                break;
            }
            case SITE_LOCAL_ADDRESS: {
                result = createSimpleCriteria(reader, element, SiteLocalInterfaceCriteria.INSTANCE);
                break;
            }
            case UP: {
                result = createSimpleCriteria(reader, element, UpInterfaceCriteria.INSTANCE);
                break;
            }
            case VIRTUAL: {
                result = createSimpleCriteria(reader, element, VirtualInterfaceCriteria.INSTANCE);
                break;
            }
            case NIC: {
                final String name = ParseUtils.readStringAttributeElement(reader, Attribute.NAME.getLocalName());
                result = new NicCriteriaElement(name);
                break;
            }
            case NIC_MATCH: {
                result = createNicMatchCriteria(reader);
                break;
            }
            case SUBNET_MATCH: {
                result = createSubnetMatchCriteria(reader);
                break;
            }
            default: throw new XMLStreamException("Unexpected element '" + reader.getName() + "' encountered", reader.getLocation());
        }

        return result;
    }

    public static CompoundCriteriaElement createCompoundCriteria(final XMLExtendedStreamReader reader, boolean isAny) throws XMLStreamException {
        final Map<Element, AbstractInterfaceCriteriaElement<?>> interfaceCriteria = new HashMap<Element, AbstractInterfaceCriteriaElement<?>>();
        // Handle attributes
        ParseUtils.requireNoAttributes(reader);
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    AbstractInterfaceCriteriaElement<?> aice = InterfaceParsingUtils.parseSimpleInterfaceCriteria(reader, element);
                    interfaceCriteria.put(aice.getElement(), aice);
                    break;
                }
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
        if (interfaceCriteria.isEmpty()) {
            throw InterfaceParsingUtils.missingCriteria(reader, InterfaceParsingUtils.SIMPLE_CRITERIA_STRING);
        }
        return new CompoundCriteriaElement(new HashSet<AbstractInterfaceCriteriaElement<?>>(interfaceCriteria.values()), isAny);
    }

    /** Returns an XMLStreamException stating that at least one of <code>criteria</code>  must be supplied */
    static XMLStreamException missingCriteria(XMLExtendedStreamReader reader, String criteria) {
        return new XMLStreamException("At least one of the following elements must be supplied: " + criteria, reader.getLocation());
    }

    /** Create a nic match criteria element. */
    static NicMatchCriteriaElement createNicMatchCriteria(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // Handle attributes
        Pattern pattern = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case PATTERN: {
                        try {
                            pattern = Pattern.compile(value);
                        }
                        catch (PatternSyntaxException e) {
                            throw new XMLStreamException("Invalid pattern " + value + " (" + e.getLocalizedMessage() +")", reader.getLocation(), e);
                        }
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (pattern == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.PATTERN));
        }
        // Handle elements
        ParseUtils.requireNoContent(reader);
        return new NicMatchCriteriaElement(pattern);
    }

    /** Create a new subnet match criteria element. */
    static SubnetMatchCriteriaElement createSubnetMatchCriteria(final XMLExtendedStreamReader reader) throws XMLStreamException {
        String value = null;
        byte[] net = null;
        int mask = -1;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case VALUE: {
                        String[] split = null;
                        try {
                            split = value.split("/");
                            if (split.length != 2) {
                                throw new XMLStreamException("Invalid 'value' " + value + " -- must be of the form address/mask", reader.getLocation());
                            }
                            InetAddress addr = InetAddress.getByName(split[1]);
                            net = addr.getAddress();
                            mask = Integer.valueOf(split[1]);
                        }
                        catch (NumberFormatException e) {
                            throw new XMLStreamException("Invalid mask " + split[1] + " (" + e.getLocalizedMessage() +")", reader.getLocation(), e);
                        }
                        catch (UnknownHostException e) {
                            throw new XMLStreamException("Invalid address " + split[1] + " (" + e.getLocalizedMessage() +")", reader.getLocation(), e);
                        }
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (net == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.VALUE));
        }
        // Handle elements
        ParseUtils.requireNoContent(reader);
        return new SubnetMatchCriteriaElement(value, net, mask);
    }

    /** Create a simple criteria element. */
    static SimpleCriteriaElement createSimpleCriteria(final XMLExtendedStreamReader reader, Element type, InterfaceCriteria criteria)
        throws XMLStreamException {
        if (criteria == null) {
            throw new IllegalArgumentException("criteria is null");
        }
        ParseUtils.requireNoAttributes(reader);
        ParseUtils.requireNoContent(reader);
        return new SimpleCriteriaElement(type, criteria);
    }

    /** Prevent instantiation */
    private InterfaceParsingUtils() {}
}
