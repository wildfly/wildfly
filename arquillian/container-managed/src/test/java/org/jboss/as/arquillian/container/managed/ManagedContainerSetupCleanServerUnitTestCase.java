package org.jboss.as.arquillian.container.managed;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author ggrossetie
 * @since 11/02/13
 */
@Ignore
public class ManagedContainerSetupCleanServerUnitTestCase {

    private File jbossHome = new File(createRootDir(), "jboss-home");

    private String defaultServerBaseDir = createServer(jbossHome, "standalone", 1).getAbsolutePath();
    private File alternativeServer = createServer(createRootDir(), "standalone2", 2);

    private File tempDir = new File(System.getProperty("java.io.tmpdir"));
    private File cleanServerBaseDir = new File(tempDir, ManagedDeployableContainer.TEMP_CONTAINER_DIRECTORY);

    private File userCleanServerBaseDir = createUserCleanServerBaseDir();

    @Test
    public void testDefaultServerBaseDir() throws Exception {
        ManagedDeployableContainer.setupCleanServerDirectories(defaultServerBaseDir, jbossHome.toString(), null);
        assertCleanServerBaseDir(cleanServerBaseDir, 1);
    }

    @Test
    public void testAlternativeServerBaseDir() throws Exception {
        ManagedDeployableContainer.setupCleanServerDirectories(".." + File.separatorChar + alternativeServer.getName(), jbossHome.toString(), null);
        assertCleanServerBaseDir(cleanServerBaseDir, 2);
    }

    @Test
    public void testUserCleanServerBaseDir() throws Exception {
        ManagedDeployableContainer.setupCleanServerDirectories(defaultServerBaseDir, jbossHome.getAbsolutePath(), userCleanServerBaseDir.getAbsolutePath());
        assertCleanServerBaseDir(userCleanServerBaseDir, 1);
    }

    private void assertCleanServerBaseDir(File cleanServerBaseDir, int id) {
        Assert.assertTrue(new File(cleanServerBaseDir, ManagedDeployableContainer.DATA_DIR).exists());
        Assert.assertTrue(new File(cleanServerBaseDir, ManagedDeployableContainer.CONFIG_DIR).exists());

        Assert.assertTrue(new File(cleanServerBaseDir, ManagedDeployableContainer.DATA_DIR + File.separatorChar + id).exists());
        Assert.assertTrue(new File(cleanServerBaseDir, ManagedDeployableContainer.CONFIG_DIR + File.separatorChar + id).exists());
    }

    private File createUserCleanServerBaseDir() {
        File root = new File("target/user-clean-server-base-dir");
        if (!root.exists()) {
            root.mkdirs();
        }
        return root.getAbsoluteFile();
    }

    private File createRootDir() {
        File root = new File("target/server-home");
        if (!root.exists()) {
            root.mkdirs();
        }
        return root.getAbsoluteFile();
    }

    private File createServer(File home, String serverName, int id) {
        File server = new File(home, serverName);
        if (!server.exists()) {
            server.mkdirs();
        }
        createDirectoryWithFile(server, ManagedDeployableContainer.DATA_DIR, id);
        createDirectoryWithFile(server, ManagedDeployableContainer.CONFIG_DIR, id);
        return server.getAbsoluteFile();
    }

    private File createDirectoryWithFile(File server, String name, int id) {
        File dir = new File(server, name);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, String.valueOf(id));
        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Assert.assertTrue(file.exists());

        return dir.getAbsoluteFile();
    }
}
