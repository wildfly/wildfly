/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.test.domain.xml;

import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import junit.framework.TestCase;

/**
 * A XSDValidationUnitTestCase.
 *
 * @author <a href="alex@jboss.com">Alexey Loubyansky</a>
 * @version $Revision: 1.1 $
 */
public class XMLValidationUnitTestCase extends TestCase
{
   public void testHost() throws Exception
   {
      parseXml("host-example.xml");
   }

   public void testDomain() throws Exception
   {
      parseXml("jboss-domain-example.xml");
   }

   private void parseXml(String xmlName) throws ParserConfigurationException, SAXException, SAXNotRecognizedException,
         SAXNotSupportedException, IOException
   {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      if(!factory.isNamespaceAware())
         factory.setNamespaceAware(true);
      if(!factory.isValidating())
         factory.setValidating(true);
      if(!factory.isXIncludeAware())
         factory.setXIncludeAware(true);

      SAXParser parser = factory.newSAXParser();
      XMLReader reader = parser.getXMLReader();
      reader.setFeature("http://apache.org/xml/features/validation/schema", true);
      reader.setErrorHandler(new ErrorHandlerImpl());
      reader.setEntityResolver(new EntityResolver()
      {
         @Override
         public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException
         {
            if(systemId == null)
               fail("Failed to resolve schema: systemId is null");
            int lastSlash = systemId.lastIndexOf('/');
            if(lastSlash > 0)
               systemId = systemId.substring(lastSlash + 1);
            URL xsdUrl = getXsdUrl(systemId);
            return new InputSource(xsdUrl.openStream());
         }}
      );
      URL xmlUrl = getXmlUrl(xmlName);
      InputSource is = new InputSource();
      is.setByteStream(xmlUrl.openStream());
      reader.parse(is);
   }

   private URL getXmlUrl(String xmlName)
   {
      return getResourceUrl("examples/" + xmlName);
   }

   private URL getXsdUrl(String xsdName)
   {
      return getResourceUrl("schema/" + xsdName);
   }

   private URL getResourceUrl(String resourceName)
   {
      URL url = Thread.currentThread().getContextClassLoader().getResource(resourceName);
      assertNotNull(url);
      return url;
   }

   private final class ErrorHandlerImpl implements ErrorHandler
   {
      @Override
      public void error(SAXParseException e) throws SAXException
      {
         fail(formatMessage(e));
      }

      @Override
      public void fatalError(SAXParseException e) throws SAXException
      {
         fail(formatMessage(e));
      }

      @Override
      public void warning(SAXParseException e) throws SAXException
      {
         System.out.println(formatMessage(e));
      }

      private String formatMessage(SAXParseException e)
      {
         StringBuffer sb = new StringBuffer();
         sb.append(e.getLineNumber()).append(':').append(e.getColumnNumber());
         if(e.getPublicId() != null)
            sb.append(" publicId='").append(e.getPublicId()).append('\'');
         if(e.getSystemId() != null)
            sb.append(" systemId='").append(e.getSystemId()).append('\'');
         sb.append(' ').append(e.getLocalizedMessage());
         return sb.toString();
      }
   }
}
