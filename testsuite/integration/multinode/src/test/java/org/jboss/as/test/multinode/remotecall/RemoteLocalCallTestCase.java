/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.multinode.remotecall;

import static org.jboss.as.test.shared.PermissionUtils.createFilePermission;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.security.SecurityPermission;
import java.util.Arrays;
import jakarta.ejb.EJBException;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing multinode communication - calling Stateless and Stateful beans (via remote and local and home interfaces).
 *
 * @author William DeCoste, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class RemoteLocalCallTestCase {
    private static final Logger log = Logger.getLogger(RemoteLocalCallTestCase.class);
    public static final String ARCHIVE_NAME_CLIENT = "remotelocalcall-test-client";
    public static final String ARCHIVE_NAME_SERVER = "remotelocalcall-test-server";

    @BeforeClass
    public static void printSysProps() {
        log.trace("System properties:\n" + System.getProperties());
    }

    @Deployment(name = "server")
    @TargetsContainer("multinode-server")
    public static Archive<?> deployment0() {
        JavaArchive jar = createJar(ARCHIVE_NAME_SERVER);
        return jar;
    }

    @Deployment(name = "client")
    @TargetsContainer("multinode-client")
    public static Archive<?> deployment1() {
        JavaArchive jar = createJar(ARCHIVE_NAME_CLIENT);
        jar.addClasses(RemoteLocalCallTestCase.class);
        jar.addAsManifestResource("META-INF/jboss-ejb-client-receivers.xml", "jboss-ejb-client.xml");
        jar.addAsManifestResource(
                createPermissionsXmlAsset(
                        createFilePermission("delete",
                                "jbossas.multinode.client", Arrays.asList("standalone", "data", "ejb-xa-recovery", "-")),
                        createFilePermission("read",
                                "jboss.home", Arrays.asList("standalone", "tmp", "auth", "-")),
                        new SecurityPermission("putProviderProperty.WildFlyElytron")),
                "permissions.xml");
        return jar;
    }

    private static JavaArchive createJar(String archiveName) {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, archiveName + ".jar");
        jar.addClasses(StatefulBean.class, StatefulLocal.class, StatefulLocalHome.class, StatefulRemote.class,
                StatefulRemoteHome.class, StatelessBean.class, StatelessLocal.class, StatelessLocalHome.class,
                StatelessRemote.class, StatelessRemoteHome.class);
        return jar;
    }

    @Test
    @OperateOnDeployment("client")
    public void testStatelessLocalFromRemote(@ArquillianResource InitialContext ctx) throws Exception {
        StatefulRemote bean = (StatefulRemote) ctx.lookup("java:module/" + StatefulBean.class.getSimpleName() + "!"
                + StatefulRemote.class.getName());
        Assert.assertNotNull(bean);

        try {
            bean.localCall();
            Assert.fail("should not be allowed to call local interface remotely");
        } catch (EJBException e) {
            // it's OK
        } catch (Exception ee) {
            if(!(ee.getCause() instanceof EJBException)) {
                Assert.fail("should be " + EJBException.class.getName());
            }
        }
    }

    @Test
    @OperateOnDeployment("client")
    public void testStatelessLocalHomeFromRemoteHome(@ArquillianResource InitialContext ctx) throws Exception {
        StatefulRemoteHome home = (StatefulRemoteHome) ctx.lookup("java:module/" + StatefulBean.class.getSimpleName() + "!"
                + StatefulRemoteHome.class.getName());
        StatefulRemote bean = home.create();
        Assert.assertNotNull(bean);

        try {
            bean.localHomeCall();
            Assert.fail("should not be allowed to call local interface remotely");
        } catch (EJBException e) {
            // it's OK
        } catch (Exception ee) {
            if(!(ee.getCause() instanceof EJBException)) {
                Assert.fail("should be " + EJBException.class.getName());
            }
        }
    }

    @Test
    @OperateOnDeployment("client")
    public void testStatelessRemoteFromRemote(@ArquillianResource InitialContext ctx) throws Exception {
        StatefulRemote bean = (StatefulRemote) ctx.lookup("java:module/" + StatefulBean.class.getSimpleName() + "!"
                + StatefulRemote.class.getName());
        Assert.assertNotNull(bean);

        int methodCount = bean.remoteCall();
        Assert.assertEquals(1, methodCount);
    }

    @Test
    @OperateOnDeployment("client")
    public void testStatelessRemoteHomeFromRemoteHome(@ArquillianResource InitialContext ctx) throws Exception {
        StatefulRemoteHome home = (StatefulRemoteHome) ctx.lookup("java:module/" + StatefulBean.class.getSimpleName() + "!"
                + StatefulRemoteHome.class.getName());
        StatefulRemote bean = home.create();
        Assert.assertNotNull(bean);

        int methodCount = bean.remoteHomeCall();
        Assert.assertEquals(1, methodCount);
    }

    @Test
    @OperateOnDeployment("client")
    public void testStatefulRemoteFromRemote(@ArquillianResource InitialContext ctx) throws Exception {
        StatelessRemote bean = (StatelessRemote) ctx.lookup("java:module/" + StatelessBean.class.getSimpleName() + "!"
                + StatelessRemote.class.getName());
        Assert.assertNotNull(bean);

        int methodCount = bean.remoteCall();
        Assert.assertEquals(1, methodCount);
    }

    @Test
    @OperateOnDeployment("client")
    public void testStatefulRemoteHomeFromRemoteHome(@ArquillianResource InitialContext ctx) throws Exception {
        StatelessRemoteHome home = (StatelessRemoteHome) ctx.lookup("java:module/" + StatelessBean.class.getSimpleName() + "!"
                + StatelessRemoteHome.class.getName());
        StatelessRemote bean = home.create();
        Assert.assertNotNull(bean);

        int methodCount = bean.remoteHomeCall();
        Assert.assertEquals(1, methodCount);
    }

    @Test
    @OperateOnDeployment("client")
    public void testStatefulLocalFromRemote(@ArquillianResource InitialContext ctx) throws Exception {
        InitialContext jndiContext = new InitialContext();
        StatelessRemote bean = (StatelessRemote) jndiContext.lookup("java:module/" + StatelessBean.class.getSimpleName() + "!"
                + StatelessRemote.class.getName());
        Assert.assertNotNull(bean);

        try {
            bean.localCall();
            Assert.fail("should not be allowed to call local interface remotely");
        } catch (EJBException e) {
            // it's OK
        } catch (Exception ee) {
            if(!(ee.getCause() instanceof EJBException)) {
                Assert.fail("should be " + EJBException.class.getName());
            }
        }
    }

    @Test
    @OperateOnDeployment("client")
    public void testStatefulLocalHomeFromRemoteHome(@ArquillianResource InitialContext ctx) throws Exception {
        InitialContext jndiContext = new InitialContext();
        StatelessRemoteHome home = (StatelessRemoteHome) jndiContext.lookup("java:module/"
                + StatelessBean.class.getSimpleName() + "!" + StatelessRemoteHome.class.getName());
        StatelessRemote bean = home.create();
        Assert.assertNotNull(bean);

        try {
            bean.localHomeCall();
            Assert.fail("should not be allowed to call local interface remotely");
        } catch (EJBException e) {
            // it's OK
        } catch (Exception ee) {
            if(!(ee.getCause() instanceof EJBException)) {
                Assert.fail("should be " + EJBException.class.getName());
            }
        }
    }
}