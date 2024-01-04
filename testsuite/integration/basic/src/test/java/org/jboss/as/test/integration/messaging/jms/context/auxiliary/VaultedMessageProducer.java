/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.jms.context.auxiliary;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.jms.Destination;
import jakarta.jms.JMSConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSPasswordCredential;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 *         <p>
 *         Use the RemoteConnectionFactory Connection Factory that requires authentication.
 */
@Stateless
public class VaultedMessageProducer {

    @Inject
    @JMSConnectionFactory("java:jboss/exported/jms/RemoteConnectionFactory")
    @JMSPasswordCredential(userName = "${test.userName}", password = "${test.password}")
    private JMSContext context;

    public void sendToDestination(Destination destination, String text) {
        context.createProducer()
                .send(destination, text);
    }
}
