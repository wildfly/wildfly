/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.managedbean;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Thomas.Diesler@jboss.com
 */
@ExtendWith(ArquillianExtension.class)
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
        Assertions.assertNotNull(bean.getSimple());
        Assertions.assertNotNull(bean.getSimple2());
        String s = bean.echo("Hello");
        Assertions.assertEquals("#InterceptorFromParent##InterceptorBean##OtherInterceptorBean##CDIInterceptor##BeanParent##BeanWithSimpleInjected#Hello#CDIBean#CDIBean", s);
        Assertions.assertEquals(100, bean.getNumber());
        Assertions.assertEquals("value", bean.getValue());
        Assertions.assertEquals("value", bean.getValue2());
    }
}
