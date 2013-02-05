/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.weld.multideployment;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
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
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
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
