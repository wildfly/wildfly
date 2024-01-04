/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.mdb.timeout;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.util.PropertyPermission;

import jakarta.inject.Inject;
import jakarta.jms.Queue;
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
 * and the timeout time in MDB is specified by {@link TxTestUtil#waitForTimeout(jakarta.transaction.TransactionManager)}.
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
        Assert.assertNull("No message should be received as mdb timeouted", MessageDrivenTimeoutTestCase.receiveMessage(q, initCtx, false));

        Assert.assertEquals("Expecting no commmit happened as default timeout was hit", 0, checker.getCommitted());
    }
}
