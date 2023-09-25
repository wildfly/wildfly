/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.context.application.lifecycle;

import org.jboss.logging.Logger;

import jakarta.enterprise.context.control.RequestContextController;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Queue;
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
