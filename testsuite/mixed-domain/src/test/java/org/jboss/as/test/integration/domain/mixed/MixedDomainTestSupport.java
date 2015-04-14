/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.domain.mixed;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.WildFlyManagedConfiguration;
import org.junit.Assert;


/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MixedDomainTestSupport extends DomainTestSupport {

    private final Version.AsVersion version;

    private MixedDomainTestSupport(Version.AsVersion version, String testClass, String domainConfig, String masterConfig, String slaveConfig,
                                   String jbossHome)
            throws Exception {
        super(testClass, domainConfig, masterConfig, slaveConfig,  new WildFlyManagedConfiguration(), new WildFlyManagedConfiguration(jbossHome));
        this.version = version;
    }

    public static MixedDomainTestSupport create(String testClass, Version.AsVersion version) throws Exception {
        final File dir = OldVersionCopier.expandOldVersion(version);
        final String copiedDomainXml = copyDomainFile();
        return new MixedDomainTestSupport(version, testClass, copiedDomainXml, "master-config/host.xml",
                "slave-config/host-slave.xml", dir.getAbsolutePath());
    }

    public void start() {
        try {
            //Start the master in admin only  and reconfigure the domain with what
            //we want to test in the mixed domain and have the DomainAdjuster
            //strip down the domain model to something more workable. The domain
            //adjusters will also make adjustments for the legacy version being
            //tested.
            DomainLifecycleUtil masterUtil = getDomainMasterLifecycleUtil();
            masterUtil.getConfiguration().setAdminOnly(true);
            masterUtil.start();
            DomainAdjuster.adjustForVersion(masterUtil.getDomainClient(), version);

            //Now reload the master in normal mode
            masterUtil.executeAwaitConnectionClosed(Util.createEmptyOperation("reload", PathAddress.pathAddress(HOST, "master")));
            masterUtil.connect();
            masterUtil.awaitHostController(System.currentTimeMillis());

            //Start the slaves
            DomainLifecycleUtil slaveUtil = getDomainSlaveLifecycleUtil();
            if (slaveUtil != null) {
                //slaveUtil.getConfiguration().addHostCommandLineProperty("-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=y");
                slaveUtil.start();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String copyDomainFile() throws Exception {

        final File originalDomainXml = loadFile("target", "jbossas", "domain", "configuration", "domain.xml");
        final File targetDirectory = createDirectory("target", "test-classes", "copied-master-config");
        final File copiedDomainXml = new File(targetDirectory, "domain.xml");
        if (copiedDomainXml.exists()) {
            Assert.assertTrue(copiedDomainXml.delete());
        }
        final InputStream in = new BufferedInputStream(new FileInputStream(originalDomainXml));
        try {
            final OutputStream out = new BufferedOutputStream(new FileOutputStream(copiedDomainXml));
            try {
                byte[] bytes = new byte[1024];
                int len = in.read(bytes);
                while (len != -1) {
                    out.write(bytes, 0, len);
                    len = in.read(bytes);
                }
            } finally {
                safeClose(out);
            }
        } finally {
            safeClose(in);
        }
        return "copied-master-config/domain.xml";
    }

    private static File loadFile(String first, String... parts) {
        final Path p = Paths.get(first, parts);
        final File file = p.toFile();
        Assert.assertTrue(file.getAbsolutePath() + " does not exist", file.exists());
        return file;
    }


    private static File createDirectory(String first, String... parts) throws IOException {
        Path p = Paths.get(first, parts);
        File dir = Files.createDirectories(p).toFile();
        Assert.assertTrue(dir.exists());
        return dir;
    }
}
