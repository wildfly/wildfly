package org.jboss.as.test.integration.deployment.structure.jar;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
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

    @Deployment
    public static Archive<?> createDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "deployment-structure.jar");
        jar.addAsManifestResource(ModuleAliasDeploymentStructureTestCase.class.getPackage(), "module-alias-deployment-structure.xml", "jboss-deployment-structure.xml");
        JavaArchive jarOne = ShrinkWrap.create(JavaArchive.class, "available.jar");
        jarOne.addClass(Available.class);
        jar.add(jarOne, "/", ZipExporter.class);
        return jar;
    }

    @Test
    public void testModuleAlias() throws Exception {
        ModuleLoader moduleLoader = ((ModuleClassLoader)getClass().getClassLoader()).getModule().getModuleLoader();
        Module module = moduleLoader.loadModule(ModuleIdentifier.create("org.acme.foo"));
        Assert.assertNotNull("Module not null", module);
        String className = getClass().getPackage().getName() + ".Available";
        Object obj = module.getClassLoader().loadClass(className).newInstance();
        Assert.assertNotNull("Object not null", obj);
    }
}
