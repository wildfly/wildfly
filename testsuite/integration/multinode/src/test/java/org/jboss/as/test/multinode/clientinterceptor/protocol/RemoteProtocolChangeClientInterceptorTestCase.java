/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.multinode.clientinterceptor.protocol;

import static org.jboss.as.test.shared.TestSuiteEnvironment.getSystemProperty;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.io.File;
import java.io.FilePermission;
import java.security.SecurityPermission;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.PropertyPermission;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.multinode.clientinterceptor.StatelessBean;
import org.jboss.as.test.multinode.clientinterceptor.StatelessRemote;
import org.jboss.as.test.shared.TestSuiteEnvironment;
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
 * A test case verifying client-side interceptor execution after changing JBoss Remoting protocol.
 *
 * @author Sultan Zhantemirov
 */
@RunWith(Arquillian.class)
@ServerSetup(RemoteProtocolChangeClientInterceptorTestCase.SetupTask.class)
public class RemoteProtocolChangeClientInterceptorTestCase {

    private static final String ARCHIVE_NAME_CLIENT = "remote-protocol-test-client";
    private static final String ARCHIVE_NAME_SERVER = "remote-protocol-test-server";

    private static final String moduleName = "protocol-change-interceptor-module";

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
        jar.addClasses(StatelessBean.class, StatelessRemote.class, ProtocolSampleClientInterceptor.class);
        jar.addClasses(RemoteProtocolChangeClientInterceptorTestCase.class, TestSuiteEnvironment.class);
        jar.addPackage(AbstractClientInterceptorsSetupTask.class.getPackage());
        jar.addAsManifestResource(RemoteProtocolChangeClientInterceptorTestCase.class.getPackage(), "jboss-ejb-client.xml", "jboss-ejb-client.xml");
        jar.addAsManifestResource(
                createPermissionsXmlAsset(
                        new SecurityPermission("putProviderProperty.WildFlyElytron"),
                        new PropertyPermission("management.address", "read"),
                        new PropertyPermission("node0", "read"),
                        new PropertyPermission("jboss.http.port", "read"),
                        new PropertyPermission("jboss.socket.binding.port-offset", "read"),
                        new FilePermission(System.getProperty("jboss.home") + File.separatorChar + "standalone" + File.separatorChar + "tmp" + File.separatorChar + "auth" + File.separatorChar + "-", "read")),

                "permissions.xml");
        return jar;
    }

    @Test
    @InSequence(1)
    @OperateOnDeployment("client")
    public void testDefaultProtocol() throws Exception {
        final Hashtable<String, String> props = new Hashtable<>();
        props.put(Context.URL_PKG_PREFIXES, "org.wildfly.naming.client");

        StatelessRemote bean = getRemote(new InitialContext(props));
        Assert.assertNotNull(bean);

        // StatelessBean.methodCount field should equal 1 after first invoking
        Assert.assertEquals(ProtocolSampleClientInterceptor.COUNT + 1, bean.method());
    }

    @Test
    @InSequence(2)
    @OperateOnDeployment("client")
    public void testHttpRemotingProtocol() throws Exception {
        final Hashtable<String, String> props = new Hashtable<>();
        props.put(Context.URL_PKG_PREFIXES, "org.wildfly.naming.client");
        props.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        props.put(Context.PROVIDER_URL, "http-remoting://" + TestSuiteEnvironment.getServerAddress() + ":"
                + (TestSuiteEnvironment.getHttpPort() + Integer.parseInt(getSystemProperty("jboss.socket.binding.port-offset", "100"))));

        StatelessRemote bean = getRemote(new InitialContext(props));
        Assert.assertNotNull(bean);

        // StatelessBean.methodCount field should equal 2 after second invoking (methodCount is a static field and is shared within a single JVM)
        Assert.assertEquals(ProtocolSampleClientInterceptor.COUNT + 2, bean.method());
    }

    private StatelessRemote getRemote(InitialContext ctx) throws NamingException {
        return (StatelessRemote) ctx.lookup("ejb:/" + RemoteProtocolChangeClientInterceptorTestCase.ARCHIVE_NAME_SERVER + "/" + "" + "/" + "StatelessBean" + "!" + StatelessRemote.class.getName());
    }

    static class SetupTask extends AbstractClientInterceptorsSetupTask.SetupTask {
        @Override
        public List<InterceptorModule> getModules() {
            return Collections.singletonList(new InterceptorModule(
                    ProtocolSampleClientInterceptor.class,
                    moduleName,
                    "module.xml",
                    RemoteProtocolChangeClientInterceptorTestCase.class.getResource("module.xml"),
                    "client-side-interceptor.jar"
            ));
        }
    }
}
