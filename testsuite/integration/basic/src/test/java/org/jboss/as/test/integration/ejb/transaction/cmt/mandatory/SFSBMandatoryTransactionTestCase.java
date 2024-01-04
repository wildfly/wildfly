/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.transaction.cmt.mandatory;

import jakarta.ejb.EJBTransactionRequiredException;
import jakarta.inject.Inject;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that makes sure a SFSB is not discarded if the transaction
 * interceptor throws an exception.
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class SFSBMandatoryTransactionTestCase {

    @Inject
    private UserTransaction userTransaction;

    @Inject
    private MandatorySFSB sfsb;

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "test-mandatory.war");
        war.addPackage(SFSBMandatoryTransactionTestCase.class.getPackage());
        war.add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml");
        return war;
    }

    @Test
    public void testSFSBNotDestroyed() throws SystemException, NotSupportedException {
        try {
            sfsb.doStuff();
            throw new RuntimeException("Expected an EJBTransactionRequiredException");
        } catch (EJBTransactionRequiredException e) {
            //ignore
        }
        userTransaction.begin();
        sfsb.doStuff();
        userTransaction.rollback();
    }

}
