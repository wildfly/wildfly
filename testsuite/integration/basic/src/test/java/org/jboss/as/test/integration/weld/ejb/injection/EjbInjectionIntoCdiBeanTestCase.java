/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.injection;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;

/**
 * AS7-1269
 *
 * Tests the @EJB injection into CDI beans works correctly
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class EjbInjectionIntoCdiBeanTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class);
        war.addPackage(EjbInjectionIntoCdiBeanTestCase.class.getPackage());
        war.addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        return war;
    }

    @Inject
    private BusStation bean;

    @Resource(lookup="java:jboss/UserTransaction")
    private UserTransaction userTransaction;

    @Test
    public void testEjbInjection() {
        Assert.assertNotNull(bean.getBus());
        Assert.assertNotNull(bean.getLookupBus());
        Assert.assertNotNull(bean.getLookupBus2());
    }

    @Test
    public void testUserTransactionInjection() {
        Assert.assertNotNull(userTransaction);
    }
}
