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
package org.jboss.as.test.integration.ejb.interceptor.serverside.protocol;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.PropertyPermission;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.integration.ejb.interceptor.serverside.AbstractServerInterceptorsSetupTask;
import org.jboss.as.test.shared.integration.ejb.interceptor.serverside.InterceptorModule;
import org.jboss.as.test.integration.ejb.interceptor.serverside.SampleBean;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A test case verifying server-side interceptor execution after changing JBoss Remoting protocol.
 *
 * @author Sultan Zhantemirov
 */
@RunWith(Arquillian.class)
@ServerSetup(RemoteProtocolChangeTestCase.SetupTask.class)
public class RemoteProtocolChangeTestCase {

    private static Context ctx;

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class,  "remote-protocol-interceptor-test.jar");
        jar.addClasses(SampleBean.class, TestSuiteEnvironment.class);
        jar.addPackage(RemoteProtocolChangeTestCase.class.getPackage());
        jar.addPackage(AbstractServerInterceptorsSetupTask.class.getPackage());
        jar.addAsManifestResource(createPermissionsXmlAsset(
                new PropertyPermission("management.address", "read"),
                new PropertyPermission("node0", "read")
        ), "permissions.xml");
        return jar;
    }

    @Test
    @InSequence(1)
    public void initialCheck() throws Exception {
        final Hashtable<String, String> props = new Hashtable<>();
        props.put(Context.PROVIDER_URL, "http-remoting://" + TestSuiteEnvironment.getServerAddress() + ":8080");
        ctx = new InitialContext(props);

        final SampleBean bean1 = (SampleBean) ctx.lookup("java:module/" + SampleBean.class.getSimpleName());
        Assert.assertEquals(ProtocolSampleInterceptor.PREFIX + SampleBean.class.getSimpleName(), bean1.getSimpleName());
    }

    @Test
    @InSequence(2)
    public void otherRemoteProtocolCheck() throws Exception {
        final Hashtable<String, String> props = new Hashtable<>();
        props.put(Context.PROVIDER_URL, "remoting://" + TestSuiteEnvironment.getServerAddress() + ":8080");
        ctx = new InitialContext(props);

        final SampleBean bean2 = (SampleBean) ctx.lookup("java:module/" + SampleBean.class.getSimpleName());

        Assert.assertEquals(ProtocolSampleInterceptor.PREFIX + SampleBean.class.getSimpleName(), bean2.getSimpleName());
    }

    static class SetupTask extends AbstractServerInterceptorsSetupTask.SetupTask {
        @Override
        public List<InterceptorModule> getModules() {
            return Collections.singletonList(new InterceptorModule(
                            ProtocolSampleInterceptor.class,
                            "interceptor-module-protocol",
                            "module.xml",
                            RemoteProtocolChangeTestCase.class.getResource("module.xml"),
                            "server-side-interceptor-protocol.jar"
                    )
            );
        }
    }
}
