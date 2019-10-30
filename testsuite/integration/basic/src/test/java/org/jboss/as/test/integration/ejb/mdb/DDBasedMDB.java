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

package org.jboss.as.test.integration.ejb.mdb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
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
