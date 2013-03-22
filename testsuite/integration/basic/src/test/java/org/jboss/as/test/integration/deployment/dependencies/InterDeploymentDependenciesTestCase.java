package org.jboss.as.test.integration.deployment.dependencies;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests inter deployment dependencies
 */
@RunWith(Arquillian.class)
public class InterDeploymentDependenciesTestCase {


    @Deployment(name = "testDeployment")
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "test.jar")
                .addClasses(InterDeploymentDependenciesTestCase.class, StringView.class);
    }

    @Deployment(name = "dependee", managed = false, testable = false)
    public static Archive<?> dependee() {
        return ShrinkWrap.create(JavaArchive.class, "dependee.jar")
                .addClasses(DependeeEjb.class, StringView.class);
    }

    @Deployment(name = "dependent", managed = false, testable = false)
    public static Archive<?> dependent() {
        return ShrinkWrap.create(JavaArchive.class, "dependent.jar")
                .addClasses(DependentEjb.class, StringView.class)
                .addAsManifestResource(InterDeploymentDependenciesTestCase.class.getPackage(), "jboss-all.xml", "jboss-all.xml");
    }

    @ArquillianResource
    public Deployer deployer;

    @Test
    public void testDeploymentDependencies() throws NamingException {
        try {
            boolean failed = true;
            try {
                deployer.deploy("dependent");
                failed = false;
            } catch (Exception e) {

            } finally {
                deployer.undeploy("dependent");
            }
            if (!failed) {
                Assert.fail("Deployment did not fail");
            }
            deployer.deploy("dependee");
            deployer.deploy("dependent");

            StringView ejb = (StringView) new InitialContext().lookup("java:global/dependent/DependentEjb");
            Assert.assertEquals("hello", ejb.getString());

        } finally {
            deployer.undeploy("dependent");
            deployer.undeploy("dependee");
        }

    }

    @Test
    public void testDeploymentDependenciesWithRestart() throws NamingException {
        try {
            boolean failed = true;
            try {
                deployer.deploy("dependent");
                failed = false;
            } catch (Exception e) {

            } finally {
                deployer.undeploy("dependent");
            }
            if (!failed) {
                Assert.fail("Deployment did not fail");
            }
            deployer.deploy("dependee");
            deployer.deploy("dependent");

            StringView ejb = (StringView) new InitialContext().lookup("java:global/dependent/DependentEjb");
            Assert.assertEquals("hello", ejb.getString());

            deployer.undeploy("dependee");
            deployer.deploy("dependee");

            ejb = (StringView) new InitialContext().lookup("java:global/dependent/DependentEjb");
            Assert.assertEquals("hello", ejb.getString());

        } finally {
            deployer.undeploy("dependent");
            deployer.undeploy("dependee");
        }

    }

}
