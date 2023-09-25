/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.ejb;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class RolePropagationWithoutExplicitSecDomainElytronTest {

    private static final String BEAN_DEPLOYMENT = "PostConstructStartupSingletonBeanDeployment";

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = BEAN_DEPLOYMENT, managed = false)
    public static WebArchive createDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "PostConstructStartupSingletonBeanDeployment" + ".war");
        war.addClasses(RolePropagationTestSecuredBean.class);
        war.addClasses(RolePropagationTest.class);
        war.addClasses(RolePropagationTestImpl.class);
        return war;
    }

    @Test
    @RunAsClient
    public void testStartupSingletonSecuredBeanRolePropagation() {
        try {
            deployer.deploy(BEAN_DEPLOYMENT);
        } catch (Exception ex) {
            Assert.fail("Deployment should not fail because TEST role should have been propagated from RunAs annotation.");
        } finally {
            deployer.undeploy(BEAN_DEPLOYMENT);
        }
    }
}
