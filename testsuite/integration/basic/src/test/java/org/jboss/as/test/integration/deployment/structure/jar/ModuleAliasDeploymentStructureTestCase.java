package org.jboss.as.test.integration.deployment.structure.jar;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests a module alias for the root deployment
 * <p/>
 * @author Thomas.Diesler@jboss.com
 * @since 23-May-2013
 */
@RunWith(Arquillian.class)
public class ModuleAliasDeploymentStructureTestCase {

    private static final String TEST_ONE_JAR = "test-one.jar";

    @ArquillianResource
    Deployer deployer;

    @Deployment
    public static Archive<?> createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "deployment-structure-tests");
        return archive;
    }

    @Test
    public void testModuleAlias() throws Exception {
        deployer.deploy(TEST_ONE_JAR);
        try {
            ModuleLoader moduleLoader = Module.getCallerModuleLoader();
            Module module = moduleLoader.loadModule(ModuleIdentifier.create("org.acme.foo"));
            String className = getClass().getPackage().getName() + ".Available";
            Object obj = module.getClassLoader().loadClass(className).newInstance();
            Assert.assertNotNull("Object not null", obj);
        } finally {
            deployer.undeploy(TEST_ONE_JAR);
        }
    }

    @Deployment(name = TEST_ONE_JAR, managed = false, testable = false)
    public static JavaArchive getArchiveOne() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, TEST_ONE_JAR);
        archive.addAsManifestResource(Available.class.getPackage(), "module-alias-deployment-structure.xml", "jboss-deployment-structure.xml");
        archive.add(getNestedArchive(), "/", ZipExporter.class);
        return archive;
    }

    private static JavaArchive getNestedArchive() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "nested.jar");
        archive.addClasses(Available.class);
        return archive;
    }
}
