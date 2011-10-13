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
package org.jboss.as.test.smoke.embedded.demos.rar;

import javax.naming.InitialContext;

import junit.framework.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.demos.rar.archive.HelloWorldConnection;
import org.jboss.as.demos.rar.archive.HelloWorldConnectionFactory;
import org.jboss.as.test.smoke.modular.utils.PollingUtils;
import org.jboss.as.test.smoke.modular.utils.ShrinkWrapUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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

    @Deployment
    public static Archive<?> getDeployment(){
        JavaArchive archive = ShrinkWrapUtils.createJavaArchive("demos/rar-example.rar", RarTestCase.class.getPackage(), HelloWorldConnection.class.getPackage());
        archive.addClass(PollingUtils.class);
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
        InitialContext context = new InitialContext();
        //HelloWorldConnectionFactory factory = (HelloWorldConnectionFactory)context.lookup(JNDI_NAME);
        PollingUtils.JndiLookupTask task = new PollingUtils.JndiLookupTask(context, JNDI_NAME);
        PollingUtils.retryWithTimeout(10000, task);
        HelloWorldConnectionFactory factory = task.getResult(HelloWorldConnectionFactory.class);

        HelloWorldConnection conn = factory.getConnection();
        if (conn == null) {
            throw new RuntimeException("No connection");
        }
        return conn;
    }
}
