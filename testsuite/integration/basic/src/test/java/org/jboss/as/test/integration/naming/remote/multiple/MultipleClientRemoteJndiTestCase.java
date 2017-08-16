package org.jboss.as.test.integration.naming.remote.multiple;

import java.io.FilePermission;
import java.net.SocketPermission;
import java.net.URL;
import java.util.PropertyPermission;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.wildfly.naming.java.permission.JndiPermission;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import static java.util.concurrent.TimeUnit.SECONDS;
import org.jboss.as.test.integration.security.common.Utils;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import org.jboss.remoting3.security.RemotingPermission;
import static org.junit.Assert.assertEquals;

/**
 * Regression test for AS7-5718
 * @author jlivings@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MultipleClientRemoteJndiTestCase {

    @ArquillianResource(RunRmiServlet.class)
    @OperateOnDeployment("one")
    private URL urlOne;

    @ArquillianResource(RunRmiServlet.class)
    @OperateOnDeployment("two")
    private URL urlTwo;

    private static final Package thisPackage = MultipleClientRemoteJndiTestCase.class.getPackage();

    @Deployment(name="one")
    public static WebArchive deploymentOne() {
        return ShrinkWrap.create(WebArchive.class, "one.war")
                .addClasses(RunRmiServlet.class, MyObject.class)
                .setWebXML(thisPackage, "web.xml")
                .addAsManifestResource(thisPackage, "war-jboss-deployment-structure.xml", "jboss-deployment-structure.xml")
                .addAsManifestResource(createPermissionsXmlAsset(
                        // RunRmiServlet reads node0 system property
                        new PropertyPermission("node0", "read"),
                        // RunRmiServlet looks up for MyObject using connection through http-remoting Endpoint
                        new RemotingPermission("connect"),
                        new SocketPermission(Utils.getDefaultHost(true), "accept,connect,listen,resolve"),
                        new RuntimePermission("getClassLoader"),
                        new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read")),
                        "permissions.xml");
    }

    @Deployment(name="two")
    public static WebArchive deploymentTwo() {
        return ShrinkWrap.create(WebArchive.class, "two.war")
                .addClasses(RunRmiServlet.class, MyObject.class)
                .setWebXML(thisPackage, "web.xml")
                .addAsManifestResource(thisPackage, "war-jboss-deployment-structure.xml", "jboss-deployment-structure.xml")
                .addAsManifestResource(createPermissionsXmlAsset(
                        // RunRmiServlet reads node0 system property
                        new PropertyPermission("node0", "read"),
                        // RunRmiServlet looks up for MyObject using connection through http-remoting Endpoint
                        new RemotingPermission("connect"),
                        new SocketPermission(Utils.getDefaultHost(true), "accept,connect,listen,resolve"),
                        new RuntimePermission("getClassLoader")),
                        "permissions.xml");
    }

    @Deployment(name="binder")
    public static WebArchive deploymentThree() {
        return ShrinkWrap.create(WebArchive.class, "binder.war")
                .addClasses(BindRmiServlet.class, MyObject.class)
                .setWebXML(MultipleClientRemoteJndiTestCase.class.getPackage(), "web.xml")
                // BindRmiServlet binds java:jboss/exported/loc/stub
                .addAsManifestResource(createPermissionsXmlAsset(new JndiPermission("java:jboss/exported/loc/stub", "bind")),
                        "permissions.xml");
    }

    @Test
    public void testLifeCycle() throws Exception {
        String result1 = HttpRequest.get(urlOne.toExternalForm() + "RunRmiServlet", 1000, SECONDS);
        assertEquals("Test", result1);
        String result2 = HttpRequest.get(urlTwo.toExternalForm() + "RunRmiServlet", 1000, SECONDS);
        assertEquals("Test", result2);
    }
}
