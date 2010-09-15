package org.jboss.as.messaging;

import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.deployers.impl.FileConfigurationParser;
import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stax.StAXSource;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA. User: starksm Date: Sep 13, 2010 Time: 11:56:27 PM To change this template use File |
 * Settings | File Templates.
 */
public class ConfigurationElement extends AbstractModelElement<ConfigurationElement> implements ServiceActivator {
   private static final long serialVersionUID = 1L;
   Configuration config = new ConfigurationImpl();

   public ConfigurationElement(final Location location) {
      super(location);
   }

   public ConfigurationElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
      super(reader);
      try {
         XmlElementReader ereader = new XmlElementReader(reader);
         DOMResult result = new DOMResult();
         TransformerFactory transFactory = TransformerFactory.newInstance();
         Transformer transformer = transFactory.newTransformer();
         transformer.transform(new StAXSource(ereader), result);
         Document document = (Document) result.getNode();
         org.w3c.dom.Element docElement = document.getDocumentElement();
         NodeList nodes = docElement.getElementsByTagNameNS("urn:hornetq", "configuration");
         org.w3c.dom.Element configElement = (org.w3c.dom.Element) nodes.item(0);
         FileConfigurationParser jmsParser = new FileConfigurationParser();
         jmsParser.parseMainConfig(configElement, config);
      }
      catch (Exception e) {
         throw new XMLStreamException("Failed to transform stax stream into document", e);
      }
   }

   public Configuration getConfiguration() {
      return config;
   }

   @Override
   public long elementHash() {
      return 0;  //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   protected void appendDifference(Collection<AbstractModelUpdate<ConfigurationElement>> target, ConfigurationElement other) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   protected Class<ConfigurationElement> getElementClass() {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   public void activate(ServiceActivatorContext serviceActivatorContext) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   private class XmlElementReader implements XMLStreamReader {
      XMLExtendedStreamReader reader;
      boolean sawEnd = false;

      XmlElementReader(XMLExtendedStreamReader reader) {
         this.reader = reader;
      }

      @Override
      public Object getProperty(String name) throws IllegalArgumentException {
         return reader.getProperty(name);
      }

      @Override
      public int next() throws XMLStreamException {
         int next = -1;
         if (sawEnd == false) {
            next = reader.next();
            if (next == XMLStreamConstants.START_ELEMENT)
               System.out.println("start, "+reader.getLocalName());
            if (next == XMLStreamConstants.END_ELEMENT) {
               System.out.println("end, "+reader.getLocalName());
               if (reader.getLocalName().equals("subsystem"))
                  sawEnd = true;
               return next;
            }
         }
         return next;
      }

      @Override
      public void require(int type, String namespaceURI, String localName) throws XMLStreamException {
         reader.require(type, namespaceURI, localName);
      }

      @Override
      public String getElementText() throws XMLStreamException {
         return reader.getElementText();
      }

      @Override
      public int nextTag() throws XMLStreamException {
         return reader.nextTag();
      }

      @Override
      public boolean hasNext() throws XMLStreamException {
         return reader.hasNext();
      }

      @Override
      public void close() throws XMLStreamException {
         reader.close();
      }

      @Override
      public String getNamespaceURI(String prefix) {
         return reader.getNamespaceURI(prefix);
      }

      @Override
      public boolean isStartElement() {
         return reader.isStartElement();
      }

      @Override
      public boolean isEndElement() {
         return reader.isEndElement();
      }

      @Override
      public boolean isCharacters() {
         return reader.isCharacters();
      }

      @Override
      public boolean isWhiteSpace() {
         return reader.isWhiteSpace();
      }

      @Override
      public String getAttributeValue(String namespaceURI, String localName) {
         return reader.getAttributeValue(namespaceURI, localName);
      }

      @Override
      public int getAttributeCount() {
         return reader.getAttributeCount();
      }

      @Override
      public QName getAttributeName(int index) {
         return reader.getAttributeName(index);
      }

      @Override
      public String getAttributeNamespace(int index) {
         return reader.getAttributeNamespace(index);
      }

      @Override
      public String getAttributeLocalName(int index) {
         return reader.getAttributeLocalName(index);
      }

      @Override
      public String getAttributePrefix(int index) {
         return reader.getAttributePrefix(index);
      }

      @Override
      public String getAttributeType(int index) {
         return reader.getAttributeType(index);
      }

      @Override
      public String getAttributeValue(int index) {
         return reader.getAttributeValue(index);
      }

      @Override
      public boolean isAttributeSpecified(int index) {
         return reader.isAttributeSpecified(index);
      }

      @Override
      public int getNamespaceCount() {
         return reader.getNamespaceCount();
      }

      @Override
      public String getNamespacePrefix(int index) {
         return reader.getNamespacePrefix(index);
      }

      @Override
      public String getNamespaceURI(int index) {
         return reader.getNamespaceURI(index);
      }

      @Override
      public NamespaceContext getNamespaceContext() {
         return reader.getNamespaceContext();
      }

      @Override
      public int getEventType() {
         return reader.getEventType();
      }

      @Override
      public String getText() {
         return reader.getText();
      }

      @Override
      public char[] getTextCharacters() {
         return reader.getTextCharacters();
      }

      @Override
      public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException {
         return reader.getTextCharacters(sourceStart, target, targetStart, length);
      }

      @Override
      public int getTextStart() {
         return reader.getTextStart();
      }

      @Override
      public int getTextLength() {
         return reader.getTextLength();
      }

      @Override
      public String getEncoding() {
         return reader.getEncoding();
      }

      @Override
      public boolean hasText() {
         return reader.hasText();
      }

      @Override
      public javax.xml.stream.Location getLocation() {
         return reader.getLocation();
      }

      @Override
      public QName getName() {
         return reader.getName();
      }

      @Override
      public String getLocalName() {
         return reader.getLocalName();
      }

      @Override
      public boolean hasName() {
         return reader.hasName();
      }

      @Override
      public String getNamespaceURI() {
         return reader.getNamespaceURI();
      }

      @Override
      public String getPrefix() {
         return reader.getPrefix();
      }

      @Override
      public String getVersion() {
         return reader.getVersion();
      }

      @Override
      public boolean isStandalone() {
         return reader.isStandalone();
      }

      @Override
      public boolean standaloneSet() {
         return reader.standaloneSet();
      }

      @Override
      public String getCharacterEncodingScheme() {
         return reader.getCharacterEncodingScheme();
      }

      @Override
      public String getPITarget() {
         return reader.getPITarget();
      }

      @Override
      public String getPIData() {
         return reader.getPIData();
      }
   }
}
