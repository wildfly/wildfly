package org.jboss.as.test.integration.naming.remote.multiple;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;

import java.io.FilePermission;
import java.net.URL;
import java.util.PropertyPermission;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.naming.java.permission.JndiPermission;

/**
 * Regression test for AS7-5718
 *
 * @author jlivings@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class NestedRemoteContextTestCase {
    @ArquillianResource(CallEjbServlet.class)
    private URL callEjbUrl;

    private static final Package thisPackage = NestedRemoteContextTestCase.class.getPackage();

    @Deployment
    public static EnterpriseArchive deploymentTwo() {
        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "ejb.jar")
                .addClasses(MyEjbBean.class, MyEjb.class, MyObject.class);

        WebArchive war = ShrinkWrap.create(WebArchive.class, "web.war")
                .addClasses(CallEjbServlet.class, MyObject.class)
                .setWebXML(thisPackage, "web.xml");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "ejb.ear")
                .addAsModule(ejbJar)
                .addAsModule(war)
                .addAsManifestResource(thisPackage, "ear-jboss-deployment-structure.xml", "jboss-deployment-structure.xml")
                .addAsManifestResource(createPermissionsXmlAsset(
                        // CallEjbServlet reads node0 system property
                        new PropertyPermission("node0", "read"),
                        new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read")),
                        "permissions.xml");
        return ear;
    }

    @Deployment(name = "binder")
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
        String result = HttpRequest.get(callEjbUrl.toExternalForm() + "CallEjbServlet", 1000, SECONDS);
        assertEquals("TestHello", result);
    }
}
