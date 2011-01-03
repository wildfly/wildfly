/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3;

import org.jboss.as.ExtensionContext;
import org.jboss.as.model.ParseResult;
import org.jboss.as.model.ParseUtils;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamException;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class EJB3SubsystemParser implements XMLElementReader<ParseResult<ExtensionContext.SubsystemConfiguration<EJB3SubsystemElement>>> {
   protected static final String NAMESPACE = "urn:jboss:domain:ejb3:1.0";

   private static final EJB3SubsystemParser instance = new EJB3SubsystemParser();

   public static EJB3SubsystemParser getInstance() {
      return instance;
   }

   @Override
   public void readElement(XMLExtendedStreamReader reader, ParseResult<ExtensionContext.SubsystemConfiguration<EJB3SubsystemElement>> result)
           throws XMLStreamException {
      // parse <jboss-ejb3> domain element

      // TODO: do something real
      ParseUtils.requireNoAttributes(reader);
      ParseUtils.requireNoContent(reader);
      result.setResult(new ExtensionContext.SubsystemConfiguration<EJB3SubsystemElement>(new EJB3SubsystemAdd(NAMESPACE)));
   }
}
