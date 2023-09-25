/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
