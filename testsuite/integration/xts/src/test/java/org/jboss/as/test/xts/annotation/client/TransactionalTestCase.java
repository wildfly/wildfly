/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.xts.annotation.client;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.xts.annotation.service.TransactionalService;
import org.jboss.as.test.xts.annotation.service.TransactionalServiceImpl;
import org.jboss.as.test.xts.util.DeploymentHelper;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.arjuna.mw.wst11.UserTransaction;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@RunWith(Arquillian.class)
public class TransactionalTestCase {

    private static final String DEPLOYMENT_NAME = "transactional-test";

    private static final String SERVER_HOST_PORT = TestSuiteEnvironment.getServerAddress() + ":"
            + TestSuiteEnvironment.getHttpPort();

    private static final String DEPLOYMENT_URL = "http://" + SERVER_HOST_PORT + "/" + DEPLOYMENT_NAME;

    @Deployment
    public static WebArchive getDeployment() {
        return DeploymentHelper.getInstance().getWebArchiveWithPermissions(DEPLOYMENT_NAME)
                .addClass(TransactionalClient.class)
                .addClass(TransactionalService.class)
                .addClass(TransactionalServiceImpl.class)
                .addClass(TestSuiteEnvironment.class)
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.xts,org.jboss.jts\n"), "MANIFEST.MF");
    }

    @Test
    public void testNoTransaction() throws Exception {
        final String deploymentUrl = DEPLOYMENT_URL;
        final TransactionalService transactionalService = TransactionalClient.newInstance(deploymentUrl);

        final boolean isTransactionActive = transactionalService.isTransactionActive();

        Assert.assertEquals(false, isTransactionActive);
    }

    @Test
    public void testActiveTransaction() throws Exception {
        final String deploymentUrl = DEPLOYMENT_URL;
        final TransactionalService transactionalService = TransactionalClient.newInstance(deploymentUrl);
        final UserTransaction userTransaction = UserTransaction.getUserTransaction();

        userTransaction.begin();
        final boolean isTransactionActive = transactionalService.isTransactionActive();
        userTransaction.commit();

        Assert.assertEquals(true, isTransactionActive);
    }

}
