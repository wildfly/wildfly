/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.cmt.fail;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.io.File;
import java.util.PropertyPermission;

import jakarta.ejb.EJBException;
import jakarta.inject.Inject;
import javax.naming.InitialContext;
import javax.transaction.xa.XAResource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.as.test.integration.transactions.RemoteLookups;
import org.jboss.as.test.integration.transactions.TxTestUtil;
import org.jboss.as.test.integration.transactions.spi.TestLastResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.tm.LastResource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test of behavior when one phase commit is used.
 */
@RunWith(Arquillian.class)
public class TransactionFirstPhaseErrorTestCase {

    @ArquillianResource
    private InitialContext initCtx;

    @Inject
    private TransactionCheckerSingleton checker;

    @Deployment
    public static Archive<?> createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "test-txn-one-phase.jar")
        .addPackage(TxTestUtil.class.getPackage())
        .addClass(TestLastResource.class)
        .addClasses(InnerBean.class, OuterBean.class)
        .addAsManifestResource(new StringAsset("Dependencies: org.jboss.jboss-transaction-spi \n"), "MANIFEST.MF")
        // grant necessary permissions for -Dsecurity.manager
        .addAsResource(createPermissionsXmlAsset(
            new PropertyPermission("ts.timeout.factor", "read")), "META-INF/jboss-permissions.xml");
        return jar;
    }

    @Before
    public void startUp() {
        checker.resetAll();
    }

    @After
    public void tearDown() {
        String jbossHome = System.getProperty("jboss.home.dir");
        String defaultStorePath = jbossHome + "/standalone/data/tx-object-store";
        File rootDir = new File(defaultStorePath);
        if (rootDir.exists() && rootDir.isDirectory()) {
            deleteContents(rootDir);
        }
    }

    private void deleteContents(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                deleteContents(file);
            }
            if (!file.delete()) {
                throw new RuntimeException("Failed to delete: " + file.getAbsolutePath());
            }
        }
    }

    /**
     * Using {@link XAResource} which fails with <code>XAResource.XAER_RMFAIL</code>
     * during commit.<br>
     * Expecting the error will be propagated to the caller.
     */
    @Test
    public void xaOnePhaseCommitFail() throws Exception {
        OuterBean bean = RemoteLookups.lookupModule(initCtx, OuterBean.class);
        try {
            bean.outerMethodXA();
            Assert.fail("Expecting the one phase commit failed and exception was propagated to the caller.");
        } catch (EJBException expected) {
            Assert.assertTrue("Expecting on RMFAIL to get unknown state of the transaction outcome - ie. HeuristicMixedException",
                    expected.getCause() != null && expected.getCause().getClass().equals(jakarta.transaction.HeuristicMixedException.class));
        }
    }

    /**
     * Using two {@link XAResource}s where the first one fails with <code>XAResource.XAER_RMFAIL</code>
     * during commit.<br>
     * Expecting the no error will be thrown as 2PC prepare phase finished and rmfail says
     * that recovery manager should retry the commit later.
     */
    @Test
    public void xaTwoPhaseCommitFail() throws Exception {
        OuterBean bean = RemoteLookups.lookupModule(initCtx, OuterBean.class);
        bean.outerMethod2pcXA();
    }

    /**
     * Using {@link XAResource} where optimization for {@link LastResource} is used.
     * The commit call fails with <code>XAResource.XAER_RMFAIL</code><br>
     * Expecting the error will be propagated to the caller.
     */
    @Test
    public void localOnePhaseCommitFail() throws Exception {
        OuterBean bean = RemoteLookups.lookupModule(initCtx, OuterBean.class);
        try {
            bean.outerMethodLocal();
            Assert.fail("Expecting the one phase commit failed and exception was propagated to the caller.");
        } catch (EJBException expected) {
            Assert.assertTrue("Expecting on RMFAIL to get unknown state of the transaction outcome - ie. HeuristicMixedException",
                    expected.getCause() != null && expected.getCause().getClass().equals(jakarta.transaction.HeuristicMixedException.class));
        }
    }
}
