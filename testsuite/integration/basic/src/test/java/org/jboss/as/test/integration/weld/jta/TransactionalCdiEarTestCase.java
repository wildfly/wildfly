/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.jta;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that the Transactional annotation works in a CDI Bean which is in an ear deployment.
 * <p>
 * This is a test case for WFLY-11506 that requires standalone.xml server configuration. Notice that if you are using
 * -Dtest= system property to run this test, the result could end up in a false negative because Wildfly uses
 * standalone-full.xml configuration file to run individual tests, and this variant includes the org.jboss.jts module in
 * the ear classpath via MessagingDependencyProcessor, making the issue not reproducible.
 *
 * @author Yeray Borges
 */
@RunWith(Arquillian.class)
public class TransactionalCdiEarTestCase {

    @Inject
    CdiBean cdiBean;

    @Deployment
    public static EnterpriseArchive deployment(){
        final String deployName = TransactionalCdiEarTestCase.class.getSimpleName();

        final WebArchive warModule = ShrinkWrap.create(WebArchive.class, deployName + ".war")
                .addClasses(CdiBean.class, TransactionalCdiEarTestCase.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, deployName + ".ear")
                .addAsModule(warModule);

        return ear;
    }

    @Test
    public void testIfTransactionIsActive() {
        Assert.assertTrue(cdiBean.isTransactionActive());
    }

    @Test
    public void testIfTransactionIsInactive() {
        Assert.assertTrue(cdiBean.isTransactionInactive());
    }

}
