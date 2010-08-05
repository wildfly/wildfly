/**
 * 
 */
package org.jboss.as.model;

import javax.xml.stream.XMLStreamException;

import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A ForeignNamespaceAttribute.
 * 
 * @author Brian Stansberry
 */
public class ParsedAttribute {

    private final String namespace;
    private final String prefix;
    private final String localPart;
    private final String value;
    
    public ParsedAttribute(String namespace, String prefix, String localPart, String value) {
        this.namespace = namespace;
        this.prefix = prefix;
        this.localPart = localPart;
        this.value = value;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getLocalPart() {
        return localPart;
    }

    public String getValue() {
        return value;
    }
    
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        if (prefix == null && namespace == null) {
            streamWriter.writeAttribute(localPart, value);
        }
        else if (prefix == null) {
            streamWriter.writeAttribute(namespace, localPart, value);
        }
        else {
            streamWriter.writeAttribute(prefix, namespace, localPart, value);
        }
    }
}
