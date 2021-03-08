/*
 * JBoss, Home of Professional Open Source
 * Copyright 2019, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.integration.weld.context.application.lifecycle;

import org.jboss.logging.Logger;

import javax.enterprise.context.control.RequestContextController;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Queue;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author emmartins
 */
public interface Utils {

    static void sendMessage(JMSContext jmsContext, Queue jmsQueue, RequestContextController requestContextController) {
        requestContextController.activate();
        final JMSProducer producer = jmsContext.createProducer();
        producer.send(jmsQueue, "a message");
        requestContextController.deactivate();
    }

    static void lookupCommonResources(Logger logger, boolean statelessEjb) throws NamingException {
        final InitialContext initialContext = new InitialContext();
        // lookup app name
        logger.info("java:app/AppName: "+initialContext.lookup("java:app/AppName"));
        // lookup module name
        logger.info("java:module/ModuleName: "+initialContext.lookup("java:module/ModuleName"));
        // lookup default EE concurrency resources
        logger.info("java:comp/DefaultContextService: "+initialContext.lookup("java:comp/DefaultContextService"));
        logger.info("java:comp/DefaultManagedExecutorService: "+initialContext.lookup("java:comp/DefaultManagedExecutorService"));
        logger.info("java:comp/DefaultManagedScheduledExecutorService: "+initialContext.lookup("java:comp/DefaultManagedScheduledExecutorService"));
        logger.info("java:comp/DefaultManagedThreadFactory: "+initialContext.lookup("java:comp/DefaultManagedThreadFactory"));
        // lookup default datasource
        logger.info("java:comp/DefaultDataSource: "+initialContext.lookup("java:comp/DefaultDataSource"));
        // lookup default Jakarta Messaging connection factory
        logger.info("java:comp/DefaultJMSConnectionFactory: "+initialContext.lookup("java:comp/DefaultJMSConnectionFactory"));
        // lookup tx resources
        logger.info("java:comp/TransactionSynchronizationRegistry: "+initialContext.lookup("java:comp/TransactionSynchronizationRegistry"));
        if (!statelessEjb) {
            logger.info("java:comp/UserTransaction: "+initialContext.lookup("java:comp/UserTransaction"));
        } else {
            logger.info("java:comp/EJBContext: "+initialContext.lookup("java:comp/EJBContext"));
        }
    }
}
