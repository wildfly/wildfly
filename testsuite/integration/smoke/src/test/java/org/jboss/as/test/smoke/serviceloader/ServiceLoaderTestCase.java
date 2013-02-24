/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
