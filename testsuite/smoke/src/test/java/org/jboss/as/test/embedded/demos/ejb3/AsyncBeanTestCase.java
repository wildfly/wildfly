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

package org.jboss.as.test.embedded.demos.ejb3;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.annotation.Resource;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.demos.ejb3.archive.AsyncBean;
import org.jboss.as.demos.ejb3.archive.AsyncLocal;
import org.jboss.as.demos.ejb3.archive.SimpleInterceptor;
import org.jboss.as.demos.ejb3.archive.SimpleStatelessSessionBean;
import org.jboss.as.demos.ejb3.archive.SimpleStatelessSessionLocal;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testcase for testing the basic functionality of a EJB3 async methods.
 *
 * @author John Bailey
 */
@RunWith(Arquillian.class)
@Run(RunModeType.IN_CONTAINER)
public class AsyncBeanTestCase {

    @Deployment
    public static JavaArchive createDeployment() {
        // create the ejb jar
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb3-async-example.jar");
        jar.addManifestResource("archives/ejb3-example.jar/META-INF/MANIFEST.MF", "MANIFEST.MF");
        jar.addPackage(AsyncBean.class.getPackage());
        return jar;
    }

    @Test
    public void testAsync() throws Exception {
        final Context ctx = new InitialContext();
        final AsyncLocal localBean = (AsyncLocal) ctx.lookup("java:global/ejb3-async-example/" + AsyncBean.class.getSimpleName() + "!" + AsyncLocal.class.getName());
        final Future<String> echoFuture = localBean.asyncMethod("Hello World");
        final String echo = echoFuture.get();
        Assert.assertEquals("Unexpected echo message received from async bean", "Async#Hello World", echo);
    }
}
