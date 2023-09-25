/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.serviceloader;

import java.util.ServiceLoader;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.modules.Module;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
@RunWith(Arquillian.class)
public class ServiceLoaderTestCase {


    @Deployment
    public static Archive<?> getDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "serviceloader-example.jar");
        jar.addAsServiceProvider(TestService.class, TestServiceImpl.class);
        jar.addPackage(ServiceLoaderTestCase.class.getPackage());
        return jar;
    }

    @Test
    public void decorateWithServiceLoader() {
        Module module = Module.forClass(TestService.class);
        ServiceLoader<TestService> loader = module.loadService(TestService.class);
        String s = "Hello";
        for (TestService service : loader) {
            s = service.decorate(s);
        }
        Assert.assertEquals("#TestService#Hello", s);
    }


}
