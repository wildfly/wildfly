package org.jboss.as.modcluster.test;

import java.util.ArrayDeque;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.staxmapper.XMLExtendedStreamWriter;

public class FormattingXMLStreamWriter implements XMLExtendedStreamWriter {

    private XMLStreamWriter delegate;
    private ArrayDeque<String> unspecifiedNamespaces = new ArrayDeque<String>();
    private static final String NO_NAMESPACE = new String();

    public FormattingXMLStreamWriter(XMLStreamWriter createXMLStreamWriter) {
        delegate = createXMLStreamWriter;
        unspecifiedNamespaces.push(NO_NAMESPACE);
    }

    @Override
    public void close() throws XMLStreamException {
        delegate.close();
    }

    @Override
    public void flush() throws XMLStreamException {
        delegate.flush();
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return delegate.getNamespaceContext();
    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        return delegate.getPrefix(uri);
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        return delegate.getProperty(name);
    }

    @Override
    public void setDefaultNamespace(String name) throws XMLStreamException {
        delegate.setDefaultNamespace(name);
    }

    @Override
    public void setNamespaceContext(NamespaceContext name) throws XMLStreamException {
        delegate.setNamespaceContext(name);
    }

    @Override
    public void setPrefix(String arg0, String arg1) throws XMLStreamException {
        delegate.setPrefix(arg0, arg1);
    }

    @Override
    public void writeAttribute(String arg0, String arg1) throws XMLStreamException {
        delegate.writeAttribute(arg0, arg1);
    }

    @Override
    public void writeAttribute(String arg0, String arg1, String arg2) throws XMLStreamException {
        delegate.writeAttribute(arg0, arg1, arg2);
    }

    @Override
    public void writeAttribute(String arg0, String arg1, String arg2, String arg3) throws XMLStreamException {
        delegate.writeAttribute(arg0, arg1, arg2, arg3);
    }

    @Override
    public void writeCData(String arg0) throws XMLStreamException {
        delegate.writeCData(arg0);
    }

    @Override
    public void writeCharacters(String arg0) throws XMLStreamException {
        delegate.writeCharacters(arg0);
    }

    @Override
    public void writeCharacters(char[] arg0, int arg1, int arg2) throws XMLStreamException {
        delegate.writeCharacters(arg0, arg1, arg2);
    }

    @Override
    public void writeComment(String arg0) throws XMLStreamException {
        delegate.writeComment(arg0);
    }

    @Override
    public void writeDTD(String arg0) throws XMLStreamException {
        delegate.writeDTD(arg0);
    }

    @Override
    public void writeDefaultNamespace(String arg0) throws XMLStreamException {
       delegate.writeDefaultNamespace(arg0);

    }

    @Override
    public void writeEmptyElement(String arg0) throws XMLStreamException {
        delegate.writeEmptyElement(arg0);
    }

    @Override
    public void writeEmptyElement(String arg0, String arg1) throws XMLStreamException {
        delegate.writeEmptyElement(arg0, arg1);
     }

    @Override
    public void writeEmptyElement(String arg0, String arg1, String arg2) throws XMLStreamException {
        delegate.writeEmptyElement(arg0, arg1, arg2);
    }

    @Override
    public void writeEndDocument() throws XMLStreamException {
        delegate.writeEndDocument();
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        delegate.writeEndElement();
    }

    @Override
    public void writeEntityRef(String arg0) throws XMLStreamException {
        delegate.writeEntityRef(arg0);
    }

    @Override
    public void writeNamespace(String arg0, String arg1) throws XMLStreamException {
        delegate.writeNamespace(arg0, arg1);
    }

    @Override
    public void writeProcessingInstruction(String arg0) throws XMLStreamException {
        delegate.writeProcessingInstruction(arg0);
    }

    @Override
    public void writeProcessingInstruction(String arg0, String arg1) throws XMLStreamException {
        delegate.writeProcessingInstruction(arg0, arg1);
    }

    @Override
    public void writeStartDocument() throws XMLStreamException {
        delegate.writeStartDocument();
    }

    @Override
    public void writeStartDocument(String arg0) throws XMLStreamException {
        delegate.writeStartDocument(arg0);
    }

    @Override
    public void writeStartDocument(String arg0, String arg1) throws XMLStreamException {
        delegate.writeStartDocument(arg0, arg1);
    }

    @Override
    public void writeStartElement(String arg0) throws XMLStreamException {
        delegate.writeStartElement(arg0);
    }

    @Override
    public void writeStartElement(String arg0, String arg1) throws XMLStreamException {
        delegate.writeStartElement(arg0, arg1);
    }

    @Override
    public void writeStartElement(String arg0, String arg1, String arg2) throws XMLStreamException {
        delegate.writeStartElement(arg0, arg1, arg2);
    }

    @Override
    public void setUnspecifiedElementNamespace(String namespace) {
        ArrayDeque<String> namespaces = this.unspecifiedNamespaces;
        namespaces.pop();
        namespaces.push(namespace == null ? NO_NAMESPACE : namespace);
    }

    @Override
    public void writeAttribute(String arg0, String[] arg1) throws XMLStreamException {
        delegate.writeAttribute(arg0, join(arg1));
    }

    private String join(String[] values) {
        final StringBuilder b = new StringBuilder();
        for (int i = 0, valuesLength = values.length; i < valuesLength; i++) {
            final String s = values[i];
            if (s != null) {
                if (i > 0) {
                    b.append(' ');
                }
                b.append(s);
            }
        }
        return b.toString();
    }

    @Override
    public void writeAttribute(String arg0, Iterable<String> arg1) throws XMLStreamException {
        delegate.writeAttribute(arg0, join(arg1));
    }

    private String join(Iterable<String> values) {
        final StringBuilder b = new StringBuilder();
        Iterator<String> iterator = values.iterator();
        while (iterator.hasNext()) {
            final String s = iterator.next();
            if (s != null) {
                b.append(s);
                if (iterator.hasNext()) b.append(' ');
            }
        }
        return b.toString();
    }

    @Override
    public void writeAttribute(String arg0, String arg1, String[] arg2) throws XMLStreamException {
        delegate.writeAttribute(arg0, arg1,join(arg2));
    }

    @Override
    public void writeAttribute(String arg0, String arg1, Iterable<String> arg2) throws XMLStreamException {
        delegate.writeAttribute(arg0, arg1,join(arg2));
    }

    @Override
    public void writeAttribute(String arg0, String arg1, String arg2, String[] arg3) throws XMLStreamException {
        delegate.writeAttribute(arg0, arg1,arg2, join(arg3));
    }

    @Override
    public void writeAttribute(String arg0, String arg1, String arg2, Iterable<String> arg3) throws XMLStreamException {
        delegate.writeAttribute(arg0, arg1,arg2, join(arg3));
    }

}
