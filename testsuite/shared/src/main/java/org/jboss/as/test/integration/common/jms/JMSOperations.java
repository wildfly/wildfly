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

package org.jboss.as.test.integration.common.jms;

import org.jboss.as.controller.client.ModelControllerClient;

/**
 * Utility to administrate JMS-related resources on the server. An separate implementation should be created for
 * every possible JMS provider to be tested.
 * Use JMSOperationsProvider to get instances of implementing classes.
 * An implementing class must have a default constructor.
 * Specify the fully qualified name of the activated implementation class in resources/jmsoperations.properties file.
 * @author <a href="jmartisk@redhat.com">Jan Martiska</a>
 */
public interface JMSOperations {

    public void createJmsQueue(final String queueName, final String jndiName);

    public void createJmsTopic(final String topicName, final String jndiName);

    public void removeJmsQueue(final String queueName);

    public void removeJmsTopic(final String topicName);

    public void close();

    /**
     * not recommended to access the controller client directly
     * prefer to use the abstractions provided by a JMSOperations implementation!
     */
    @Deprecated public ModelControllerClient getModelControllerClient();

}
