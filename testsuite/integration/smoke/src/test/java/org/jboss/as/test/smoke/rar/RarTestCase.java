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
