/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.xts.annotation.compensationscoped;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.IntermittentFailure;
import org.jboss.as.test.xts.util.DeploymentHelper;
import org.jboss.narayana.compensations.internal.BAController;
import org.jboss.narayana.compensations.internal.BAControllerFactory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.inject.Inject;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@RunWith(Arquillian.class)
public class CompensationScopedTestCase {

    @Inject
    private CompensationScopedData data;

    private BAController baController;

    @Deployment
    public static Archive<?> getDeployment() {
        final WebArchive archive = DeploymentHelper.getInstance().getWebArchiveWithPermissions("test")
                .addPackage(CompensationScopedTestCase.class.getPackage());
        return archive;
    }

    @BeforeClass
    public static void failing() {
        IntermittentFailure.thisTestIsFailingIntermittently("WFLY-9871");
    }

    @Before
    public void before() {
        Assert.assertNotNull("Data should be injected", data);
        baController = BAControllerFactory.getLocalInstance();
    }

    @After
    public void after() {
        try {
            baController.cancelBusinessActivity();
        } catch (Exception e) {
        }
    }

    @Test
    public void shouldSeeDifferentValuesInDifferentTransactions() throws Exception {
        final String firstTransactionData = "FIRST_TRANSACTION_DATA";
        final String secondTransactionData = "SECOND_TRANSACTION_DATA";

        baController.beginBusinessActivity();
        updateValue(firstTransactionData);
        final Object firstTransactionContext = baController.suspend();

        baController.beginBusinessActivity();
        updateValue(secondTransactionData);
        baController.closeBusinessActivity();

        baController.resume(firstTransactionContext);
        assertValue(firstTransactionData);
        baController.closeBusinessActivity();
    }

    private void updateValue(final String value) {
        data.set(value);
        assertValue(value);
    }

    private void assertValue(final String value) {
        Assert.assertEquals("Value should be set to " + value, value, data.get());
    }

}
