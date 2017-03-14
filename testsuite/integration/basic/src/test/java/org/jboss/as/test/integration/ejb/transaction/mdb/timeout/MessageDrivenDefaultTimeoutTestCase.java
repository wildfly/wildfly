/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.transaction.mdb.timeout;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.util.PropertyPermission;

import javax.inject.Inject;
import javax.jms.Queue;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.as.test.integration.transactions.TxTestUtil;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * <p>
 * Test of timeouting a global transaction on MDB because of default transaction
 * timeout is redefined to be small enough to timeout being hit.
 * <p>
 * Default transaction timeout is defined under {@link TransactionDefaultTimeoutSetupTask}
 * and the timeout time in MDB is specified by {@link TxTestUtil#waitForTimeout(javax.transaction.TransactionManager)}.
 */
@RunWith(Arquillian.class)
@ServerSetup({TransactionTimeoutQueueSetupTask.class, TransactionDefaultTimeoutSetupTask.class})
public class MessageDrivenDefaultTimeoutTestCase {

    @ArquillianResource
    private InitialContext initCtx;

    @Inject
    private TransactionCheckerSingleton checker;

    @Deployment
    public static Archive<?> deployment() {
        final Archive<?> deployment = ShrinkWrap.create(JavaArchive.class, "mdb-default-timeout.jar")
            .addPackage(MessageDrivenDefaultTimeoutTestCase.class.getPackage())
            .addPackage(TxTestUtil.class.getPackage())
            .addClass(TimeoutUtil.class)
            // grant necessary permissions for -Dsecurity.manager because of usage TimeoutUtil
            .addAsResource(createPermissionsXmlAsset(
                new PropertyPermission("ts.timeout.factor", "read")), "META-INF/jboss-permissions.xml");
        return deployment;
    }

    @Before
    public void startUp() throws NamingException {
        checker.resetAll();
    }

    /**
     * MDB receives a message with default transaction timeout redefined.
     * The bean waits till timeout occurs and the transaction should be rolled-back.
     */
    @Test
    public void defaultTimeout() throws Exception {
        String text = "default timeout";
        Queue q = MessageDrivenTimeoutTestCase.sendMessage(text, TransactionTimeoutQueueSetupTask.DEFAULT_TIMEOUT_JNDI_NAME, initCtx);
        Assert.assertNull("No message should be received as mdb timeouted", MessageDrivenTimeoutTestCase.receiveMessage(q, initCtx));

        Assert.assertEquals("Expecting no commmit happened as default timeout was hit", 0, checker.getCommitted());
        Assert.assertTrue("Expecting a rollback happened as default timeout was hit", checker.getRolledback() > 0);
    }
}
