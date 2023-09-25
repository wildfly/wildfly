/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import javax.sql.DataSource;

import org.jboss.logging.Logger;

/**
 * User: jpai
 */
public class DDBasedMDB implements MessageListener {

    @EJB
    private JMSMessagingUtil jmsMessagingUtil;
    @EJB
    private BMTSLSB bmtslsb;

    @Resource(lookup = "java:jboss/datasources/ExampleDS")
    private DataSource dataSource;

    private static final Logger logger = Logger.getLogger(DDBasedMDB.class);

    static {
        // @see https://issues.jboss.org/browse/WFLY-3989
        // do an activity that depends on TCCL being the right one (== the application classloader)
        final String className = DDBasedMDB.class.getName();
        try {
            loadClassThroughTCCL(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load " + className + " through TCCL " + Thread.currentThread().getContextClassLoader() + " in the static block of MDB");
        }
    }

    public DDBasedMDB() {
        // @see https://issues.jboss.org/browse/WFLY-3989
        // do an activity that depends on TCCL being the right one (== the application classloader)
        final String className = this.getClass().getName();
        try {
            loadClassThroughTCCL(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load " + className + " through TCCL " + Thread.currentThread().getContextClassLoader() + " in the constructor of MDB");
        }
    }

    @Override
    public void onMessage(Message message) {
        logger.trace("Received message " + message + " in MDB " + this.getClass().getName());
        Connection conn = null;
        try {
            final Destination replyTo = message.getJMSReplyTo();
            if (replyTo == null) {
                return;
            }

            logger.trace("Doing a DB operation using a DataSource");
            try {
                conn = dataSource.getConnection();
                final PreparedStatement preparedStatement = conn.prepareStatement("select upper('foo')");
                preparedStatement.execute();
            } catch (SQLException e) {
                throw  new RuntimeException(e);
            }
            logger.trace("Done invoking DB operation. Holding on to connection till this method completes");
            logger.trace("Invoking a BMT SLSB which will use UserTransaction");
            bmtslsb.doSomethingWithUserTransaction();
            logger.trace("Sending a reply to destination " + replyTo);
            jmsMessagingUtil.reply(message);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static void loadClassThroughTCCL(String className) throws ClassNotFoundException {
        Thread.currentThread().getContextClassLoader().loadClass(className);
    }
}
