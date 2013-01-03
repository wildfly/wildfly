/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jaxr;

import javax.xml.registry.JAXRException;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.Messages;

/**
 * This module is using message IDs in the range 14000-14099. This file is using the subset 14080-14099 for host
 * controller logger messages. See http://community.jboss.org/docs/DOC-16810 for the full list of currently reserved
 * JBAS message id blocks.
 *
 * Date: 31.1.2012
 *
 * @author Kurt T Stam
 */
@MessageBundle(projectCode = "JBAS")
public interface JAXRMessages {

    /**
     * The default messages.
     */
    JAXRMessages MESSAGES = Messages.getBundle(JAXRMessages.class);

    /**
     * Creates an exception indicating it could not instantiate.
     *
     * @param factoryName    JAXR ConnectionFactory implementation class.
     *
     * @return a {@link JAXRException} for the error.
     */
    @Message(id = 14080, value = "Failed to create instance of %s")
    JAXRException couldNotInstantiateJAXRFactory(String factoryName);

}
