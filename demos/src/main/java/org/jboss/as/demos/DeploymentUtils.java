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
package org.jboss.as.demos;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jboss.as.deployment.client.api.DuplicateDeploymentNameException;
import org.jboss.as.deployment.client.api.server.ServerDeploymentManager;
import org.jboss.as.deployment.client.api.server.ServerDeploymentPlanResult;
import org.jboss.as.standalone.client.api.StandaloneClient;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * Used to deploy/undeploy deployments to a running <b>standalone</b> application server
 *
 * TODO Use the real deployment API once that is complete
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class DeploymentUtils {

    private final List<Deployment> deployments = new ArrayList<Deployment>();
    private final StandaloneClient client;


    public DeploymentUtils() throws UnknownHostException {
        client = StandaloneClient.Factory.create(InetAddress.getByName("localhost"), 9999);
    }

    public DeploymentUtils(String archiveName, Package pkg) throws UnknownHostException {
        this();
        addDeployment(archiveName, pkg);
    }

    public DeploymentUtils(String archiveName, Package pkg, boolean show) throws UnknownHostException {
        this();
        addDeployment(archiveName, pkg, show);
    }

    public synchronized void addDeployment(String archiveName, Package pkg) {
        addDeployment(archiveName, pkg, false);
    }

    public synchronized void addDeployment(String archiveName, Package pkg, boolean show) {
        deployments.add(new Deployment(archiveName, pkg, show));
    }

    public synchronized void deploy()  throws DuplicateDeploymentNameException, IOException, ExecutionException, InterruptedException  {
        for (Deployment deployment : deployments) {
            deployment.deploy();
        }
    }

    public synchronized void undeploy() {
        for (int i = deployments.size() - 1 ; i >= 0 ; i--) {
            deployments.get(i).undeploy();
        }
    }

    /**
     * FIXME remove once we use the deployment API
     */
    public void waitForDeploymentHack(ObjectName objectName) throws Exception {
        MBeanServerConnection mbeanServer = getConnection();

        for (int i = 0 ; i < 10 ; i++) {
            try {
                System.out.println("Checking remote server for " + objectName + "...");
                mbeanServer.getMBeanInfo(objectName);
                System.out.println(objectName + " was found!");
                return;
            } catch (InstanceNotFoundException e) {
                Thread.sleep(1000);
            }
        }
    }

    public MBeanServerConnection getConnection() throws Exception {
        return JMXConnectorFactory.connect(new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:1090/jmxrmi"),
                new HashMap<String, Object>()).getMBeanServerConnection();
    }

    public String showJndi() throws Exception {
        return (String)getConnection().invoke(new ObjectName("jboss:type=JNDIView"), "list", new Object[] {true}, new String[] {"boolean"});
    }

    private class Deployment {
        final String archiveName;
        final Package pkg;
        final JavaArchive archive;
        final File realArchive;

        String deployment;

        public Deployment(String archiveName, Package pkg, boolean show) {
            this.archiveName = archiveName;
            this.pkg = pkg;

            ArchivePath metaInf = ArchivePaths.create("META-INF");

            archive = ShrinkWrap.create(JavaArchive.class, archiveName);
            archive.addPackage(pkg);

            File sourceMetaInf = getSourceMetaInfDir();
            addFiles(archive, sourceMetaInf, metaInf);

            System.out.println(archive.toString(show));

            realArchive = new File(getOutputDir(), archive.getName());
            archive.as(ZipExporter.class).exportZip(realArchive, true);
        }

        private void addFiles(JavaArchive archive, File dir, ArchivePath dest) {
            for (String name : dir.list()) {
                File file = new File(dir, name);
                if (file.isDirectory()) {
                    addFiles(archive, file, ArchivePaths.create(dest, name));
                } else {
                    archive.addResource(file, ArchivePaths.create(dest, name));
                }
            }
        }

        public synchronized void deploy() throws DuplicateDeploymentNameException, IOException, ExecutionException, InterruptedException {
            ServerDeploymentManager manager = client.getDeploymentManager();
            deployment = manager.addDeploymentContent(realArchive);
            ServerDeploymentPlanResult result = manager.execute(manager.newDeploymentPlan().add(deployment, realArchive).deploy(deployment).build()).get();
        }

        public synchronized void undeploy() {
            ServerDeploymentManager manager = client.getDeploymentManager();
            manager.execute(manager.newDeploymentPlan().undeploy(deployment).remove(deployment).build());
        }

        private File getSourceMetaInfDir() {
            String name = "archives/" + archiveName + "/META-INF/MANIFEST.MF";

            URL url = Thread.currentThread().getContextClassLoader().getResource(name);
            if (url == null) {
                throw new IllegalArgumentException("No resource called " + name);
            }
            try {
                File file = new File(url.toURI());
                return file.getParentFile();
            } catch (URISyntaxException e) {
                throw new RuntimeException("Could not get file for " + url);
            }
        }

        private File getOutputDir() {
            File file = new File("target");
            if (!file.exists()) {
                throw new IllegalStateException("target/ does not exist");
            }
            if (!file.isDirectory()) {
                throw new IllegalStateException("target/ is not a directory");
            }
            file = new File(file, "archives");
            if (file.exists()) {
                if (!file.isDirectory()) {
                    throw new IllegalStateException("target/archives/ already exists and is not a directory");
                }
            } else {
                file.mkdir();
            }
            return file.getAbsoluteFile();
        }

        private void close(Closeable c) {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (IOException ignore) {
            }
        }
    }
}
