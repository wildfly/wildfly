/**
 *
 */
package org.jboss.as.model.socket;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.Element;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parsing utility methods.
 *
 * @author Brian Stansberry
 */
class ParsingUtil {


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
     * of a type included in {@link ParsingUtil#SIMPLE_CRITERIA}.
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
                result = new SimpleCriteriaElement(reader, element, LinkLocalInterfaceCriteria.INSTANCE);
                break;
            }
            case LOOPBACK: {
                result = new SimpleCriteriaElement(reader, element, LoopbackInterfaceCriteria.INSTANCE);
                break;
            }
            case MULTICAST: {
                result = new SimpleCriteriaElement(reader, element, SupportsMulticastInterfaceCriteria.INSTANCE);
                break;
            }
            case POINT_TO_POINT: {
                result = new SimpleCriteriaElement(reader, element, PointToPointInterfaceCriteria.INSTANCE);
                break;
            }
            case PUBLIC_ADDRESS: {
                result = new SimpleCriteriaElement(reader, element, PublicAddressInterfaceCriteria.INSTANCE);
                break;
            }
            case SITE_LOCAL_ADDRESS: {
                result = new SimpleCriteriaElement(reader, element, SiteLocalInterfaceCriteria.INSTANCE);
                break;
            }
            case UP: {
                result = new SimpleCriteriaElement(reader, element, UpInterfaceCriteria.INSTANCE);
                break;
            }
            case VIRTUAL: {
                result = new SimpleCriteriaElement(reader, element, VirtualInterfaceCriteria.INSTANCE);
                break;
            }
            case NIC: {
                result = new NicCriteriaElement(reader);
                break;
            }
            case NIC_MATCH: {
                result = new NicMatchCriteriaElement(reader);
                break;
            }
            case SUBNET_MATCH: {
                result = new SubnetMatchCriteriaElement(reader);
                break;
            }
            default: throw new XMLStreamException("Unexpected element '" + reader.getName() + "' encountered", reader.getLocation());
        }

        return result;
    }

    /** Returns an XMLStreamException stating that at least one of <code>criteria</code>  must be supplied */
    public static XMLStreamException missingCriteria(XMLExtendedStreamReader reader, String criteria) {
        return new XMLStreamException("At least one of the following elements must be supplied: " + criteria, reader.getLocation());
    }

    /** Prevent instantiation */
    private ParsingUtil() {}
}
