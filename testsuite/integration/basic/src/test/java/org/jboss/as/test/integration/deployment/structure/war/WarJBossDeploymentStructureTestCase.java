package org.jboss.as.test.integration.deployment.structure.war;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import java.io.IOException;
import java.io.InputStream;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;


/**
 * Tests parsing of jboss-deployment-structure.xml file in a deployment
 * <p/>
 * User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class WarJBossDeploymentStructureTestCase {

    private static final Logger logger = Logger.getLogger(WarJBossDeploymentStructureTestCase.class);

    @EJB(mappedName = "java:module/ClassLoadingEJB")
    private ClassLoadingEJB ejb;

    public static final String TO_BE_FOUND_CLASS_NAME = "org.jboss.as.test.integration.deployment.structure.war.Available";
    public static final String TO_BE_MISSSING_CLASS_NAME = "org.jboss.as.test.integration.deployment.structure.war.ToBeIgnored";

    @Deployment
    public static Archive<?> createDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "deployment-structure.war");
        war.addAsManifestResource(WarJBossDeploymentStructureTestCase.class.getPackage(), "jboss-all.xml", "jboss-all.xml");

        final JavaArchive jarOne = ShrinkWrap.create(JavaArchive.class, "available.jar");
        jarOne.addClass(Available.class);

        final JavaArchive ignoredJar = ShrinkWrap.create(JavaArchive.class, "ignored.jar");
        ignoredJar.addClass(ToBeIgnored.class);

        war.addClasses(ClassLoadingEJB.class, WarJBossDeploymentStructureTestCase.class);

        war.add(jarOne, "a", ZipExporter.class);
        war.add(ignoredJar, "i", ZipExporter.class);

        war.addAsWebResource(new StringAsset("Root file"), "root-file.txt");

        war.addAsManifestResource(createPermissionsXmlAsset(
                new RuntimePermission("getClassLoader"),
                new RuntimePermission("getProtectionDomain")),
                "permissions.xml");

        return war;
    }

    /**
     * Make sure the <filter> element in jboss-deployment-structure.xml is processed correctly and the
     * exclude/include is honoured
     *
     * @throws Exception
     */
    @Test
    public void testDeploymentStructureFilters() throws Exception {
        this.ejb.loadClass(TO_BE_FOUND_CLASS_NAME);

        try {
            this.ejb.loadClass(TO_BE_MISSSING_CLASS_NAME);
            Assert.fail("Unexpectedly found class " + TO_BE_MISSSING_CLASS_NAME);
        } catch (ClassNotFoundException cnfe) {
            // expected
        }
    }

    @Test
    public void testUsePhysicalCodeSource() throws ClassNotFoundException {
        Class<?> clazz = this.ejb.loadClass(TO_BE_FOUND_CLASS_NAME);
        Assert.assertTrue(clazz.getProtectionDomain().getCodeSource().getLocation().getProtocol().equals("jar"));
        Assert.assertTrue(ClassLoadingEJB.class.getProtectionDomain().getCodeSource().getLocation().getProtocol().equals("file"));
    }

    /**
     * EE.5.15, part of testsuite migration AS6->AS7 (jbas7556)
     */
    @Test
    public void testModuleName() throws Exception {
        String result = ejb.query("java:module/ModuleName");
        assertEquals("deployment-structure", result);
        result = ejb.getResourceModuleName();
        assertEquals("deployment-structure", result);
    }

    @Test
    public void testAppName() throws Exception {
        String result = ejb.query("java:app/AppName");
        assertEquals("deployment-structure", result);
        result = ejb.getResourceAppName();
        assertEquals("deployment-structure", result);
    }


    @Test
    public void testAddingRootResource() throws ClassNotFoundException, IOException {
        InputStream clazz = getClass().getClassLoader().getResourceAsStream("root-file.txt");
        try {
            byte[] data = new byte[100];
            int res;
            StringBuilder sb = new StringBuilder();
            while ((res = clazz.read(data)) > 0) {
                sb.append(new String(data, 0, res));
            }
            Assert.assertEquals("Root file", sb.toString());
        } finally {
            clazz.close();
        }
    }
}
