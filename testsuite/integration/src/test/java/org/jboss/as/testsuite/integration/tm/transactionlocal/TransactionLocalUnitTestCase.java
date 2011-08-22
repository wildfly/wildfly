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
package org.jboss.as.testsuite.integration.tm.transactionlocal;

import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.tm.TransactionLocal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;


/**
 * @author istudens@redhat.com
 */
@RunWith(Arquillian.class)
public class TransactionLocalUnitTestCase {

    public static final String ARCHIVE_NAME = "transactionlocal-test";

    protected TransactionManager tm;

    @Deployment
    public static JavaArchive deploy() {
        return ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar")
                .addClass(TransactionLocalUnitTestCase.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.jboss-transaction-spi\n"),"MANIFEST.MF");
    }

    @Before
    public void initTM() {
        try {
            tm = (TransactionManager) new InitialContext().lookup("java:jboss/TransactionManager");
        } catch (Exception e) {
            throw new RuntimeException("Cannot get TransactionManager! " + e, e);
        }

    }

    @Test
    public void testSimpleSetGet() throws Exception {
        TransactionLocal local = new TransactionLocal(tm);
        tm.begin();
        try {
            local.set("Simple");
            assertEquals("Simple", local.get());
        } finally {
            tm.commit();
        }
    }

    @Test
    public void testSimpleSetNull() throws Exception {
        TransactionLocal local = new TransactionLocal(tm);
        tm.begin();
        try {
            local.set(null);
        } finally {
            tm.commit();
        }
    }

    @Test
    public void testSimpleGetWithNoInitial() throws Exception {
        TransactionLocal local = new TransactionLocal(tm);
        tm.begin();
        try {
            assertEquals(null, local.get());
        } finally {
            tm.commit();
        }
    }

    @Test
    public void testSimpleGetWithInitial() throws Exception {
        TransactionLocal local = new TransactionLocal(tm) {
            protected Object initialValue() {
                return "Initial";
            }
        };
        tm.begin();
        try {
            assertEquals("Initial", local.get());
        } finally {
            tm.commit();
        }
    }

    @Test
    public void testGetNoTx() throws Exception {
        TransactionLocal local = new TransactionLocal(tm);
        assertEquals(null, local.get(null));
    }

    @Test
    public void testGetNoTxInitial() throws Exception {
        TransactionLocal local = new TransactionLocal(tm) {
            protected Object initialValue() {
                return "Initial";
            }
        };
        assertEquals("Initial", local.get());
    }

    @Test(expected = IllegalStateException.class)
    public void testSetNoTx() throws Exception {
        TransactionLocal local = new TransactionLocal(tm);
        local.set("Something");
    }

    @Test
    public void testSimpleSetGetExplicit() throws Exception {
        TransactionLocal local = new TransactionLocal(tm);
        tm.begin();
        Transaction tx = tm.suspend();
        try {
            local.set(tx, "Simple");
            assertEquals("Simple", local.get(tx));
        } finally {
            tm.resume(tx);
            tm.commit();
        }
    }

    @Test
    public void testSimplePutNullExplicit() throws Exception {
        TransactionLocal local = new TransactionLocal(tm);
        tm.begin();
        Transaction tx = tm.suspend();
        try {
            local.set(tx, null);
        } finally {
            tm.resume(tx);
            tm.commit();
        }
    }

    @Test
    public void testSimpleGetWithNoInitialExplicit() throws Exception {
        TransactionLocal local = new TransactionLocal(tm);
        tm.begin();
        Transaction tx = tm.suspend();
        try {
            assertEquals(null, local.get(tx));
        } finally {
            tm.resume(tx);
            tm.commit();
        }
    }

    @Test
    public void testSimpleGetWithInitialExplicit() throws Exception {
        TransactionLocal local = new TransactionLocal(tm) {
            protected Object initialValue() {
                return "Initial";
            }
        };
        tm.begin();
        Transaction tx = tm.suspend();
        try {
            assertEquals("Initial", local.get(tx));
        } finally {
            tm.resume(tx);
            tm.commit();
        }
    }

    @Test
    public void testGetNoTxExplicit() throws Exception {
        TransactionLocal local = new TransactionLocal(tm);
        assertEquals(null, local.get(null));
    }

    @Test
    public void testGetNoTxInitialExplicit() throws Exception {
        TransactionLocal local = new TransactionLocal(tm) {
            protected Object initialValue() {
                return "Initial";
            }
        };
        assertEquals("Initial", local.get(null));
    }

    @Test(expected = IllegalStateException.class)
    public void testSetNoTxExplicit() throws Exception {
        TransactionLocal local = new TransactionLocal(tm);
        local.set(null, "Something");
    }

    @Test
    public void testGetAfterCommit() throws Exception {
        TransactionLocal local = new TransactionLocal(tm);
        tm.begin();
        try {
            local.set("Something");
        } finally {
            tm.commit();
        }
        assertEquals(null, local.get());
    }

    @Test
    public void testGetInitialAfterCommit() throws Exception {
        TransactionLocal local = new TransactionLocal(tm) {
            protected Object initialValue() {
                return "Initial";
            }
        };
        tm.begin();
        try {
            local.set("Something");
            assertEquals("Something", local.get());
        } finally {
            tm.commit();
        }
        assertEquals("Initial", local.get());
    }

    @Test
    public void testGetMarkedRolledBack() throws Exception {
        TransactionLocal local = new TransactionLocal(tm);
        tm.begin();
        tm.setRollbackOnly();
        try {
            assertEquals(null, local.get());
        } finally {
            tm.rollback();
        }
    }

    @Test
    public void testGetInitialMarkedRolledBack() throws Exception {
        TransactionLocal local = new TransactionLocal(tm) {
            protected Object initialValue() {
                return "Initial";
            }
        };
        tm.begin();
        tm.setRollbackOnly();
        try {
            assertEquals("Initial", local.get());
        } finally {
            tm.rollback();
        }
    }

    @Test
    public void testSetMarkedRolledBack() throws Exception {
        TransactionLocal local = new TransactionLocal(tm);
        tm.begin();
        tm.setRollbackOnly();
        try {
            local.set("Something");
            assertEquals("Something", local.get());
        } finally {
            tm.rollback();
        }
    }

    @Test
    public void testGetAfterComplete() throws Exception {
        TransactionLocal local = new TransactionLocal(tm);
        tm.begin();
        Transaction tx = tm.getTransaction();
        try {
            local.set("Something");
        } finally {
            tx.commit();
        }
        assertEquals(null, local.get());
        tm.suspend();
    }

    @Test
    public void testGetInitialAfterComplete() throws Exception {
        TransactionLocal local = new TransactionLocal(tm) {
            protected Object initialValue() {
                return "Initial";
            }
        };
        tm.begin();
        Transaction tx = tm.getTransaction();
        try {
            local.set("Something");
            assertEquals("Something", local.get());
        } finally {
            tx.commit();
        }
        assertEquals("Initial", local.get());
        tm.suspend();
    }

    @Test
    public void testSuspendResume() throws Exception {
        TransactionLocal local = new TransactionLocal(tm);
        tm.begin();
        Transaction tx1 = tm.getTransaction();
        try {
            local.set("Something");
            assertEquals("Something", local.get());
            tm.suspend();
            tm.begin();
            try {
                Transaction tx2 = tm.getTransaction();
                assertEquals(null, local.get());
                assertEquals("Something", local.get(tx1));
                tm.suspend();
                tm.resume(tx1);
                assertEquals("Something", local.get());
                assertEquals(null, local.get(tx2));
                tm.suspend();
                tm.resume(tx2);
            } finally {
                tm.commit();
            }
        } finally {
            tm.resume(tx1);
            tm.commit();
        }
    }
}
