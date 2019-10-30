/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.integration.ejb.packaging.war;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import javax.naming.InitialContext;
import org.junit.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing packaging ejb in war.
 *
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class WarPackagingTestCase {
    private static final Logger log = Logger.getLogger(WarPackagingTestCase.class);
    private static final String ARCHIVE_NAME = "ejbinwar";

    private static final String JAR_SUCCESS_STRING = "Hi war from jar";
    private static final String WAR_SUCCESS_STRING = "Hi jar from war";

    @ArquillianResource
    private InitialContext ctx;

    @Deployment(name = "test")
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        // classes directly in war
        war.addClasses(Servlet.class, WarBean.class, WarPackagingTestCase.class);
        war.addAsWebInfResource(WarPackagingTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        war.addAsWebInfResource(WarPackagingTestCase.class.getPackage(), "web.xml", "web.xml");
        // jar with bean interface
        JavaArchive jarInterface = ShrinkWrap.create(JavaArchive.class, "interfacelib.jar");
        jarInterface.addClass(BeanInterface.class);
        // jar with bean implementation
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jarlib.jar");
        jar.addClass(JarBean.class);

        war.addAsLibraries(jarInterface);
        war.addAsLibraries(jar);

        return war;
    }

    @Test
    public void test() throws Exception {
        JarBean jarBean = (JarBean) ctx.lookup("java:module/" + JarBean.class.getSimpleName() + "!" + JarBean.class.getName());
        Assert.assertEquals(JAR_SUCCESS_STRING, jarBean.checkMe());
        jarBean = (JarBean) ctx.lookup("java:global/" + ARCHIVE_NAME + "/" + JarBean.class.getSimpleName() + "!"
                + JarBean.class.getName());
        Assert.assertEquals(JAR_SUCCESS_STRING, jarBean.checkMe());

        WarBean warBean = (WarBean) ctx.lookup("java:module/" + WarBean.class.getSimpleName() + "!" + WarBean.class.getName());
        Assert.assertEquals(WAR_SUCCESS_STRING, warBean.checkMe());
        warBean = (WarBean) ctx.lookup("java:module/" + WarBean.class.getSimpleName() + "!" + WarBean.class.getName());
        Assert.assertEquals(WAR_SUCCESS_STRING, warBean.checkMe());
    }

    @Test
    @RunAsClient
    public void testServletCall(@ArquillianResource @OperateOnDeployment("test") URL baseUrl) throws Exception {
        String url = "http://" + baseUrl.getHost() + ":" + baseUrl.getPort() + "/ejbinwar/servlet?archive=jar";
        log.trace(url);
        String res = HttpRequest.get(url, 2, TimeUnit.SECONDS);
        Assert.assertEquals(JAR_SUCCESS_STRING, res);
    }
}
