/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.multideployment;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
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
        StringAsset beansXml = new StringAsset("<beans bean-discovery-mode=\"all\"></beans>");
        WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war")
                .addClasses(WeldBundledLibraryDeploymentEarTestCase.class, AbstractBundledLibraryDeploymentTestCase.class)
                .addAsWebInfResource(beansXml, "beans.xml");
        JavaArchive library = ShrinkWrap.create(JavaArchive.class, "library.jar").addClasses(InjectedBean.class).addAsManifestResource(beansXml, "beans.xml");
        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "ejb-archive.jar").addClasses(InjectedSessionBean.class).addAsManifestResource(beansXml, "beans.xml");
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class);
        ear.addAsModule(war);
        ear.addAsModule(ejbJar);
        ear.addAsLibrary(library);
        ear.setManifest(new StringAsset("Extension-List: weld1\nweld1-Extension-Name: " + EXTENSION_NAME + "\n"));
        return ear;
    }
}
