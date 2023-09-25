/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.xts.annotation.client;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.xts.annotation.service.CompensatableService;
import org.jboss.as.test.xts.annotation.service.CompensatableServiceImpl;
import org.jboss.as.test.xts.util.DeploymentHelper;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.arjuna.mw.wst11.UserBusinessActivity;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@RunWith(Arquillian.class)
public class CompensatableTestCase {

    private static final String DEPLOYMENT_NAME = "compensatable-test";

    private static final String SERVER_HOST_PORT = TestSuiteEnvironment.getServerAddress() + ":"
            + TestSuiteEnvironment.getHttpPort();

    private static final String DEPLOYMENT_URL = "http://" + SERVER_HOST_PORT + "/" + DEPLOYMENT_NAME;

    @Deployment
    public static WebArchive getDeployment() {
        return DeploymentHelper.getInstance().getWebArchiveWithPermissions(DEPLOYMENT_NAME)
                .addClass(CompensatableClient.class)
                .addClass(CompensatableService.class)
                .addClass(CompensatableServiceImpl.class)
                .addClass(TestSuiteEnvironment.class);
    }

    @Test
    public void testNoTransaction() throws Exception {
        final String deploymentUrl = DEPLOYMENT_URL;
        final CompensatableService compensatableService = CompensatableClient.newInstance(deploymentUrl);

        final boolean isTransactionActive = compensatableService.isTransactionActive();

        Assert.assertEquals(false, isTransactionActive);
    }

    @Test
    public void testActiveTransaction() throws Exception {
        final String deploymentUrl = DEPLOYMENT_URL;
        final CompensatableService compensatableService = CompensatableClient.newInstance(deploymentUrl);

        final UserBusinessActivity userBusinessActivity = UserBusinessActivity.getUserBusinessActivity();

        userBusinessActivity.begin();
        final boolean isTransactionActive = compensatableService.isTransactionActive();
        userBusinessActivity.close();

        Assert.assertEquals(true, isTransactionActive);
    }

}
