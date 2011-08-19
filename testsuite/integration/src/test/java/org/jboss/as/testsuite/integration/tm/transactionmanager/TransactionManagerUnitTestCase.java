/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.testsuite.integration.tm.transactionmanager;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;


/**
 * Tests for the transaction manager
 *
 * @author Adrian@jboss.org
 * @author istudens@redhat.com
 */
@RunWith(Arquillian.class)
public class TransactionManagerUnitTestCase {

    public static final String ARCHIVE_NAME = "tmtest";

    @Inject
    TMTestBean tmBean;

    @Rule
    public TestName name = new TestName();

    protected void runTest(Operation[] ops) throws Exception {
        tmBean.testOperations(name.getMethodName(), ops);
    }

    @Deployment
    public static JavaArchive deploy() {
        return ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar")
                .addPackage(TransactionManagerUnitTestCase.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.jboss-transaction-spi,org.jboss.common-core\n"),"MANIFEST.MF");
    }


    @Test
    public void testNoResourcesCommit() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.COMMIT, 1),
                });
    }

    @Test
    public void testNoResourcesRollback() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.ROLLBACK, 1),
                });
    }

    @Test
    public void testNoResourcesSuspendResume() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.SUSPEND, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.RESUME, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.COMMIT, 1),
                });
    }

    @Test
    public void testOneResourceCommit() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.COMMIT, 1),
                        new Operation(Operation.STATE, 1, Resource.COMMITTED),
                });
    }

    @Test
    public void testOneResourceRollback() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.ROLLBACK, 1),
                        new Operation(Operation.STATE, 1, Resource.ROLLEDBACK),
                });
    }

    @Test
    public void testOneResourceSetRollback() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.SETROLLBACK, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_MARKED_ROLLBACK),
                        new Operation(Operation.COMMIT, 1, 0, new RollbackException()),
                        new Operation(Operation.STATE, 1, Resource.ROLLEDBACK),
                });
    }

    @Test
    public void testLocalResourceCommit() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE_LOCAL, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.COMMIT, 1),
                        new Operation(Operation.STATE, 1, Resource.COMMITTED),
                });
    }

    @Test
    public void testLocalResourceRollback() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE_LOCAL, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.ROLLBACK, 1),
                        new Operation(Operation.STATE, 1, Resource.ROLLEDBACK),
                });
    }

    @Test
    public void testLocalResourceSetRollback() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE_LOCAL, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.SETROLLBACK, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_MARKED_ROLLBACK),
                        new Operation(Operation.COMMIT, 1, 0, new RollbackException()),
                        new Operation(Operation.STATE, 1, Resource.ROLLEDBACK),
                });
    }

    @Test
    public void testLocalResourceCommitFail() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE_LOCAL, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.FAIL_LOCAL, 1),
                        new Operation(Operation.COMMIT, 1, 0, new RollbackException()),
                        new Operation(Operation.STATE, 1, Resource.ROLLEDBACK),
                });
    }

    @Test
    public void testTwoResourceSameRMCommit() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.CREATE, 2),
                        new Operation(Operation.ENLIST, 2),
                        new Operation(Operation.STATE, 2, Resource.ACTIVE),
                        new Operation(Operation.COMMIT, 1),
                        new Operation(Operation.STATE, 1, Resource.COMMITTED),
                        new Operation(Operation.STATE, 2, Resource.COMMITTED),
                });
    }

    @Test
    public void testTwoResourceSameRMRollback() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.CREATE, 2),
                        new Operation(Operation.ENLIST, 2),
                        new Operation(Operation.STATE, 2, Resource.ACTIVE),
                        new Operation(Operation.ROLLBACK, 1),
                        new Operation(Operation.STATE, 1, Resource.ROLLEDBACK),
                        new Operation(Operation.STATE, 2, Resource.ROLLEDBACK),
                });
    }

    @Test
    public void testTwoResourceSameRMSetRollback() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.CREATE, 2),
                        new Operation(Operation.ENLIST, 2),
                        new Operation(Operation.STATE, 2, Resource.ACTIVE),
                        new Operation(Operation.SETROLLBACK, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_MARKED_ROLLBACK),
                        new Operation(Operation.COMMIT, 1, 0, new RollbackException()),
                        new Operation(Operation.STATE, 1, Resource.ROLLEDBACK),
                        new Operation(Operation.STATE, 2, Resource.ROLLEDBACK),
                });
    }

    @Test
    public void testTwoResourceDifferentRMCommitOneReadOnly() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.SETSTATUS, 1, XAResource.XA_RDONLY),
                        new Operation(Operation.CREATE, 2),
                        new Operation(Operation.ENLIST, 2),
                        new Operation(Operation.DIFFRM, 2),
                        new Operation(Operation.STATE, 2, Resource.ACTIVE),
                        new Operation(Operation.COMMIT, 1),
                        new Operation(Operation.STATE, 1, Resource.PREPARED),
                        new Operation(Operation.STATE, 2, Resource.COMMITTED),
                });
    }

    @Test
    public void testTwoResourceDifferentRMRollbackOneReadOnly() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.SETSTATUS, 1, XAResource.XA_RDONLY),
                        new Operation(Operation.CREATE, 2),
                        new Operation(Operation.ENLIST, 2),
                        new Operation(Operation.DIFFRM, 2),
                        new Operation(Operation.STATE, 2, Resource.ACTIVE),
                        new Operation(Operation.ROLLBACK, 1),
                        new Operation(Operation.STATE, 1, Resource.ROLLEDBACK),
                        new Operation(Operation.STATE, 2, Resource.ROLLEDBACK),
                });
    }

    @Test
    public void testTwoResourceDifferentRMSetRollbackOneReadOnly() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.SETSTATUS, 1, XAResource.XA_RDONLY),
                        new Operation(Operation.CREATE, 2),
                        new Operation(Operation.ENLIST, 2),
                        new Operation(Operation.DIFFRM, 2),
                        new Operation(Operation.STATE, 2, Resource.ACTIVE),
                        new Operation(Operation.SETROLLBACK, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_MARKED_ROLLBACK),
                        new Operation(Operation.COMMIT, 1, 0, new RollbackException()),
                        new Operation(Operation.STATE, 1, Resource.ROLLEDBACK),
                        new Operation(Operation.STATE, 2, Resource.ROLLEDBACK),
                });
    }

    @Test
    public void testTwoResourceOneLocalCommitOneReadOnly() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE_LOCAL, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.CREATE, 2),
                        new Operation(Operation.ENLIST, 2),
                        new Operation(Operation.STATE, 2, Resource.ACTIVE),
                        new Operation(Operation.SETSTATUS, 2, XAResource.XA_RDONLY),
                        new Operation(Operation.COMMIT, 1),
                        new Operation(Operation.STATE, 1, Resource.COMMITTED),
                        new Operation(Operation.STATE, 2, Resource.PREPARED),
                });
    }

    @Test
    public void testTwoResourceOneLocalRollbackOneReadOnly() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE_LOCAL, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.CREATE, 2),
                        new Operation(Operation.ENLIST, 2),
                        new Operation(Operation.STATE, 2, Resource.ACTIVE),
                        new Operation(Operation.SETSTATUS, 2, XAResource.XA_RDONLY),
                        new Operation(Operation.ROLLBACK, 1),
                        new Operation(Operation.STATE, 1, Resource.ROLLEDBACK),
                        new Operation(Operation.STATE, 2, Resource.ROLLEDBACK),
                });
    }

    @Test
    public void testTwoResourceOneLocalSetRollbackOneReadOnly() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE_LOCAL, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.CREATE, 2),
                        new Operation(Operation.ENLIST, 2),
                        new Operation(Operation.STATE, 2, Resource.ACTIVE),
                        new Operation(Operation.SETSTATUS, 2, XAResource.XA_RDONLY),
                        new Operation(Operation.SETROLLBACK, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_MARKED_ROLLBACK),
                        new Operation(Operation.COMMIT, 1, 0, new RollbackException()),
                        new Operation(Operation.STATE, 1, Resource.ROLLEDBACK),
                        new Operation(Operation.STATE, 2, Resource.ROLLEDBACK),
                });
    }

    @Test
    public void testTwoResourceOneLocalCommitFailOneReadOnly() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE_LOCAL, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.CREATE, 2),
                        new Operation(Operation.ENLIST, 2),
                        new Operation(Operation.STATE, 2, Resource.ACTIVE),
                        new Operation(Operation.SETSTATUS, 2, XAResource.XA_RDONLY),
                        new Operation(Operation.FAIL_LOCAL, 1),
                        new Operation(Operation.COMMIT, 1, 0, new RollbackException()),
                        new Operation(Operation.STATE, 1, Resource.ROLLEDBACK),
                        new Operation(Operation.STATE, 2, Resource.PREPARED),
                });
    }

    @Test
    public void testTwoResourceDifferentRMCommit() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.CREATE, 2),
                        new Operation(Operation.ENLIST, 2),
                        new Operation(Operation.DIFFRM, 2),
                        new Operation(Operation.STATE, 2, Resource.ACTIVE),
                        new Operation(Operation.COMMIT, 1),
                        new Operation(Operation.STATE, 1, Resource.COMMITTED),
                        new Operation(Operation.STATE, 2, Resource.COMMITTED),
                });
    }

    @Test
    public void testTwoResourceDifferentRMRollback() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.CREATE, 2),
                        new Operation(Operation.ENLIST, 2),
                        new Operation(Operation.DIFFRM, 2),
                        new Operation(Operation.STATE, 2, Resource.ACTIVE),
                        new Operation(Operation.ROLLBACK, 1),
                        new Operation(Operation.STATE, 1, Resource.ROLLEDBACK),
                        new Operation(Operation.STATE, 2, Resource.ROLLEDBACK),
                });
    }

    @Test
    public void testTwoResourceDifferentRMSetRollback() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.CREATE, 2),
                        new Operation(Operation.ENLIST, 2),
                        new Operation(Operation.DIFFRM, 2),
                        new Operation(Operation.STATE, 2, Resource.ACTIVE),
                        new Operation(Operation.SETROLLBACK, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_MARKED_ROLLBACK),
                        new Operation(Operation.COMMIT, 1, 0, new RollbackException()),
                        new Operation(Operation.STATE, 1, Resource.ROLLEDBACK),
                        new Operation(Operation.STATE, 2, Resource.ROLLEDBACK),
                });
    }

    @Test
    public void testTwoResourceOneLocalCommit() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE_LOCAL, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.CREATE, 2),
                        new Operation(Operation.ENLIST, 2),
                        new Operation(Operation.STATE, 2, Resource.ACTIVE),
                        new Operation(Operation.COMMIT, 1),
                        new Operation(Operation.STATE, 1, Resource.COMMITTED),
                        new Operation(Operation.STATE, 2, Resource.COMMITTED),
                });
    }

    @Test
    public void testTwoResourceOneLocalRollback() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE_LOCAL, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.CREATE, 2),
                        new Operation(Operation.ENLIST, 2),
                        new Operation(Operation.STATE, 2, Resource.ACTIVE),
                        new Operation(Operation.ROLLBACK, 1),
                        new Operation(Operation.STATE, 1, Resource.ROLLEDBACK),
                        new Operation(Operation.STATE, 2, Resource.ROLLEDBACK),
                });
    }

    @Test
    public void testTwoResourceOneLocalSetRollback() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE_LOCAL, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.CREATE, 2),
                        new Operation(Operation.ENLIST, 2),
                        new Operation(Operation.STATE, 2, Resource.ACTIVE),
                        new Operation(Operation.SETROLLBACK, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_MARKED_ROLLBACK),
                        new Operation(Operation.COMMIT, 1, 0, new RollbackException()),
                        new Operation(Operation.STATE, 1, Resource.ROLLEDBACK),
                        new Operation(Operation.STATE, 2, Resource.ROLLEDBACK),
                });
    }

    @Test
    public void testTwoResourceOneLocalCommitFail() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE_LOCAL, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.CREATE, 2),
                        new Operation(Operation.ENLIST, 2),
                        new Operation(Operation.STATE, 2, Resource.ACTIVE),
                        new Operation(Operation.FAIL_LOCAL, 1),
                        new Operation(Operation.COMMIT, 1, 0, new RollbackException()),
                        new Operation(Operation.STATE, 1, Resource.ROLLEDBACK),
                        new Operation(Operation.STATE, 2, Resource.ROLLEDBACK),
                });
    }

    @Test
    public void testOneResourceCommitHeurRB() throws Exception {
        runTest(new Operation[]
                {
                        new Operation(Operation.BEGIN, 1),
                        new Operation(Operation.STATUS, 1, Status.STATUS_ACTIVE),
                        new Operation(Operation.CREATE, 1),
                        new Operation(Operation.ENLIST, 1),
                        new Operation(Operation.STATE, 1, Resource.ACTIVE),
                        new Operation(Operation.SETSTATUS, 1, XAException.XA_HEURRB),
                        new Operation(Operation.COMMIT, 1, 0, new RollbackException()),
                        new Operation(Operation.STATE, 1, Resource.FORGOT),
                });
    }
}
