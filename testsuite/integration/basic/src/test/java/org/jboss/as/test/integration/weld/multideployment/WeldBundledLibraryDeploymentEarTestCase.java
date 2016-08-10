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
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that CDI beans defined in an installed library can be used in an .ear deployment.
 *
 * @author Jozef Hartinger
 * @see https://issues.jboss.org/browse/AS7-6821
 */
@RunWith(Arquillian.class)
public class WeldBundledLibraryDeploymentEarTestCase extends AbstractBundledLibraryDeploymentTestCase {

    @Inject
    private SimpleBean bean;

    @Inject
    private InjectedBean injectedBean;

    @Inject
    private InjectedSessionBean injectedSessionBean;

    @Test
    public void testSimpleBeanInjected() {
        Assert.assertNotNull(bean);
        bean.ping();

        Assert.assertNotNull(injectedBean);
        Assert.assertNotNull(injectedBean.getBean());
        injectedBean.getBean().ping();

        Assert.assertNotNull(injectedSessionBean);
        Assert.assertNotNull(injectedSessionBean.getBean());
        injectedSessionBean.getBean().ping();
    }

    @Deployment
    public static Archive<?> getDeployment() throws Exception {
        doSetup();

        WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war")
                .addClasses(WeldBundledLibraryDeploymentEarTestCase.class, AbstractBundledLibraryDeploymentTestCase.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        JavaArchive library = ShrinkWrap.create(JavaArchive.class, "library.jar").addClasses(InjectedBean.class).addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "ejb-archive.jar").addClasses(InjectedSessionBean.class).addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class);
        ear.addAsModule(war);
        ear.addAsModule(ejbJar);
        ear.addAsLibrary(library);
        ear.setManifest(new StringAsset("Extension-List: weld1\nweld1-Extension-Name: " + EXTENSION_NAME + "\n"));
        return ear;
    }
}
