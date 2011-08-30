package org.jboss.as.testsuite.integration.deployment.structure;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;


/**
 * Tests parsing of jboss-deployment-structure.xml file in a deployment
 * <p/>
 * User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class JBossDeploymentStructureTestCase {

    private static final Logger logger = Logger.getLogger(JBossDeploymentStructureTestCase.class);

    @EJB(mappedName = "java:module/ClassLoadingEJB")
    private ClassLoadingEJB ejb;

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
        ear.addAsManifestResource("deployment/structure/jboss-deployment-structure.xml", "jboss-deployment-structure.xml");

        final JavaArchive jarOne = ShrinkWrap.create(JavaArchive.class, "available.jar");
        jarOne.addClass(Available.class);

        final JavaArchive ignoredJar = ShrinkWrap.create(JavaArchive.class, "ignored.jar");
        ignoredJar.addClass(ToBeIgnored.class);

        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        ejbJar.addClasses(ClassLoadingEJB.class, JBossDeploymentStructureTestCase.class);

        ear.addAsModule(jarOne);
        ear.addAsModule(ignoredJar);
        ear.addAsModule(ejbJar);

        logger.info(ear.toString(true));
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
        final String toBeFoundClassName = "org.jboss.as.testsuite.integration.deployment.structure.Available";
        this.ejb.loadClass(toBeFoundClassName);

        final String toBeMisssingClassName = "org.jboss.as.testsuite.integration.deployment.structure.ToBeIgnored";
        try {
            this.ejb.loadClass(toBeMisssingClassName);
            Assert.fail("Unexpectedly found class " + toBeMisssingClassName);
        } catch (ClassNotFoundException cnfe) {
            // expected
        }
    }

}
