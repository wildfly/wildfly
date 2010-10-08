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

    //Attributes
    private final String name;
    private String javaHome;
    private Boolean debugEnabled;
    private String debugOptions;
    private Boolean envClasspathIgnored;

    //Elements
    private String heapSize;
    private String maxHeap;
    private String permgenSize;
    private String maxPermgen;
    private String agentPath;
    private String agentLib;
    private String javaagent;
    private String stack;
    private final JvmOptionsElement jvmOptionsElement = new JvmOptionsElement();
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
                    case DEBUG_ENABLED: {
                        debugEnabled = Boolean.valueOf(value);
                        break;
                    }
                    case DEBUG_OPTIONS: {
                        debugOptions = value;
                        break;
                    }
                    case ENV_CLASSPATH_IGNORED: {
                        envClasspathIgnored = Boolean.valueOf(value);
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
                            if (heapSize != null || maxHeap != null) {
                                throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());
                            }
                            parseMinMax(reader, new HeapSetter());
                            break;
                        }
                        case PERMGEN: {
                            if (permgenSize != null || maxPermgen != null) {
                                throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());
                            }
                            parseMinMax(reader, new PermGenSetter());
                            break;
                        }
                        case STACK : {
                            if (stack != null) {
                                throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());
                            }
                            parseStack(reader);
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
                        case AGENT_LIB: {
                            if (agentLib != null) {
                                throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());
                            }
                            if (agentPath != null) {
                                throw new XMLStreamException(element.getLocalName() + " when we already also have a " + Element.AGENT_PATH, reader.getLocation());
                            }
                            agentLib = parseValue(reader);
                            break;
                        }
                        case AGENT_PATH: {
                            if (agentPath != null) {
                                throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());
                            }
                            if (agentLib != null) {
                                throw new XMLStreamException(element.getLocalName() + " when we already also have a " + Element.AGENT_LIB, reader.getLocation());
                            }
                            agentPath = parseValue(reader);
                            break;
                        }
                        case JAVA_AGENT: {
                            javaagent = parseValue(reader);
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
            if (element.getDebugOptions() != null) {
                this.debugOptions = element.getDebugOptions();
            }
            if (element.isDebugEnabled() != null) {
                this.debugEnabled = element.isDebugEnabled();
            }
            if (element.isEnvClasspathIgnored() != null) {
                this.envClasspathIgnored = element.isEnvClasspathIgnored();
            }
            if (element.getPermgenSize() != null) {
                this.permgenSize = element.getPermgenSize();
            }
            if (element.getMaxPermgen() != null) {
                this.maxPermgen = element.getMaxPermgen();
            }
            if (element.getHeapSize() != null) {
                this.heapSize = element.getHeapSize();
            }
            if (element.getMaxHeap() != null) {
                this.maxHeap = element.getMaxHeap();
            }
            if (element.getStack() != null) {
                this.stack = element.getStack();
            }
            if (element.getAgentLib() != null) {
                this.agentLib = element.getAgentLib();
            }
            if (element.getAgentPath() != null) {
                this.agentPath = element.getAgentPath();
            }
            if (element.getJavaagent() != null) {
                this.javaagent = element.getJavaagent();
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

    public String getPermgenSize() {
        return permgenSize;
    }

    public void setPermgenSize(String permgenSize) {
        this.permgenSize = permgenSize;
    }

    public String getMaxPermgen() {
        return maxPermgen;
    }

    public void setMaxPermgen(String maxPermgen) {
        this.maxPermgen = maxPermgen;
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

    public Boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(Boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public String getDebugOptions() {
        return debugOptions;
    }

    public void setDebugOptions(String debugOptions) {
        this.debugOptions = debugOptions;
    }

    public String getStack() {
        return stack;
    }

    public void setStack(String stack) {
        this.stack = stack;
    }

    public Boolean isEnvClasspathIgnored() {
        return envClasspathIgnored;
    }

    public void setEnvClasspathIgnored(Boolean envClasspathIgnored) {
        this.envClasspathIgnored = envClasspathIgnored;
    }

    public JvmOptionsElement getJvmOptions() {
        return jvmOptionsElement;
    }

    public PropertiesElement getEnvironmentVariables() {
        return environmentVariables;
    }

    public PropertiesElement getSystemProperties() {
        return systemProperties;
    }

    public String getAgentPath() {
        return agentPath;
    }

    public void setAgentPath(String agentPath) {
        if (agentLib != null) {
            throw new IllegalArgumentException("Attempting to set 'agent-path' when 'agent-lib' was already set");
        }
        this.agentPath = agentPath;
    }

    public String getAgentLib() {
        return agentLib;
    }

    public void setAgentLib(String agentLib) {
        if (agentPath != null) {
            throw new IllegalArgumentException("Attempting to set 'agent-lib' when 'agent-path' was already set");
        }
        this.agentLib = agentLib;
    }

    public String getJavaagent() {
        return javaagent;
    }

    public void setJavaagent(String javaagent) {
        this.javaagent = javaagent;
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

        if (debugEnabled != null) {
            streamWriter.writeAttribute(Attribute.DEBUG_ENABLED.getLocalName(), debugEnabled.toString());
        }

        if (debugOptions != null) {
            streamWriter.writeAttribute(Attribute.DEBUG_OPTIONS.getLocalName(), debugOptions.toString());
        }

        if (envClasspathIgnored != null) {
            streamWriter.writeAttribute(Attribute.ENV_CLASSPATH_IGNORED.getLocalName(), envClasspathIgnored.toString());
        }

        if (heapSize != null || maxHeap != null) {
            streamWriter.writeStartElement(Element.HEAP.getLocalName());
            if (heapSize != null)
                streamWriter.writeAttribute(Attribute.SIZE.getLocalName(), heapSize);
            if (maxHeap != null)
                streamWriter.writeAttribute(Attribute.MAX_SIZE.getLocalName(), maxHeap);
            streamWriter.writeEndElement();
        }

        if (permgenSize != null || maxPermgen != null) {
            streamWriter.writeStartElement(Element.PERMGEN.getLocalName());
            if (permgenSize != null)
                streamWriter.writeAttribute(Attribute.SIZE.getLocalName(), permgenSize);
            if (maxPermgen != null)
                streamWriter.writeAttribute(Attribute.MAX_SIZE.getLocalName(), permgenSize);
            streamWriter.writeEndElement();
        }

        if (stack != null) {
            streamWriter.writeStartElement(Element.STACK.getLocalName());
            streamWriter.writeAttribute(Attribute.SIZE.getLocalName(), stack);
            streamWriter.writeEndElement();
        }

        if (agentLib != null) {
            streamWriter.writeStartElement(Element.AGENT_LIB.getLocalName());
            streamWriter.writeAttribute(Attribute.VALUE.getLocalName(), agentLib);
            streamWriter.writeEndElement();
        }

        if (agentPath != null) {
            streamWriter.writeStartElement(Element.AGENT_PATH.getLocalName());
            streamWriter.writeAttribute(Attribute.VALUE.getLocalName(), agentPath);
            streamWriter.writeEndElement();
        }

        if (javaagent != null) {
            streamWriter.writeStartElement(Element.JAVA_AGENT.getLocalName());
            streamWriter.writeAttribute(Attribute.VALUE.getLocalName(), javaagent);
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

    private void parseMinMax(XMLExtendedStreamReader reader, MinMaxSetter setter) throws XMLStreamException {
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

        setter.setMinMax(size, max);
        // Handle elements
        ParseUtils.requireNoContent(reader);
    }

    private void parseStack(XMLExtendedStreamReader reader) throws XMLStreamException {
        // Handle attributes
        String size = null;
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
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (size == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.SIZE));
        }
        this.stack = size;

        // Handle elements
        ParseUtils.requireNoContent(reader);
    }

    private String parseValue(XMLExtendedStreamReader reader) throws XMLStreamException {
        String found = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case SIZE: {
                        found = value;
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (found == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.VALUE));
        }

        // Handle elements
        ParseUtils.requireNoContent(reader);

        return found;
    }

    private interface MinMaxSetter {
        void setMinMax(String min, String max);
    }

    private class HeapSetter implements MinMaxSetter {

        @Override
        public void setMinMax(String min, String max) {
            heapSize = min;
            maxHeap = max;
        }
    }

    private class PermGenSetter implements MinMaxSetter {

        @Override
        public void setMinMax(String min, String max) {
            permgenSize = min;
            maxHeap = max;
        }
    }

}
