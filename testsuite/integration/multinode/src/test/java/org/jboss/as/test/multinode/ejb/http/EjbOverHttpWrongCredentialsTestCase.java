/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.multinode.ejb.http;

import static org.jboss.as.test.shared.PermissionUtils.createFilePermission;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.net.SocketPermission;
import java.util.Arrays;
import javax.naming.InitialContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * EJB over HTTP remote call should fail with incorrect wildfly-config.xml credentials
 *
 * @author <a href="mailto:szhantem@redhat.com">Sultan Zhantemirov</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(EjbOverHttpTestCase.EjbOverHttpTestCaseServerSetup.class)
public class EjbOverHttpWrongCredentialsTestCase {

    public static final String ARCHIVE_NAME_SERVER = "ejboverhttp-test-server";
    public static final String ARCHIVE_NAME_CLIENT_WRONG_CREDENTIALS = "ejboverhttp-test-client-wrong-credentials";
    private static final int serverPort = 8180;

    @Deployment(name = "server")
    @TargetsContainer("multinode-server")
    public static Archive<?> deployment0() {
        JavaArchive jar = createJar(ARCHIVE_NAME_SERVER);
        return jar;
    }

    @Deployment(name = "client-wrong-credentials")
    @TargetsContainer("multinode-client")
    public static Archive<?> deployment1() {
        JavaArchive jar = createClientJar();
        return jar;
    }

    private static JavaArchive createJar(String archiveName) {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, archiveName + ".jar");
        jar.addClasses(StatelessBean.class, StatelessLocal.class, StatelessRemote.class);
        return jar;
    }

    private static JavaArchive createClientJar() {
        JavaArchive jar = createJar(EjbOverHttpWrongCredentialsTestCase.ARCHIVE_NAME_CLIENT_WRONG_CREDENTIALS);
        jar.addClasses(EjbOverHttpTestCase.class);
        jar.addAsManifestResource("META-INF/jboss-ejb-client-profile.xml", "jboss-ejb-client.xml")
                .addAsManifestResource("ejb-http-wildfly-config-wrong.xml", "wildfly-config.xml")
                .addAsManifestResource(
                        createPermissionsXmlAsset(
                                createFilePermission("read,write,delete",
                                        "jbossas.multinode.client", Arrays.asList("standalone", "data", "ejb-xa-recovery", "-")),
                                new SocketPermission(TestSuiteEnvironment.formatPossibleIpv6Address(System.getProperty("node0")) + ":" + serverPort,
                                        "connect,resolve")),
                        "permissions.xml");
        return jar;
    }

    @Test
    @OperateOnDeployment("client-wrong-credentials")
    public void testBasicInvocationWithWrongCredentials(@ArquillianResource InitialContext ctx) throws Exception {
        StatelessRemote bean = (StatelessRemote) ctx.lookup("java:module/" + StatelessBean.class.getSimpleName() + "!"
                + StatelessRemote.class.getName());
        Assert.assertNotNull(bean);

        try {
            int methodCount = bean.remoteCall();
            Assert.assertEquals(EjbOverHttpTestCase.NO_EJB_RETURN_CODE, methodCount);
        } catch (javax.naming.AuthenticationException e) {
            // expected
        }
    }
}
