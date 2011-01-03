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

import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;
import java.util.List;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class EJB3SubsystemElement extends AbstractSubsystemElement<EJB3SubsystemElement> {
   EJB3SubsystemElement() {
      super(EJB3SubsystemParser.NAMESPACE);
   }

   @Override
   protected void getUpdates(List<? super AbstractSubsystemUpdate<EJB3SubsystemElement, ?>> objects) {
      throw new RuntimeException("NYI: org.jboss.as.ejb3.EJB3SubsystemElement.getUpdates");
   }

   @Override
   protected boolean isEmpty() {
      throw new RuntimeException("NYI: org.jboss.as.ejb3.EJB3SubsystemElement.isEmpty");
   }

   @Override
   protected AbstractSubsystemAdd<EJB3SubsystemElement> getAdd() {
      throw new RuntimeException("NYI: org.jboss.as.ejb3.EJB3SubsystemElement.getAdd");
   }

   @Override
   protected <P> void applyRemove(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
      throw new RuntimeException("NYI: org.jboss.as.ejb3.EJB3SubsystemElement.applyRemove");
   }

   @Override
   protected Class<EJB3SubsystemElement> getElementClass() {
      return EJB3SubsystemElement.class;
   }

   @Override
   public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
      // TODO: really write out some stuff
      streamWriter.writeEndElement();
   }
}
