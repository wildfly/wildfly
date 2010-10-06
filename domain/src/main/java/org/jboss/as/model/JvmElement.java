/**
 *
 */
package org.jboss.as.model;

import java.util.Collections;

import javax.xml.stream.XMLStreamException;

import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A Java Virtual Machine configuration.
 *
 * @author Brian Stansberry
 */
public class JvmElement extends AbstractModelElement<JvmElement> {

    private static final long serialVersionUID = 4963103173530602991L;

    private final String name;
    private String javaHome;
    private String heapSize;
    private String maxHeap;
    private PropertiesElement environmentVariables = new PropertiesElement(Element.VARIABLE, true);
    private PropertiesElement systemProperties = new PropertiesElement(Element.PROPERTY, true);

    /**
     */
    public JvmElement(final String name) {
        this.name = name;
    }

    /**
     * @param reader
     * @throws XMLStreamException
     */
    public JvmElement(XMLExtendedStreamReader reader) throws XMLStreamException {
        // Handle attributes
        String name = null;
        String home = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = value;
                        break;
                    }
                    case JAVA_HOME: {
                        home = value;
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (name == null) {
            // FIXME and fix xsd. A name is only required at domain and host
            // level (i.e. when wrapped in <jvms/>). At server-group and server
            // levels it can be unnamed, in which case configuration from
            // domain and host levels aren't mixed in. OR make name required in xsd always
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        this.name = name;
        this.javaHome = home;
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case HEAP: {
                            parseHeap(reader);
                            break;
                        }
                        case ENVIRONMENT_VARIABLES: {
                            if (environmentVariables != null) {
                                throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());
                            }
                            this.environmentVariables = new PropertiesElement(reader, Element.VARIABLE, true);
                            break;
                        }
                        case SYSTEM_PROPERTIES: {
                            if (systemProperties != null) {
                                throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());
                            }
                            this.systemProperties = new PropertiesElement(reader);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    public JvmElement(JvmElement ... toCombine) {
        // FIXME -- hack Location
        super();

        this.name = toCombine[0].getName();

        for (JvmElement element : toCombine) {
            if (! this.name.equals(element.getName())) {
                throw new IllegalArgumentException("Jvm " + element.getName() + " has a different name from the other jvm elements; all must have the same name");
            }
            if (element.getJavaHome() != null) {
                this.javaHome = element.getJavaHome();
            }
            if (element.getHeapSize() != null) {
                this.heapSize = element.getHeapSize();
            }
            if (element.getMaxHeap() != null) {
                this.maxHeap = element.getMaxHeap();
            }
        }

        PropertiesElement[] combinedEnv = new PropertiesElement[toCombine.length];
        for (int i = 0; i < toCombine.length; i++) {
            combinedEnv[i] = toCombine[i].getEnvironmentVariables();
        }
        this.environmentVariables = new PropertiesElement(Element.ENVIRONMENT_VARIABLES, true, combinedEnv);

        PropertiesElement[] combinedSysp = new PropertiesElement[toCombine.length];
        for (int i = 0; i < toCombine.length; i++) {
            combinedSysp[i] = toCombine[i].getSystemProperties();
        }
        this.systemProperties = new PropertiesElement(Element.SYSTEM_PROPERTIES, true, combinedSysp);
    }



    public String getJavaHome() {
        return javaHome;
    }

    void setJavaHome(String javaHome) {
        this.javaHome = javaHome;
    }

    public String getHeapSize() {
        return heapSize;
    }

    void setHeapSize(String heapSize) {
        this.heapSize = heapSize;
    }

    public String getMaxHeap() {
        return maxHeap;
    }

    void setMaxHeap(String maxHeap) {
        this.maxHeap = maxHeap;
    }

    public String getName() {
        return name;
    }

    public PropertiesElement getEnvironmentVariables() {
        return environmentVariables;
    }

    public PropertiesElement getSystemProperties() {
        return systemProperties;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.AbstractModelElement#getElementClass()
     */
    @Override
    protected Class<JvmElement> getElementClass() {
        return JvmElement.class;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.model.AbstractModelElement#writeContent(org.jboss.staxmapper.XMLExtendedStreamWriter)
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);

        if (javaHome != null) {
            streamWriter.writeAttribute(Attribute.JAVA_HOME.getLocalName(), javaHome);
        }

        if (heapSize != null || maxHeap != null) {
            streamWriter.writeStartElement(Element.HEAP.getLocalName());
            if (heapSize != null)
                streamWriter.writeAttribute(Attribute.SIZE.getLocalName(), heapSize);
            if (maxHeap != null)
                streamWriter.writeAttribute(Attribute.SIZE.getLocalName(), maxHeap);
            streamWriter.writeEndElement();
        }

        if (environmentVariables != null && environmentVariables.size() > 0) {
            streamWriter.writeStartElement(Element.ENVIRONMENT_VARIABLES.getLocalName());
            environmentVariables.writeContent(streamWriter);
        }

        if (systemProperties != null && systemProperties.size() > 0) {
            streamWriter.writeStartElement(Element.SYSTEM_PROPERTIES.getLocalName());
            systemProperties.writeContent(streamWriter);
        }
    }

    private void parseHeap(XMLExtendedStreamReader reader) throws XMLStreamException {
        // Handle attributes
        String size = null;
        String max = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case SIZE: {
                        size = value;
                        break;
                    }
                    case MAX_SIZE: {
                        max = value;
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        this.heapSize = size;
        this.maxHeap = max;
        // Handle elements
        ParseUtils.requireNoContent(reader);
    }
}
