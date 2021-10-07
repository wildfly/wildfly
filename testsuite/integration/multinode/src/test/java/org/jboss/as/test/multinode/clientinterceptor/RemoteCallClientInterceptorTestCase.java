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

package org.jboss.as.test.multinode.clientinterceptor;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.io.File;
import java.io.FilePermission;
import java.security.SecurityPermission;
import java.util.Collections;
import java.util.List;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
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
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
@RunWith(Arquillian.class)
@ServerSetup({RemoteCallClientInterceptorTestCase.SetupTask.class})
public class RemoteCallClientInterceptorTestCase {

    private static final String ARCHIVE_NAME_CLIENT = "remotelocalcall-test-client";
    private static final String ARCHIVE_NAME_SERVER = "remotelocalcall-test-server";

    private static final String moduleName = "remote-call-interceptor-module";

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
        jar.addClasses(RemoteCallClientInterceptorTestCase.class, ClientInterceptor.class);
        jar.addPackage(AbstractClientInterceptorsSetupTask.class.getPackage());
        jar.addAsManifestResource("META-INF/jboss-ejb-client-receivers.xml", "jboss-ejb-client.xml");
        jar.addAsManifestResource(
                createPermissionsXmlAsset(
                        new SecurityPermission("putProviderProperty.WildFlyElytron"),
                        new FilePermission(System.getProperty("jboss.home") + File.separatorChar + "standalone" + File.separatorChar + "tmp" + File.separatorChar + "auth" + File.separatorChar + "-", "read")),
                "permissions.xml");
        return jar;
    }

    @Test
    @OperateOnDeployment("client")
    public void testStateless() throws Exception {
        StatelessRemote bean = ClientInterceptorUtil.lookupStatelessRemote(ARCHIVE_NAME_SERVER, StatelessBean.class, StatelessRemote.class);
        Assert.assertNotNull(bean);

        int methodCount = bean.method();
        Assert.assertEquals(1, methodCount);
        Assert.assertEquals(0, ClientInterceptor.invocationLatch.getCount());
        Assert.assertEquals(0, ClientInterceptor.resultLatch.getCount());
    }

    static class SetupTask extends AbstractClientInterceptorsSetupTask.SetupTask {
        @Override
        public List<InterceptorModule> getModules() {
            return Collections.singletonList(new InterceptorModule(
                    ClientInterceptor.class,
                    moduleName,
                    "module.xml",
                    RemoteCallClientInterceptorTestCase.class.getResource("module.xml"),
                    "client-side-interceptor.jar"
            ));
        }
    }
}