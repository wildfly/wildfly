/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.interceptor.serverside.multiple;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Arrays;
import java.util.List;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.integration.ejb.interceptor.serverside.AbstractServerInterceptorsSetupTask;
import org.jboss.as.test.shared.integration.ejb.interceptor.serverside.InterceptorModule;
import org.jboss.as.test.integration.ejb.interceptor.serverside.SampleBean;
import org.jboss.as.test.integration.ejb.interceptor.serverside.ServerInterceptor;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A test case verifying:
 *  1. Multiple server-side interceptors execution
 *  2. An exception thrown in an interceptor is propagated.
 * See https://issues.jboss.org/browse/WFLY-6143 for more details.
 *
 * @author <a href="mailto:szhantem@redhat.com">Sultan Zhantemirov</a> (c) 2019 Red Hat, inc.
 */
@RunWith(Arquillian.class)
@ServerSetup(MultipleInterceptorsTestCase.SetupTask.class)
public class MultipleInterceptorsTestCase {

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "test-multiple-server-interceptor.jar");
        jar.addClass(SampleBean.class);
        jar.addPackage(MultipleInterceptorsTestCase.class.getPackage());
        jar.addPackage(AbstractServerInterceptorsSetupTask.class.getPackage());
        return jar;
    }

    @Test
    public void multipleInterceptorsCheck() throws NamingException {
        final InitialContext ctx = new InitialContext();
        SampleBean sampleBean = (SampleBean) ctx.lookup("java:module/" + SampleBean.class.getSimpleName());

        try {
            sampleBean.getSimpleName();
            fail("Should have thrown an IllegalArgumentException");
        }
        catch (Exception iae) {
            assertTrue(iae instanceof IllegalArgumentException);
        }

        Assert.assertEquals(0, ServerInterceptor.latch.getCount());
    }

    static class SetupTask extends AbstractServerInterceptorsSetupTask.SetupTask {

        @Override
        public List<InterceptorModule> getModules() {
            InterceptorModule firstModule = new InterceptorModule(
                    ServerInterceptor.class,
                    "interceptor-first-module",
                    "first-module.xml",
                    MultipleInterceptorsTestCase.class.getResource("first-module.xml"),
                    "first-interceptor.jar"
            );
            InterceptorModule secondModule = new InterceptorModule(
                    ExceptionThrowingInterceptor.class,
                    "interceptor-second-module",
                    "second-module.xml",
                    MultipleInterceptorsTestCase.class.getResource("second-module.xml"),
                    "second-interceptor.jar"
            );

            return Arrays.asList(firstModule, secondModule);
        }
    }
}
