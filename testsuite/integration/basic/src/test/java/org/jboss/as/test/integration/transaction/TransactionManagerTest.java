/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.transaction;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that the TransactionManager is bound to JNDI
 * @author Stuart Douglas
 */
@ExtendWith(ArquillianExtension.class)
public class TransactionManagerTest {

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "tranaction.war");
        war.addPackage(TransactionManagerTest.class.getPackage());
        return war;
    }

    @Test
    public void testTransactionManagerBoundToJndi() throws NamingException {
        TransactionManager tm = (TransactionManager)new InitialContext().lookup("java:jboss/TransactionManager");
        Assertions.assertNotNull(tm);
    }

    @Test
    public void testTransactionSynchronizationRegistryBoundToJndi() throws NamingException {
        TransactionSynchronizationRegistry tm = (TransactionSynchronizationRegistry)new InitialContext().lookup("java:jboss/TransactionSynchronizationRegistry");
        Assertions.assertNotNull(tm);
    }
}
