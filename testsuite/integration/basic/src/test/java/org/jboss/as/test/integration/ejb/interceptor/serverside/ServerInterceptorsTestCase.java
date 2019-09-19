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
package org.jboss.as.test.integration.ejb.interceptor.serverside;

import static org.jboss.as.controller.client.helpers.ClientConstants.NAME;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP_ADDR;
import static org.jboss.as.controller.client.helpers.ClientConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.RESULT;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUBSYSTEM;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertTrue;
import java.io.FilePermission;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.integration.ejb.interceptor.serverside.AbstractServerInterceptorsSetupTask;
import org.jboss.as.test.shared.integration.ejb.interceptor.serverside.InterceptorModule;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.security.RemotingPermission;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

/**
 * A test case verifying an ability of adding a server-side configured interceptor without changing deployments.
 * See https://issues.jboss.org/browse/WFLY-6143 for more details.
 *
 * @author <a href="mailto:szhantem@redhat.com">Sultan Zhantemirov</a> (c) 2019 Red Hat, inc.
 */
@RunWith(Arquillian.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ServerSetup(ServerInterceptorsTestCase.SetupTask.class)
public class ServerInterceptorsTestCase {

    @ArquillianResource
    private ManagementClient managementClient;

    private static final String moduleName = "interceptor-module";
    private static final int TIMEOUT = 5;

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "test-server-interceptor.jar");
        jar.addPackage(ServerInterceptorsTestCase.class.getPackage());
        jar.addPackage(AbstractServerInterceptorsSetupTask.class.getPackage());
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client," +
                "org.jboss.dmr," + "org.jboss.remoting3\n"), "MANIFEST.MF");
        jar.addAsManifestResource(createPermissionsXmlAsset(
                new RemotingPermission("connect"),
                new RemotingPermission("createEndpoint"),
                new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read")
        ), "permissions.xml");
        return jar;
    }

    @Test
    public void serverInterceptorAttributeReadCheck() throws Exception {
        // /subsystem=ejb3:read-attribute(name=server-interceptors)
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).set(SUBSYSTEM, "ejb3");
        op.get(NAME).set("server-interceptors");

        final ModelNode operationResult = managementClient.getControllerClient().execute(op);
        assertTrue(operationResult.get(RESULT).asString().contains(moduleName));
    }

    @Test
    public void serverInterceptorExecutionCheck() throws NamingException {
        final InitialContext ctx = new InitialContext();
        SampleBean bean = (SampleBean) ctx.lookup("java:module/" + SampleBean.class.getSimpleName());

        // call bean method in order to execute interceptor's method
        bean.getSimpleName();

        try {
            // waiting for the interceptor's aroundInvoke method execution
            ServerInterceptor.latch.await(TIMEOUT, TimeUnit.SECONDS);
        }
        catch (InterruptedException ie){
            throw new RuntimeException("latch.await() has been interrupted", ie);
        }
        Assert.assertEquals(0, ServerInterceptor.latch.getCount());
    }

    @Test
    public void serverInterceptorTimerExecutionCheck() throws NamingException {
        InitialContext ctx = new InitialContext();
        ScheduleBean schedulesBean = (ScheduleBean) ctx.lookup("java:module/" + ScheduleBean.class.getSimpleName());

        // call bean method in order to execute interceptor's method
        schedulesBean.getTimerInfo();

        try {
            // waiting for the interceptor's aroundTimeout method execution
            ServerInterceptor.timeoutLatch.await(TIMEOUT, TimeUnit.SECONDS);
        }
        catch (InterruptedException ie){
            throw new RuntimeException("timeoutLatch.await() has been interrupted", ie);
        }
        Assert.assertEquals(0, ServerInterceptor.timeoutLatch.getCount());
    }

    static class SetupTask extends AbstractServerInterceptorsSetupTask.SetupTask {
        @Override
        public List<InterceptorModule> getModules() {
            return Collections.singletonList(new InterceptorModule(
                    ServerInterceptor.class,
                    moduleName,
                    "module.xml",
                    ServerInterceptorsTestCase.class.getResource("module.xml"),
                    "server-side-interceptor.jar"
                    )
            );
        }
    }

}
