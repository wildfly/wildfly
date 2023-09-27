/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.multideployment;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Weld cross deployment injection test case. Verifies that weld can read beans.xml and scan classes outside the
 * deployment.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class WeldCrossDeploymentTestCase {



    @Deployment(name = "d1", order = 1, testable = false)
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "d1.jar");
        jar.addClasses(SimpleBean.class);
        jar.addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        return jar;
    }

    @Deployment(name = "d2", order = 2)
    public static Archive<?> deploy2() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "d2.jar");
        jar.addClass(WeldCrossDeploymentTestCase.class);
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        jar.addAsManifestResource(WeldCrossDeploymentTestCase.class.getPackage(), "jboss-deployment-structure.xml", "jboss-deployment-structure.xml");
        return jar;
    }

    @Inject
    private SimpleBean bean;

    @Test
    @OperateOnDeployment("d2")
    public void testSimpleBeanInjected() throws Exception {
        Assert.assertNotNull(bean);
    }
}
