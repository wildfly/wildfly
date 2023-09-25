/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.transaction.cmt.never;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.ejb.EJBException;
import jakarta.inject.Inject;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

/**
 * Test that makes sure a SFSB is not discarded if the transaction
 * interceptor throws an exception.
 *
 * This test also verifies that Jandex handles methods with arrays in their signature correctly (see AS7-1697)
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class NeverTransactionTestCase {

    @Inject
    private UserTransaction userTransaction;

    @Inject
    private NeverSFSB sfsb;

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "test-never.war");
        war.addPackage(NeverTransactionTestCase.class.getPackage());
        war.add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml");
        return war;
    }

    @Test
    public void testExceptionThrown() throws SystemException, NotSupportedException {
        sfsb.doStuff(new String[0],"",0);
        userTransaction.begin();
        try {
            sfsb.doStuff(new String[0],"",0);
            throw new RuntimeException("Expected exception when calling NEVER method with an active transaction");
        } catch (EJBException e) {

        }
        userTransaction.rollback();
    }

}
