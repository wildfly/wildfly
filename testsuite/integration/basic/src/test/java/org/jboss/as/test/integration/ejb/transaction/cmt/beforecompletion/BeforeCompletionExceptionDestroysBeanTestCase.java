/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.transaction.cmt.beforecompletion;

import jakarta.ejb.NoSuchEJBException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that ensures that a SFSB is destroyed if an exception is thrown by it's @BeforeCompletion method
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class BeforeCompletionExceptionDestroysBeanTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "test-beforecompletion.war");
        war.addPackage(BeforeCompletionExceptionDestroysBeanTestCase.class.getPackage());
        return war;
    }

    @Test
    public void testExceptionInBeforeCompletionDestroysBean() throws NamingException, SystemException, NotSupportedException,  HeuristicRollbackException, HeuristicMixedException {
        final UserTransaction userTransaction = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
        final BeforeCompletionSFSB bean = (BeforeCompletionSFSB) new InitialContext().lookup("java:module/" + BeforeCompletionSFSB.class.getSimpleName());
        //begin a transaction
        userTransaction.begin();
        bean.enlist();
        //commit, this should destroy the bean, as it's @BeforeCompletion method will throw an exception
        try {
            userTransaction.commit();
        } catch (RollbackException expected ) {

        }
        try {
            userTransaction.begin();
            bean.enlist();
            throw new RuntimeException("Expected SFSB to be destroyed");
        } catch (NoSuchEJBException expected) {

        } finally {
            userTransaction.rollback();
        }
    }


}
