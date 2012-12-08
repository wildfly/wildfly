package org.jboss.as.arquillian.container.domain.managed;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author ggrossetie
 * @since 11/02/13
 */
public class ManagedDomainContainerSetupCleanServerUnitTestCase {

    private File jbossHome = new File(createRootDir(), "jboss-home");

    private String defaultServerBaseDir = createServer(jbossHome, "domain", 1).getAbsolutePath();
    private File alternativeServer = createServer(createRootDir(), "domain2", 2);

    private File tempDir = new File(System.getProperty("java.io.tmpdir"));
    private File cleanServerBaseDir = new File(tempDir, ManagedDomainDeployableContainer.TEMP_CONTAINER_DIRECTORY);

    private File userCleanServerBaseDir = createUserCleanServerBaseDir();

    @Test
    public void testDefaultServerBaseDir() throws Exception {
        ManagedDomainDeployableContainer.setupCleanServerDirectories(defaultServerBaseDir, jbossHome.getAbsolutePath(), null);
        assertCleanServerBaseDir(cleanServerBaseDir, 1);
    }

    @Test
    public void testAlternativeServerBaseDir() throws Exception {
        ManagedDomainDeployableContainer.setupCleanServerDirectories(".." + File.separatorChar + alternativeServer.getName(), jbossHome.getAbsolutePath(), null);
        assertCleanServerBaseDir(cleanServerBaseDir, 2);
    }

    @Test
    public void testUserCleanServerBaseDir() throws Exception {
        ManagedDomainDeployableContainer.setupCleanServerDirectories(defaultServerBaseDir, jbossHome.getAbsolutePath(), userCleanServerBaseDir.getAbsolutePath());
        assertCleanServerBaseDir(userCleanServerBaseDir, 1);
    }

    private void assertCleanServerBaseDir(File cleanServerBaseDir, int id) {
        Assert.assertTrue(new File(cleanServerBaseDir, ManagedDomainDeployableContainer.SERVERS_DIR).exists());
        Assert.assertTrue(new File(cleanServerBaseDir, ManagedDomainDeployableContainer.DATA_DIR).exists());
        Assert.assertTrue(new File(cleanServerBaseDir, ManagedDomainDeployableContainer.CONFIG_DIR).exists());

        Assert.assertTrue(new File(cleanServerBaseDir, ManagedDomainDeployableContainer.SERVERS_DIR + File.separatorChar + id).exists());
        Assert.assertTrue(new File(cleanServerBaseDir, ManagedDomainDeployableContainer.DATA_DIR + File.separatorChar + id).exists());
        Assert.assertTrue(new File(cleanServerBaseDir, ManagedDomainDeployableContainer.CONFIG_DIR + File.separatorChar + id).exists());
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
        createDirectoryWithFile(server, ManagedDomainDeployableContainer.DATA_DIR, id);
        createDirectoryWithFile(server, ManagedDomainDeployableContainer.CONFIG_DIR, id);
        createDirectoryWithFile(server, ManagedDomainDeployableContainer.SERVERS_DIR, id);
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
