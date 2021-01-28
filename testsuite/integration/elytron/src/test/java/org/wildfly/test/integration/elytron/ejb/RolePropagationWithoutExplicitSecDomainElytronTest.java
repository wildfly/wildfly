/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
        if (System.getProperty("elytron") != null) {
            try {
                deployer.deploy(BEAN_DEPLOYMENT);
            } catch (Exception ex) {
                Assert.fail("Deployment should not fail because TEST role should have been propagated from RunAs annotation.");
            } finally {
                deployer.undeploy(BEAN_DEPLOYMENT);
            }
        }
    }
}
