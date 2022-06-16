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
package org.jboss.as.test.integration.ejb.transaction.cmt.timeout;

import static org.junit.Assert.assertEquals;

import javax.naming.InitialContext;
import jakarta.transaction.TransactionManager;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.transaction.client.ContextTransactionManager;

/**
 */
@RunWith(Arquillian.class)
public class TransactionTimeoutTestCase {

    @Deployment
    public static Archive<?> createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "test-ejb-cmt-timeout.jar");
        jar.addClass(BeanWithTimeoutValue.class);
        jar.addClass(TimeoutRemoteView.class);
        jar.addClass(TimeoutLocalView.class);
        jar.addAsManifestResource(TransactionTimeoutTestCase.class.getPackage(), "jboss-ejb3.xml",
                "jboss-ejb3.xml");
        jar.addAsManifestResource(TransactionTimeoutTestCase.class.getPackage(), "jboss.properties",
                "jboss.properties");
        return jar;
    }

    /**
     */
    @Test
    public void testBeanTimeouts() throws Exception {
        TimeoutRemoteView remoteView = (TimeoutRemoteView) (new InitialContext()
                .lookup("java:module/BeanWithTimeoutValue!org.jboss.as.test.integration.ejb.transaction.cmt.timeout.TimeoutRemoteView"));
        TimeoutLocalView localView = (TimeoutLocalView) (new InitialContext()
                .lookup("java:module/BeanWithTimeoutValue!org.jboss.as.test.integration.ejb.transaction.cmt.timeout.TimeoutLocalView"));

        long timeoutValue = -1;
        timeoutValue = (long) remoteView.getBeanTimeout();
        Assert.assertEquals("Bean-level timeout failed", 5L, timeoutValue);
        timeoutValue = (long) remoteView.getBeanMethodTimeout();
        Assert.assertEquals("Bean-method timeout failed", 6L, timeoutValue);
        timeoutValue = (long) remoteView.getRemoteMethodTimeout();
        Assert.assertEquals("Remote-method timeout failed", 7L, timeoutValue);
        timeoutValue = (long) localView.getLocalViewTimeout();
        Assert.assertEquals("Local-view timeout failed", 5L, timeoutValue);
    }

    @Test
    public void testDescriptor() throws Exception {
        final TimeoutLocalView localView = (TimeoutLocalView) new InitialContext()
                .lookup("java:module/DDBeanWithTimeoutValue!" + TimeoutLocalView.class.getName());
        assertEquals(10, localView.getLocalViewTimeout());
    }

    @Test
    public void testDescriptorWithNestedExpressions() throws Exception {
        final TimeoutLocalView localView = (TimeoutLocalView) new InitialContext()
                .lookup("java:module/DDBeanWithTimeoutValueUsingNestedExpression!" + TimeoutLocalView.class
                        .getName());
        assertEquals(90, localView.getLocalViewTimeout());
    }

    @Test
    public void threadStoringTimeout() throws Exception {
        TimeoutLocalView localView = (TimeoutLocalView) (new InitialContext()
            .lookup("java:module/BeanWithTimeoutValue!org.jboss.as.test.integration.ejb.transaction.cmt.timeout.TimeoutLocalView"));
        TransactionManager tm = (TransactionManager) new InitialContext().lookup("java:/TransactionManager");

        int transactionTimeoutToSet = 42;
        tm.setTransactionTimeout(transactionTimeoutToSet);
        Assert.assertEquals("Expecting transaction timeout has to be the same as it was written by setter",
                transactionTimeoutToSet, getTransactionTimeout(tm));

        localView.getLocalViewTimeout();

        Assert.assertEquals("The transaction timeout has to be the same as before CMT call",
            transactionTimeoutToSet, getTransactionTimeout(tm));
    }

    private int getTransactionTimeout(TransactionManager tmTimeout) {
        if (tmTimeout instanceof ContextTransactionManager) {
            return ((ContextTransactionManager) tmTimeout).getTransactionTimeout();
        }
        throw new IllegalStateException("Cannot get transaction timeout");
    }
}
