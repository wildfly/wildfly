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

package org.jboss.as.messaging;

import org.jboss.as.Extension;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.staxmapper.XMLMapper;

import javax.xml.namespace.QName;

/**
 * The implementation of the Messaging extension.
 *
 * @author scott.stark@jboss.org
 * @version $Id$
 */
public final class MessagingExtension implements Extension {
   private static final Logger log = Logger.getLogger("org.jboss.as.messaging");

   /**
    * {@inheritDoc}
    */
   public void registerElementHandlers(final XMLMapper mapper) {
      QName messagingNamespace = new QName(Namespace.MESSAGING_1_0.getUriString(), Element.SUBSYSTEM.getLocalName());
      mapper.registerRootElement(messagingNamespace, MessagingSubsystemParser.getInstance());
      log.infof("Registered messaging subsystem under: %s", messagingNamespace);
   }

   /**
    * {@inheritDoc}
    */
   public void activate(final ServiceActivatorContext context) {
      // no actions needed
   }
}
