/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.mdb.cmt.notsupported;

import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;
import static javax.ejb.TransactionManagementType.CONTAINER;
import static org.jboss.as.test.integration.ejb.mdb.cmt.notsupported.ContainerManagedTransactionNotSupportedTestCase.QUEUE_JNDI_NAME_FOR_ANNOTATION;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionManagement;
import javax.jms.Message;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@TransactionManagement(CONTAINER)
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = QUEUE_JNDI_NAME_FOR_ANNOTATION)
})
public class ContainerManagedTransactionNotSupportedMDBWithAnnotation extends BaseMDB {

    @TransactionAttribute(NOT_SUPPORTED)
    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
    }
}
