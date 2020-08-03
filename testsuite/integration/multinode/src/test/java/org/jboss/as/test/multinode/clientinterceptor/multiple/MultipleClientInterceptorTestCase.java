/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.multinode.clientinterceptor.multiple;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.fail;
import java.security.SecurityPermission;
import java.util.Arrays;
import java.util.List;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.multinode.clientinterceptor.ClientInterceptor;
import org.jboss.as.test.multinode.clientinterceptor.StatelessBean;
import org.jboss.as.test.multinode.clientinterceptor.StatelessRemote;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.as.test.shared.integration.interceptor.clientside.AbstractClientInterceptorsSetupTask;
import org.jboss.as.test.shared.integration.interceptor.clientside.InterceptorModule;
import org.jboss.as.test.shared.util.ClientInterceptorUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A test case verifying:
 *  1. Multiple client-side interceptors execution
 *  2. An exception thrown in an interceptor is propagated.
 * See https://issues.jboss.org/browse/WFLY-6144 for more details.
 *
 * @author <a href="mailto:szhantem@redhat.com">Sultan Zhantemirov</a> (c) 2019 Red Hat, inc.
 */
@RunWith(Arquillian.class)
@ServerSetup(MultipleClientInterceptorTestCase.SetupTask.class)
public class MultipleClientInterceptorTestCase {

    private static final String ARCHIVE_NAME_CLIENT = "multiple-interceptors-test-client";
    private static final String ARCHIVE_NAME_SERVER = "multiple-interceptors-test-server";

    private static final String firstModuleName = "interceptor-first-module";
    private static final String secondModuleName = "interceptor-second-module";

    @Deployment(name = AbstractClientInterceptorsSetupTask.DEPLOYMENT_NAME_SERVER)
    @TargetsContainer(AbstractClientInterceptorsSetupTask.TARGER_CONTAINER_SERVER)
    public static Archive<?> deployment0() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME_SERVER + ".jar");
        jar.addClasses(StatelessBean.class, StatelessRemote.class);
        return jar;
    }

    @Deployment(name = AbstractClientInterceptorsSetupTask.DEPLOYMENT_NAME_CLIENT)
    @TargetsContainer(AbstractClientInterceptorsSetupTask.TARGER_CONTAINER_CLIENT)
    public static Archive<?> deployment1() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME_CLIENT + ".jar");
        jar.addClasses(Util.class, ClientInterceptorUtil.class);
        jar.addClasses(StatelessBean.class, StatelessRemote.class);
        jar.addClasses(MultipleClientInterceptorTestCase.class, ClientInterceptor.class, ExceptionClientInterceptor.class);
        jar.addPackage(AbstractClientInterceptorsSetupTask.class.getPackage());
        jar.addAsManifestResource("META-INF/jboss-ejb-client-receivers.xml", "jboss-ejb-client.xml");
        jar.addAsManifestResource(
                createPermissionsXmlAsset(
                        new SecurityPermission("putProviderProperty.WildFlyElytron")),
                "permissions.xml");
        return jar;
    }

    @Test
    @OperateOnDeployment(AbstractClientInterceptorsSetupTask.DEPLOYMENT_NAME_CLIENT)
    public void testMultiple() throws Exception {
        StatelessRemote bean = ClientInterceptorUtil.lookupStatelessRemote(ARCHIVE_NAME_SERVER, StatelessBean.class, StatelessRemote.class);
        Assert.assertNotNull(bean);

        try {
            bean.method();
            fail("Client interceptor should have thrown an IllegalArgumentException");
        } catch (Exception e) {
            // expected
            Assert.assertTrue(e instanceof IllegalArgumentException);
            Assert.assertEquals(0, ClientInterceptor.invocationLatch.getCount());
            Assert.assertEquals(0, ClientInterceptor.resultLatch.getCount());
        }
    }

    static class SetupTask extends AbstractClientInterceptorsSetupTask.SetupTask {
        @Override
        public List<InterceptorModule> getModules() {
            InterceptorModule firstModule = new InterceptorModule(
                    ClientInterceptor.class,
                    firstModuleName,
                    "first-module.xml",
                    MultipleClientInterceptorTestCase.class.getResource("first-module.xml"),
                    "first-client-side-interceptor.jar"
            );

            InterceptorModule secondModule = new InterceptorModule(
                    ExceptionClientInterceptor.class,
                    secondModuleName,
                    "second-module.xml",
                    MultipleClientInterceptorTestCase.class.getResource("second-module.xml"),
                    "second-client-side-interceptor.jar"
            );

            return Arrays.asList(firstModule, secondModule);

        }
    }
}
