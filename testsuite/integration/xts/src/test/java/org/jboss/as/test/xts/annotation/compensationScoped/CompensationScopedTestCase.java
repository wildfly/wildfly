/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.xts.annotation.compensationScoped;

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

import javax.inject.Inject;

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
