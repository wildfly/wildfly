package org.jboss.as.test.integration.deployment.structure.ear;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;

import javax.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests parsing of jboss-deployment-structure.xml file in a deployment
 * <p/>
 * User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class EarJBossDeploymentStructureTestCase {

    private static final Logger logger = Logger.getLogger(EarJBossDeploymentStructureTestCase.class);

    @EJB(mappedName = "java:module/ClassLoadingEJB")
    private ClassLoadingEJB ejb;

    public static final String TO_BE_FOUND_CLASS_NAME = "org.jboss.as.test.integration.deployment.structure.ear.Available";
    public static final String TO_BE_MISSSING_CLASS_NAME = "org.jboss.as.test.integration.deployment.structure.ear.ToBeIgnored";
    public static final String METAINF_RESOURCE_TXT = "aa/metainf-resource.txt";

    /**
     * .ear
     * |
     * |--- META-INF
     * |       |
     * |       |--- jboss-deployment-structure.xml
     * |
     * |--- ejb.jar
     * |
     * |--- available.jar
     * |
     * |--- ignored.jar
     *
     * @return
     */
    @Deployment
    public static Archive<?> createDeployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "deployment-structure.ear");
        ear.addAsManifestResource(EarJBossDeploymentStructureTestCase.class.getPackage(), "jboss-deployment-structure.xml", "jboss-deployment-structure.xml");

        final JavaArchive jarOne = ShrinkWrap.create(JavaArchive.class, "available.jar");
        jarOne.addClass(Available.class);
        jarOne.addAsManifestResource(new StringAsset("test resource"), METAINF_RESOURCE_TXT);

        final JavaArchive ignoredJar = ShrinkWrap.create(JavaArchive.class, "ignored.jar");
        ignoredJar.addClass(ToBeIgnored.class);

        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        ejbJar.addClasses(ClassLoadingEJB.class, EarJBossDeploymentStructureTestCase.class);
        // grant necessary permissions
        ejbJar.addAsResource(createPermissionsXmlAsset(new RuntimePermission("getProtectionDomain")), "META-INF/jboss-permissions.xml");

        ear.addAsModule(jarOne);
        ear.addAsModule(ignoredJar);
        ear.addAsModule(ejbJar);

        return ear;
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
        Assert.assertTrue(ClassLoadingEJB.class.getProtectionDomain().getCodeSource().getLocation().getProtocol().equals("jar"));
    }

    @Test
    public void testMetaInfResourceImported() {
        Assert.assertTrue(this.ejb.hasResource("/META-INF/" + METAINF_RESOURCE_TXT));
    }

    /**
     * EE.5.15, part of testsuite migration AS6->AS7 (jbas7556)
     */
    @Test
    public void testModuleName() throws Exception {
        String result = ejb.query("java:module/ModuleName");
        assertEquals("ejb", result);
        result = ejb.getResourceModuleName();
        assertEquals("ejb", result);
    }

    @Test
    public void testAppName() throws Exception {
        String result = ejb.query("java:app/AppName");
        assertEquals("deployment-structure", result);
        result = ejb.getResourceAppName();
        assertEquals("deployment-structure", result);
    }
}
