/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.serviceloader;

import java.util.ServiceLoader;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.modules.Module;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
@ExtendWith(ArquillianExtension.class)
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
        Assertions.assertEquals("#TestService#Hello", s);
    }


}
