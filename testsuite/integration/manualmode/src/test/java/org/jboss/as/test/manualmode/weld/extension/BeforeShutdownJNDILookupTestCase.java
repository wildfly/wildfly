package org.jboss.as.test.manualmode.weld.extension;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.inject.spi.Extension;
import java.io.File;
import java.io.FilePermission;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * A test to ensure that the UserTransaction and TransactionSynchronizationRegistry can be retrieved via JNDI when
 * an extensions BeforeShutdown method is invoked.
 * <p/>
 * See WFLY-5232
 *
 * @author Ryan Emerson
 */
@RunWith(Arquillian.class)
@RunAsClient
public class BeforeShutdownJNDILookupTestCase {

    public static final String TEST_URL = "target" + File.separator + "results.txt";

    private static final String CONTAINER = "default-jbossas";
    private static final String DEPLOYMENT = "test.war";
    private static final Path TEST_PATH = Paths.get("", TEST_URL);

    @Deployment(name = DEPLOYMENT, managed = true)
    @TargetsContainer(CONTAINER)
    public static Archive<?> deploy() throws Exception {
        return ShrinkWrap
                .create(WebArchive.class, DEPLOYMENT)
                .addClasses(BeforeShutdownJNDILookupTestCase.class, BeforeShutdownExtension.class)
                .add(EmptyAsset.INSTANCE, ArchivePaths.create("WEB-INF/beans.xml"))
                .add(new StringAsset(BeforeShutdownExtension.class.getName()), "META-INF/services/" + Extension.class.getName())
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(
                        new FilePermission(TEST_PATH.getParent().toString(), "read, write"),
                        new FilePermission(TEST_PATH.toString(), "read, write, delete")
                ), "permissions.xml");
    }

    @ArquillianResource
    ContainerController controller;

    @Test
    public void testTransactionJNDILookupDuringShutdownEvent() throws Exception {
        controller.start(CONTAINER);
        controller.kill(CONTAINER);

        List<String> output = Files.readAllLines(TEST_PATH);
        if (output.get(0).equals("Exception")) {
            String stacktrace = output.get(1).replaceAll(",", System.getProperty("line.separator"));
            String msg = "An exception was thrown by the deployment %s during shutdown.  The server stacktrace is shown below: %n%s";
            Assert.fail(String.format(msg, DEPLOYMENT, stacktrace));
        }
        assertEquals("Contents of result.txt is not valid!", "UserTransaction", output.get(0));
    }

    @AfterClass
    public static void cleanup() throws Exception {
        Files.delete(TEST_PATH);
    }
}
