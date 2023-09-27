/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.rar;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
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
public class RarTestCase {

    private static final String JNDI_NAME = "java:/eis/HelloWorld";

    @ArquillianResource
    private InitialContext initialContext;

    @Deployment
    public static Archive<?> getDeployment(){

        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "rar-example.rar");
        archive.addPackage(RarTestCase.class.getPackage());
        archive.addAsManifestResource(RarTestCase.class.getPackage(), "ironjacamar.xml", "ironjacamar.xml");
        return archive;
    }

    @Test
    public void helloWorld() throws Exception {
        String s = getConnection().helloWorld();
        Assert.assertEquals("Hello World, AS 7 !", s);
    }

    @Test
    public void helloWorld2() throws Exception {
        String s = getConnection().helloWorld("Test");
        Assert.assertEquals("Hello World, Test !", s);
    }

    private HelloWorldConnection getConnection() throws Exception {
        HelloWorldConnectionFactory factory = (HelloWorldConnectionFactory)initialContext.lookup(JNDI_NAME);

        HelloWorldConnection conn = factory.getConnection();
        if (conn == null) {
            throw new RuntimeException("No connection");
        }
        return conn;
    }
}
