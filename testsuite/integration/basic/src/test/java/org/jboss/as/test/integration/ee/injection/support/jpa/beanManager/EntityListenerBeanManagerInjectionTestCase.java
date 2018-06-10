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

package org.jboss.as.test.integration.ee.injection.support.jpa.beanManager;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.io.FilePermission;
import java.net.URI;

import javax.enterprise.inject.spi.CDI;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class EntityListenerBeanManagerInjectionTestCase {

    private static final String ARCHIVE_NAME = "test";

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        war.addPackage(EntityListenerBeanManagerInjectionTestCase.class.getPackage());
        war.addAsWebInfResource(EntityListenerBeanManagerInjectionTestCase.class.getPackage(), "persistence.xml",
                "classes/META-INF/persistence.xml");
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        war.addAsManifestResource(createPermissionsXmlAsset(
                new FilePermission(getCdiJarUrl(), "read")
        ), "jboss-permissions.xml");
        return war;
    }

    private static String getCdiJarUrl() {
        String url = CDI.class.getClassLoader().getResource("META-INF/services/javax.enterprise.inject.spi.CDIProvider").getPath();
        return URI.create(url.substring(0, url.lastIndexOf('!'))).getPath();
    }

    @ArquillianResource
    private static InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType
                .cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    @Test
    public void testBeanManagerInEntityListenerCallbacks(Foo foo, Bar bar) throws NamingException {
        TestBean bean = lookup("TestBean", TestBean.class);
        bean.createEmployee("Joe Black", "Brno 2", 20);
        bean.updateEmployee(20);
        bean.removeEmployee(20);
        // PostLoad call back is called twice
        Assert.assertEquals(8, foo.getCounter().get());
        Assert.assertEquals(8, bar.getCounter().get());
    }
}
