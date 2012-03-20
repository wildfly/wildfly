package org.jboss.as.test.integration.jsf.jta;

import javax.naming.InitialContext;
import javax.transaction.UserTransaction;

import junit.framework.Assert;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Base class for jsf.jta tests.
 * 
 * @author baranowb
 * 
 */
public class JTATestsBase {

    protected static WebArchive createArchive(String deploymentName, Class[] classes, Package[] packages,
            String resourceBase, String[] resources, String[] webInfResources) {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, deploymentName);
        if (classes != null)
            archive.addClasses(classes);

        archive.addClass(JTATestsBase.class);
        if (packages != null)
            archive.addPackages(true, packages);

        if (resources != null)            
            for (String resource : resources) {

                archive.addAsWebResource((resourceBase + "/" + resource), resource);
            }

        if (webInfResources != null)
            for (String webInfResource : webInfResources) {

                archive.addAsWebInfResource((resourceBase + "/WEB-INF/" + webInfResource), webInfResource);
            }

        return archive;

    }

    public static final String NAME = "java:comp/UserTransaction";

    public static void doLookupTest() {
        // FIXME: this will actaully make fail whole app to load, causing 500 response to initial get.
        try {
            InitialContext ic = new InitialContext();
            Object o = ic.lookup(NAME);
            Assert.assertNotNull(o);
            Assert.assertTrue(o instanceof UserTransaction);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
