/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.context.application.startup;

import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.Testable;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testcase for WFLY-20787
 * Resource injection (@Resource) done during @Startup event should have access to all the namespaces.
 * This test is akin to the reproducer in the issue - an EAR with EJB JAR that has a CDI bean with @Startup and @Resource.
 *
 */
@RunWith(Arquillian.class)
public class ResourceInjectionOnStartupTestCase {

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ejb = Testable.archiveToTest(ShrinkWrap.create(JavaArchive.class)
                .addClasses(StartupBean.class, DummySessionBean.class, ResourceInjectionOnStartupTestCase.class));
        return ShrinkWrap.create(EnterpriseArchive.class).addAsModule(ejb);
    }

    @Inject
    StartupBean bean;

    @Test
    public void testResourceInjection() {
        Assert.assertNotNull(bean.getObservedPayload());
        Assert.assertNotNull(bean.getTransaction());
    }
}
