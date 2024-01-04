/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.cmt.notsupported;

import static jakarta.ejb.TransactionAttributeType.NOT_SUPPORTED;
import static jakarta.ejb.TransactionManagementType.CONTAINER;
import static org.jboss.as.test.integration.ejb.mdb.cmt.notsupported.ContainerManagedTransactionNotSupportedTestCase.QUEUE_JNDI_NAME_FOR_ANNOTATION;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionManagement;
import jakarta.jms.Message;

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
