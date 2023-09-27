/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
