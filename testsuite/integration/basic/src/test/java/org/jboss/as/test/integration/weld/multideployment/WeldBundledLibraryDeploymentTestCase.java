/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
 * Tests that CDI beans defined in a bundled library can be used in a deployment
 *
 * @author Jozef Hartinger
 *
 */
@RunWith(Arquillian.class)
public class WeldBundledLibraryDeploymentTestCase extends AbstractBundledLibraryDeploymentTestCase {

    @Inject
    private SimpleBean bean;

    @Test
    public void testSimpleBeanInjected() {
        Assert.assertNotNull(bean);
        bean.ping();
    }

    @Deployment
    public static Archive<?> getDeployment() throws Exception {
        doSetup();
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        jar.addClasses(WeldBundledLibraryDeploymentTestCase.class, AbstractBundledLibraryDeploymentTestCase.class);
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        jar.setManifest(new StringAsset("Extension-List: weld1\nweld1-Extension-Name: " + EXTENSION_NAME + "\n"));
        return jar;
    }
}
