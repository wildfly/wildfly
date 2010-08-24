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

package org.jboss.as.connector;

import java.util.Collection;
import java.util.EnumSet;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 *
 * A ConnectorSubsystemElement.
 *
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 *
 */
final class ConnectorSubsystemElement extends AbstractSubsystemElement<ConnectorSubsystemElement>
{

   /** The serialVersionUID */
   private static final long serialVersionUID = 6451041006443208660L;

   private ArchiveValidationElement archiveValidationElement;

   private boolean beanValidation;

   public ConnectorSubsystemElement(final Location location)
   {
      super(location, new QName("urn:jboss:domain:connector:1.0", "subsystem"));
   }

   public ConnectorSubsystemElement(final XMLExtendedStreamReader reader) throws XMLStreamException
   {
      super(reader);
      final EnumSet<Element> visited = EnumSet.noneOf(Element.class);
      while (reader.hasNext() && reader.nextTag() != END_ELEMENT)
      {
         switch (Namespace.forUri(reader.getNamespaceURI()))
         {
            case CONNECTOR_1_0 : {
               final Element element = Element.forName(reader.getLocalName());
               if (visited.contains(element))
               {
                  throw unexpectedElement(reader);
               }
               visited.add(element);
               switch (element)
               {
                  case ARCHIVE_VALIDATION : {
                     archiveValidationElement = new ArchiveValidationElement(reader);
                     break;
                  }
                  case BEAN_VALIDATION : {
                     beanValidation = elementAsBoolean(reader);
                     break;
                  }
                  default :
                     throw unexpectedElement(reader);
               }
               break;
            }
            default :
               throw unexpectedElement(reader);
         }
      }
   }

   @Override
   public void activate(ServiceActivatorContext context)
   {
      final BatchBuilder builder = context.getBatchBuilder();

      final ConnectorSubsystemConfiguration config = new ConnectorSubsystemConfiguration();

      if (this.archiveValidationElement != null)
      {
         config.setArchiveValidation(true);
         config.setArchiveValidationFailOnError(this.archiveValidationElement.isFailOnError());
         config.setArchiveValidationFailOnWarn(this.archiveValidationElement.isFailOnWarn());
      }
      config.setBeanValidation(this.beanValidation);

      final ConnectorConfigService connectorConfigService = new ConnectorConfigService(config);
      builder.addService(ConnectorServices.JBOSS_CONNECTOR, connectorConfigService);

   }

   @Override
   public Collection<String> getReferencedSocketBindings()
   {
      return null;
   }

   @Override
   public long elementHash()
   {
      return 42;
   }

   @Override
   protected void appendDifference(Collection<AbstractModelUpdate<ConnectorSubsystemElement>> target,
         ConnectorSubsystemElement other)
   {
   }

   @Override
   protected Class<ConnectorSubsystemElement> getElementClass()
   {
      return ConnectorSubsystemElement.class;
   }

   @Override
   public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException
   {
      if (this.archiveValidationElement != null)
      {
         this.archiveValidationElement.writeContent(streamWriter);
      }
      streamWriter.writeStartElement(Element.BEAN_VALIDATION.getLocalName());
      streamWriter.writeCharacters(String.valueOf(beanValidation));
      streamWriter.writeEndElement();

   }

   /**
    * convert an xml element in boolean value. Empty elements results with true (tag presence is sufficient condition)
    *
    * @param reader the StAX reader
    * @return the boolean representing element
    * @throws XMLStreamException StAX exception
    */
   private boolean elementAsBoolean(XMLExtendedStreamReader reader) throws XMLStreamException
   {
      String elementtext = reader.getElementText();
      return elementtext == null || elementtext.length() == 0 ? true : Boolean.valueOf(elementtext.trim());
   }
}
