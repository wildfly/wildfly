/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.xml;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLMapper;
import org.junit.Assert;
import org.mockito.Mockito;

/**
 * A simple {@link XMLExtendedStreamReader} that replays a fixed set of simplified StAX events.
 */
public class SimpleXMLExtendedStreamReader implements XMLExtendedStreamReader, AutoCloseable {
    private final Iterator<XMLStreamEvent> events;
    private XMLStreamEvent currentEvent;

    public SimpleXMLExtendedStreamReader(List<XMLStreamEvent> events) {
        this.events = events.iterator();
        this.currentEvent = this.events.next();
    }

    @Override
    public Object getProperty(String name) {
        return null;
    }

    @Override
    public int next() throws XMLStreamException {
        this.currentEvent = this.events.next();
        return this.currentEvent.getType();
    }

    @Override
    public void require(int type, String namespaceURI, String localName) throws XMLStreamException {
        Assert.assertEquals(type, this.currentEvent.getType());
    }

    @Override
    public boolean hasNext() throws XMLStreamException {
        return this.events.hasNext();
    }

    @Override
    public int nextTag() throws XMLStreamException {
        int result = this.next();
        while ((result != XMLStreamConstants.START_ELEMENT) && (result != XMLStreamConstants.END_ELEMENT)) {
            if ((result != XMLStreamConstants.COMMENT) || (result != XMLStreamConstants.PROCESSING_INSTRUCTION)) {
                throw new IllegalStateException();
            }
            result = this.next();
        }
        return result;
    }

    @Override
    public int getEventType() {
        return this.currentEvent.getType();
    }

    @Override
    public QName getName() {
        return this.currentEvent.getName();
    }

    @Override
    public QName getAttributeName(int index) {
        return new QName(this.currentEvent.getAttributes().get(index).getKey());
    }

    @Override
    public String getAttributeValue(int index) {
        return this.currentEvent.getAttributes().get(index).getValue();
    }

    @Override
    public String getText() {
        return this.currentEvent.getText();
    }

    @Override
    public String getElementText() throws XMLStreamException {
        if (this.getEventType() != XMLStreamConstants.START_ELEMENT) {
            throw new IllegalStateException();
        }
        int next = this.next();
        StringBuilder builder = new StringBuilder();
        while (next != XMLStreamConstants.END_ELEMENT) {
            switch (next) {
                case CHARACTERS:
                case CDATA:
                case ENTITY_REFERENCE:
                case SPACE:
                    builder.append(this.getText());
                    break;
                case COMMENT:
                    break;
                default:
                    throw new IllegalStateException();
            }
            next = this.next();
        }
        return builder.toString();
    }

    @Override
    public void close() {
        // On close, assert that we have processed all events
        Assert.assertFalse(this.events.hasNext());
    }

    @Override
    public boolean isStartElement() {
        return this.currentEvent.getType() == XMLStreamConstants.START_ELEMENT;
    }

    @Override
    public boolean isEndElement() {
        return this.currentEvent.getType() == XMLStreamConstants.END_ELEMENT;
    }

    @Override
    public boolean isCharacters() {
        return this.currentEvent.getType() == XMLStreamConstants.CHARACTERS;
    }

    @Override
    public boolean isWhiteSpace() {
        return this.currentEvent.getType() == XMLStreamConstants.SPACE;
    }

    @Override
    public String getAttributeValue(String namespaceURI, String localName) {
        for (Map.Entry<String, String> entry : this.currentEvent.getAttributes()) {
            if (entry.getKey().equals(localName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public int getAttributeCount() {
        return this.currentEvent.getAttributes().size();
    }

    @Override
    public String getAttributeNamespace(int index) {
        return this.getAttributeName(index).getNamespaceURI();
    }

    @Override
    public String getAttributeLocalName(int index) {
        return this.getAttributeName(index).getLocalPart();
    }

    @Override
    public String getAttributePrefix(int index) {
        return this.getAttributeName(index).getPrefix();
    }

    @Override
    public String getAttributeType(int index) {
        return "xs:string";
    }

    @Override
    public boolean isAttributeSpecified(int index) {
        return false;
    }

    @Override
    public int getNamespaceCount() {
        return 0;
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return XMLConstants.NULL_NS_URI;
    }

    @Override
    public String getNamespacePrefix(int index) {
        return null;
    }

    @Override
    public String getNamespaceURI(int index) {
        return null;
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return null;
    }

    @Override
    public char[] getTextCharacters() {
        return this.currentEvent.getText().toCharArray();
    }

    @Override
    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException {
        String text = this.currentEvent.getText();
        int result = 0;
        for (int sourceIndex = sourceStart, targetIndex = targetStart; (sourceIndex < text.length()) && (targetIndex < target.length); ++sourceIndex, ++targetIndex) {
            target[targetIndex] = text.charAt(sourceIndex);
            result += 1;
            if (result == length) return length;
        }
        return result;
    }

    @Override
    public int getTextStart() {
        return 0;
    }

    @Override
    public int getTextLength() {
        return this.currentEvent.getText().length();
    }

    @Override
    public String getEncoding() {
        return null;
    }

    @Override
    public boolean hasText() {
        int event = this.currentEvent.getType();
        return event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA || event == XMLStreamConstants.ENTITY_REFERENCE || event == XMLStreamConstants.COMMENT || event == XMLStreamConstants.SPACE;
    }

    @Override
    public Location getLocation() {
        return Mockito.mock(Location.class);
    }

    @Override
    public String getLocalName() {
        return this.getName().getLocalPart();
    }

    @Override
    public boolean hasName() {
        return this.isStartElement() || this.isEndElement();
    }

    @Override
    public String getNamespaceURI() {
        return this.getName().getNamespaceURI();
    }

    @Override
    public String getPrefix() {
        return XMLConstants.DEFAULT_NS_PREFIX;
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public boolean isStandalone() {
        return false;
    }

    @Override
    public boolean standaloneSet() {
        return false;
    }

    @Override
    public String getCharacterEncodingScheme() {
        return null;
    }

    @Override
    public String getPITarget() {
        return null;
    }

    @Override
    public String getPIData() {
        return null;
    }

    @Override
    public void handleAny(Object value) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void handleAttribute(Object value, int index) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void discardRemainder() throws XMLStreamException {
        while (this.events.hasNext()) {
            this.currentEvent = this.events.next();
        }
    }

    @Override
    public int getIntAttributeValue(int index) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int[] getIntListAttributeValue(int index) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getListAttributeValue(int index) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLongAttributeValue(int index) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long[] getLongListAttributeValue(int index) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getAttributeValue(int index, Class<T> kind) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> List<? extends T> getListAttributeValue(int index, Class<T> kind) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getId() throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    @Override
    public XMLMapper getXMLMapper() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTrimElementText(boolean trim) {
        throw new UnsupportedOperationException();
    }
}
