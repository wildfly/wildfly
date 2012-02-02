package org.jboss.as.test.integration.jsf.jta;

import java.io.InputStream;

import javax.naming.InitialContext;
import javax.transaction.UserTransaction;

import junit.framework.Assert;

import org.jboss.osgi.testing.ManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
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
            for (Class c : classes) {
                archive.addClass(c);
            }

        archive.addClass(JTATestsBase.class);
        if (packages != null)
            for (Package p : packages) {
                archive.addPackage(p);
            }

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        if (resources != null)
            for (String resource : resources) {
                archive.addAsWebResource(tccl.getResource(resourceBase + "/" + resource), resource);
            }

        if (webInfResources != null)
            for (String webInfResource : webInfResources) {
                archive.addAsWebInfResource(tccl.getResource(resourceBase + "/WEB-INF/" + webInfResource), webInfResource);
            }

        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                StringBuffer dependencies = new StringBuffer();

                dependencies.append("org.jboss.jsfunit.core,net.sourceforge.htmlunit");
                builder.addManifestHeader("Dependencies", dependencies.toString());
                return builder.openStream();
            }
        });

        return archive;

    }

    private static final String NAME = "java:comp/UserTransaction";

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
