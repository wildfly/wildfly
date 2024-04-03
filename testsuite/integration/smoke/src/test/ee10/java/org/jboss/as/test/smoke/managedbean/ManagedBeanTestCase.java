/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.managedbean;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Thomas.Diesler@jboss.com
 */
@RunWith(Arquillian.class)
public class ManagedBeanTestCase {

    @ArquillianResource
    private InitialContext context;

    @Deployment
    public static EnterpriseArchive createDeployment() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "managedbean-example.jar");
        jar.addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        jar.addPackage(SimpleManagedBean.class.getPackage());
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "managedbean-example.ear");
        ear.addAsModule(jar);
        return ear;
    }

    @Test
    public void testManagedBean() throws Exception {
        BeanWithSimpleInjected bean = (BeanWithSimpleInjected) context.lookup("java:module/" + BeanWithSimpleInjected.class.getSimpleName());
        Assert.assertNotNull(bean.getSimple());
        Assert.assertNotNull(bean.getSimple2());
        String s = bean.echo("Hello");
        Assert.assertEquals("#InterceptorFromParent##InterceptorBean##OtherInterceptorBean##CDIInterceptor##BeanParent##BeanWithSimpleInjected#Hello#CDIBean#CDIBean", s);
        Assert.assertEquals(100, bean.getNumber());
        Assert.assertEquals("value", bean.getValue());
        Assert.assertEquals("value", bean.getValue2());
    }
}
