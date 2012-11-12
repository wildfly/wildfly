/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.embedded;

import org.jboss.as.server.ServerEnvironment;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class EmbeddedServerFactorySetupUnitTestCase {
    File standardJBossHome = createStandardAsHome();
    File alternativeServer = createServer(createRootDir(), "standalone2", 2);
    File alternativeDataDir = createDataOrConfigDir(createRootDir(), "otherData", 3);
    File alternativeConfigDir = createDataOrConfigDir(createRootDir(), "otherConfig", 4);

    File embeddedRoot;

    @Before
    public void createEmbeddedRoot() {
        embeddedRoot = new File("target/embedded-root").getAbsoluteFile();
        if (embeddedRoot.exists()) {
            deleteDirectory(embeddedRoot);
        }

        embeddedRoot.mkdirs();
        Assert.assertTrue(embeddedRoot.exists());
    }

    @Test
    public void testNoSpecialConfig() throws Exception {
        Properties props = new Properties();
        EmbeddedStandAloneServerFactory.setupCleanDirectories(standardJBossHome, props);
        Assert.assertEquals(0, props.size());
    }

    @Test
    public void testEmbeddedRootNoOverrides() throws Exception {
        Properties props = new Properties();
        props.setProperty(EmbeddedStandAloneServerFactory.JBOSS_EMBEDDED_ROOT, embeddedRoot.getAbsolutePath());
        EmbeddedStandAloneServerFactory.setupCleanDirectories(standardJBossHome, props);

        Assert.assertEquals(4, props.size());
        Assert.assertEquals(embeddedRoot.getAbsolutePath(), props.getProperty(EmbeddedStandAloneServerFactory.JBOSS_EMBEDDED_ROOT));
        assertPropertyAndEmbeddedRootFile(props, ServerEnvironment.SERVER_BASE_DIR, -1);
        assertPropertyAndEmbeddedRootFile(props, ServerEnvironment.SERVER_DATA_DIR, 1);
        assertPropertyAndEmbeddedRootFile(props, ServerEnvironment.SERVER_CONFIG_DIR, 1);
    }

    @Test
    public void testEmbeddedRootServerOverride() throws Exception {
        Properties props = new Properties();
        props.setProperty(EmbeddedStandAloneServerFactory.JBOSS_EMBEDDED_ROOT, embeddedRoot.getAbsolutePath());
        props.setProperty(ServerEnvironment.SERVER_BASE_DIR, alternativeServer.getAbsolutePath());
        EmbeddedStandAloneServerFactory.setupCleanDirectories(standardJBossHome, props);
        Assert.assertEquals(4, props.size());
        Assert.assertEquals(embeddedRoot.getAbsolutePath(), props.getProperty(EmbeddedStandAloneServerFactory.JBOSS_EMBEDDED_ROOT));
        assertPropertyAndEmbeddedRootFile(props, ServerEnvironment.SERVER_BASE_DIR, -1);
        assertPropertyAndEmbeddedRootFile(props, ServerEnvironment.SERVER_DATA_DIR, 2);
        assertPropertyAndEmbeddedRootFile(props, ServerEnvironment.SERVER_CONFIG_DIR, 2);
    }

    @Test
    public void testDataAndConfigOverride() throws Exception {
        Properties props = new Properties();
        props.setProperty(EmbeddedStandAloneServerFactory.JBOSS_EMBEDDED_ROOT, embeddedRoot.getAbsolutePath());
        props.setProperty(ServerEnvironment.SERVER_DATA_DIR, alternativeDataDir.getAbsolutePath());
        props.setProperty(ServerEnvironment.SERVER_CONFIG_DIR, alternativeConfigDir.getAbsolutePath());
        EmbeddedStandAloneServerFactory.setupCleanDirectories(standardJBossHome, props);
        Assert.assertEquals(4, props.size());
        Assert.assertEquals(embeddedRoot.getAbsolutePath(), props.getProperty(EmbeddedStandAloneServerFactory.JBOSS_EMBEDDED_ROOT));
        assertPropertyAndEmbeddedRootFile(props, ServerEnvironment.SERVER_BASE_DIR, -1);
        assertPropertyAndEmbeddedRootFile(props, ServerEnvironment.SERVER_DATA_DIR, 3);
        assertPropertyAndEmbeddedRootFile(props, ServerEnvironment.SERVER_CONFIG_DIR, 4);
    }

    @Test
    public void testServerOverrideAndDataAndConfigOverride() throws Exception {
        Properties props = new Properties();
        props.setProperty(EmbeddedStandAloneServerFactory.JBOSS_EMBEDDED_ROOT, embeddedRoot.getAbsolutePath());
        props.setProperty(ServerEnvironment.SERVER_BASE_DIR, alternativeServer.getAbsolutePath());
        props.setProperty(ServerEnvironment.SERVER_DATA_DIR, alternativeDataDir.getAbsolutePath());
        props.setProperty(ServerEnvironment.SERVER_CONFIG_DIR, alternativeConfigDir.getAbsolutePath());
        EmbeddedStandAloneServerFactory.setupCleanDirectories(standardJBossHome, props);
        Assert.assertEquals(4, props.size());
        Assert.assertEquals(embeddedRoot.getAbsolutePath(), props.getProperty(EmbeddedStandAloneServerFactory.JBOSS_EMBEDDED_ROOT));
        assertPropertyAndEmbeddedRootFile(props, ServerEnvironment.SERVER_BASE_DIR, -1);
        assertPropertyAndEmbeddedRootFile(props, ServerEnvironment.SERVER_DATA_DIR, 3);
        assertPropertyAndEmbeddedRootFile(props, ServerEnvironment.SERVER_CONFIG_DIR, 4);
    }

    @Test
    public void testServerOverrideAndConfigOverride() throws Exception {
        Properties props = new Properties();
        props.setProperty(EmbeddedStandAloneServerFactory.JBOSS_EMBEDDED_ROOT, embeddedRoot.getAbsolutePath());
        props.setProperty(ServerEnvironment.SERVER_BASE_DIR, alternativeServer.getAbsolutePath());
        props.setProperty(ServerEnvironment.SERVER_CONFIG_DIR, alternativeConfigDir.getAbsolutePath());
        EmbeddedStandAloneServerFactory.setupCleanDirectories(standardJBossHome, props);
        Assert.assertEquals(4, props.size());
        Assert.assertEquals(embeddedRoot.getAbsolutePath(), props.getProperty(EmbeddedStandAloneServerFactory.JBOSS_EMBEDDED_ROOT));
        assertPropertyAndEmbeddedRootFile(props, ServerEnvironment.SERVER_BASE_DIR, -1);
        assertPropertyAndEmbeddedRootFile(props, ServerEnvironment.SERVER_DATA_DIR, 2);
        assertPropertyAndEmbeddedRootFile(props, ServerEnvironment.SERVER_CONFIG_DIR, 4);
    }

    private void assertPropertyAndEmbeddedRootFile(Properties props, String property, int id) {
        String dirName = props.getProperty(property);
        Assert.assertNotNull(dirName);
        File dir = new File(dirName);
        Assert.assertTrue(dir.exists());
        Assert.assertTrue(dir.isDirectory());
        File expected = id >= 0 ? new File(dir, String.valueOf(id)) : dir;
        Assert.assertTrue(expected.exists());


        File parent = dir.getParentFile();
        while (parent != null) {
            if (parent.equals(embeddedRoot)) {
                return;
            }
            parent = parent.getParentFile();
        }
        Assert.fail(dir + " is not a child of " + embeddedRoot);
    }


    private File createRootDir() {
        File root = new File("target/server-home");
        if (!root.exists()) {
            root.mkdir();
        }
        return root.getAbsoluteFile();
    }

    private File createStandardAsHome() {
        File home = new File(createRootDir(), "jboss-home");
        if (!home.exists()) {
            home.mkdir();
        }

        createServer(home, "standalone", 1);

        return home.getAbsoluteFile();
    }

    private File createServer(File home, String serverName, int id) {
        File server = new File(home, serverName);
        server = new File(home, serverName);
        if (!server.exists()) {
            server.mkdir();
        }
        createDataOrConfigDir(server, "data", id);
        createDataOrConfigDir(server, "configuration", id);
        return server.getAbsoluteFile();
    }

    private File createDataOrConfigDir(File server, String name, int id) {
        File dir = new File(server, name);
        if (!dir.exists()) {
            dir.mkdir();
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

    private void deleteDirectory(File dir) {
        for (String name : dir.list()) {
            File current = new File(dir, name);
            if (current.isDirectory()) {
                deleteDirectory(current);
            } else {
                Assert.assertTrue(current.delete());
            }
        }
    }
}
