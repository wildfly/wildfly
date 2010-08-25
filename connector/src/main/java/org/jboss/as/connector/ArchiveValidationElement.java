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

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.msc.service.Location;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 *
 * A ArchiveValidationElement.
 *
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 *
 */
final class ArchiveValidationElement extends AbstractModelElement<ArchiveValidationElement>
{

   private boolean failOnError = true;

   private boolean failOnWarn = false;

   public ArchiveValidationElement(final Location location)
   {
      super(location);
   }

   public ArchiveValidationElement(final XMLExtendedStreamReader reader) throws XMLStreamException
   {
      super(reader);
      final int count = reader.getAttributeCount();
      for (int i = 0; i < count; i++)
      {
         final String value = reader.getAttributeValue(i);
         if (reader.getAttributeNamespace(i) != null)
         {
            throw unexpectedAttribute(reader, i);
         }
         else
         {
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute)
            {
               case FAIL_ON_ERROR : {
                  failOnError = Boolean.valueOf(value);
                  break;
                  }
               case FAIL_ON_WARN : {
                  failOnWarn = Boolean.valueOf(value);
                  break;
                  }
               default :
                  throw unexpectedAttribute(reader, i);
              }
          }
      }

   }

   /** The serialVersionUID */
   private static final long serialVersionUID = 8524290005529632318L;


   @Override
   public long elementHash()
   {
      return 42;
   }

   @Override
   protected void appendDifference(Collection<AbstractModelUpdate<ArchiveValidationElement>> target,
         ArchiveValidationElement other)
   {
   }

   @Override
   protected Class<ArchiveValidationElement> getElementClass()
   {
      return ArchiveValidationElement.class;
   }

   @Override
   public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException
   {
      streamWriter.writeEmptyElement(Element.ARCHIVE_VALIDATION.getLocalName());
      streamWriter.writeAttribute(Attribute.FAIL_ON_ERROR.getLocalName(), String.valueOf(failOnError));
      streamWriter.writeAttribute(Attribute.FAIL_ON_WARN.getLocalName(), String.valueOf(failOnWarn));
   }

   /**
    * Get the failOnError.
    *
    * @return the failOnError.
    */
   public final boolean isFailOnError()
   {
      return failOnError;
   }

   /**
    * Get the failOnWarn.
    *
    * @return the failOnWarn.
    */
   public final boolean isFailOnWarn()
   {
      return failOnWarn;
   }

}
