/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.messaging.jms.definitions;

import static org.junit.Assert.assertNotNull;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.jms.Queue;
import javax.jms.Topic;


/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@Stateless
public class MessagingBean {

    // Use a @JMSDestinationDefinition inside a @JMSDestinationDefinitions
    @Resource(lookup = "java:module/env/injectedQueue1")
    private Queue queue1;

    // Use a @JMSDestinationDefinition
    @Resource(lookup = "java:global/injectedQueue2")
    private Queue queue2;

    // Use a jms-destination from the deployment descriptor
    @Resource(lookup = "java:app/injectedQueue3")
    private Queue queue3;

    // Use a @JMSDestinationDefinition inside a @JMSDestinationDefinitions
    @Resource(lookup = "java:module/env/injectedTopic1")
    private Topic topic1;

    // Use a jms-destination from the deployment descriptor
    @Resource(lookup = "java:app/injectedTopic2")
    private Topic topic2;

    public void checkInjectedResources() {
        assertNotNull(queue1);
        assertNotNull(queue2);
        assertNotNull(queue3);
        assertNotNull(topic1);
        assertNotNull(topic2);
    }
}
