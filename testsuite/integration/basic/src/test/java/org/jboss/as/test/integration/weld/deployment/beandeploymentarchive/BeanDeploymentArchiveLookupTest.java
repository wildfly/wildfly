/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.deployment.beandeploymentarchive;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.manager.BeanManagerLookupService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests WFLY-9536
 * @author <a href="mailto:manovotn@redhat.com">Matej Novotny</a>
 */
@RunWith(Arquillian.class)
public class BeanDeploymentArchiveLookupTest {

    @Deployment
    public static WebArchive createTestArchive() {
        // we are going to use WAR and one JAR, the reason is that the JAR should have different BM Impl against which we can compare
        JavaArchive lib = ShrinkWrap.create(JavaArchive.class).addClasses(NotABean.class, SomeBean.class)
            .addAsManifestResource(createBeansXml("annotated"), "beans.xml");

        return ShrinkWrap.create(WebArchive.class).addClasses(BeanDeploymentArchiveLookupTest.class, SomeOtherBean.class)
            .addAsWebInfResource(createBeansXml("annotated"), "beans.xml")
            .addAsLibraries(lib);
    }

    private static StringAsset createBeansXml(String mode) {
        return new StringAsset("<beans bean-discovery-mode=\"" + mode + "\" version=\"2.0\"/>");
    }

    @Inject
    BeanManager bm;

    @Test
    public void verifyDiscoveryOfAllClasses() {
        // BM proxy is injected, we unwrap it
        BeanManagerImpl bmImpl = BeanManagerProxy.unwrap(bm);

        // use service to lookup BM based on class
        BeanManagerImpl bmFromSomeBean = BeanManagerLookupService.lookupBeanManager(SomeBean.class, bmImpl);
        BeanManagerImpl bmFromNotABean = BeanManagerLookupService.lookupBeanManager(NotABean.class, bmImpl);
        BeanManagerImpl bmFromSomeOtherBean = BeanManagerLookupService.lookupBeanManager(SomeOtherBean.class, bmImpl);
        Assert.assertNotNull(bmFromSomeBean);
        Assert.assertNotNull(bmFromNotABean);
        Assert.assertNotNull(bmFromSomeOtherBean);
        // verify that BMs from WAR and JAR differ
        Assert.assertNotEquals(bmFromSomeBean, bmFromSomeOtherBean);
        // Manager injected here is the same as the one for SomeOtherBean as they are in the same archive
        Assert.assertEquals(bmImpl, bmFromSomeOtherBean);
        // BM injected here should be different from that we get from classes in JAR lib
        Assert.assertNotEquals(bmImpl, bmFromSomeBean);
        Assert.assertNotEquals(bmImpl, bmFromNotABean);
        // classes in JAR lib should have the same BM Impl
        Assert.assertEquals(bmFromSomeBean, bmFromNotABean);
    }
}
