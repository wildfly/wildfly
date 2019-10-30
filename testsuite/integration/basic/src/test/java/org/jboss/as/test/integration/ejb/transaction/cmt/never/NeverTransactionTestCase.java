/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.test.integration.ejb.transaction.cmt.never;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

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
