/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.testsuite.integration.tm.txtimeoutannot;

import javax.ejb.EJBTransactionRolledbackException;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for transaction timeout annotation
 * EJB3 version of TxTimeoutUnitTestCase
 * <p/>
 * JBAS-4011, the arjuna transaction manager does not allow the
 * setting of the global default tx timeout after the tx manager
 * is started, so we won't test the default timeout setting (300secs).
 *
 * @author adrian@jboss.com
 * @author pskopek@redhat.com
 * @author istudens@redhat.com
 */
@RunWith(Arquillian.class)
public class TxTimeoutAnnotUnitTestCase {
    public static final String ARCHIVE_NAME = "txtimeoutannottest";

    @Inject
    TxTimeoutAnnotBean bean;

    @Deployment
    public static JavaArchive deploy() {
        return ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar")
                .addPackage(TxTimeoutAnnotUnitTestCase.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.jboss-transaction-spi\n"),"MANIFEST.MF");
    }

    /**
     * Tests whether @TransactionTimeout expires system sends EJBTransactionRolledbackException
     * and transaction is rolled back.
     *
     * @throws Exception
     */
    @Test(expected = EJBTransactionRolledbackException.class)
    public void testOverriddenTimeoutExpires() throws Exception {
        bean.testOverriddenTimeoutExpires();
    }

    /**
     * Tests whether @TransactionTimeout expires greater that wait time of doesn't make transaction to roll back
     * or stay in different status that STATUS_ACTIVE.
     *
     * @throws Exception
     */
    @Test
    public void testOverriddenTimeoutDoesNotExpire() throws Exception {
        bean.testOverriddenTimeoutDoesNotExpire();
    }

}
