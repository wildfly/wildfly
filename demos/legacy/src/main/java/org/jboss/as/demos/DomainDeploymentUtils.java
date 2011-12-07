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

import static org.jboss.as.protocol.StreamUtils.safeClose;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.domain.DuplicateDeploymentNameException;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * Used to deploy/undeploy deployments to a running <b>domain controller</b>
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Brian Stansberry
 * @version $Revision: 1.1 $
 */
public class DomainDeploymentUtils implements Closeable {

    private final List<Deployment> deployments = new ArrayList<Deployment>();
    private final ModelControllerClient client;
//    private final DomainDeploymentManager manager;
    private boolean injectedClient = true;

    public DomainDeploymentUtils() throws UnknownHostException {
        this(ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999, DemoAuthentication.getCallbackHandler()));
        this.injectedClient = false;
    }

    public DomainDeploymentUtils(ModelControllerClient client) throws UnknownHostException {
        this.client = client;
    }

    public DomainDeploymentUtils(String archiveName, Package pkg) throws UnknownHostException {
        this();
        addDeployment(archiveName, pkg);
    }

    public DomainDeploymentUtils(String archiveName, Package pkg, boolean show) throws UnknownHostException {
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
        ModelNode op = new ModelNode();
        OperationBuilder builder = new OperationBuilder(op, true);
        op.get(ClientConstants.OP).set("composite");
        op.get(ClientConstants.OP_ADDR).setEmptyList();
        ModelNode steps = op.get("steps");
        for (Deployment deployment : deployments) {
            steps.add(deployment.addDeployment(builder));
        }
        op.get(ClientConstants.OPERATION_HEADERS, ClientConstants.ROLLBACK_ON_RUNTIME_FAILURE).set(getRolloutPlan());
        execute(builder.build());
    }

    public synchronized void undeploy() throws IOException {
        ModelNode op = new ModelNode();
        op.get(ClientConstants.OP).set("composite");
        op.get(ClientConstants.OP_ADDR).setEmptyList();
        ModelNode steps = op.get("steps");
        boolean execute = false;
        Set<String> deployed = getDeploymentNames();
        for (Deployment deployment : deployments) {
            if (deployed.contains(deployment.archiveName)) {
                steps.add(deployment.removeDeployment());
                execute = true;
            }
        }
        if (execute) {
            op.get(ClientConstants.OPERATION_HEADERS, ClientConstants.ROLLBACK_ON_RUNTIME_FAILURE).set(getRolloutPlan());
            client.execute(op);
        }
    }

    public MBeanServerConnection getServerOneConnection() throws Exception {
        return JMXConnectorFactory.connect(new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:1090/jmxrmi"),
                new HashMap<String, Object>()).getMBeanServerConnection();
    }

    public String showServerOneJndi() throws Exception {
        return (String)getServerOneConnection().invoke(new ObjectName("jboss:type=JNDIView"), "list", new Object[] {true}, new String[] {"boolean"});
    }

    public MBeanServerConnection getServerTwoConnection() throws Exception {
        return JMXConnectorFactory.connect(new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:1240/jmxrmi"),
                new HashMap<String, Object>()).getMBeanServerConnection();
    }

    public String showServerTwoJndi() throws Exception {
        return (String)getServerTwoConnection().invoke(new ObjectName("jboss:type=JNDIView"), "list", new Object[] {true}, new String[] {"boolean"});
    }

    @Override
    public void close() throws IOException {
        if (!injectedClient) {
            safeClose(client);
        }
    }

    private Set<String> getDeploymentNames() throws IOException {
        ModelNode op = new ModelNode();
        op.get("operation").set("read-children-names");
        op.get("child-type").set("deployment");

        ModelNode result = execute(op);
        Set<String> names = new HashSet<String>();
        if(! result.isDefined()) {
            return Collections.emptySet();
        }
        for (ModelNode deployment : result.asList()) {
            names.add(deployment.asString());
        }
        return names;
    }

    private ModelNode execute(ModelNode op) throws IOException {
        return execute(new OperationBuilder(op).build());
    }

    private ModelNode execute(Operation op) throws IOException {
        ModelNode result = client.execute(op);
        if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
            return result.get("result");
        }
        else if (result.hasDefined("failure-description")) {
            System.out.println(op.getOperation());
            System.out.println(result);
            throw new RuntimeException(result.get("failure-description").toString());
        }
        else if (result.hasDefined("domain-failure-description")) {
            System.out.println(op);
            System.out.println(result);
            throw new RuntimeException(result.get("domain-failure-description").toString());
        }
        else if (result.hasDefined("host-failure-descriptions")) {
            System.out.println(result);
            throw new RuntimeException(result.get("host-failure-descriptions").toString());
        }
        else {
            System.out.println(result);
            throw new RuntimeException("Operation outcome is " + result.get("outcome").asString());
        }
    }

    private static ModelNode getRolloutPlan() {
        ModelNode result = new ModelNode();
        ModelNode series = result.get("in-series");
        series.add().get("server-group", "main-server-group");
        series.add().get("server-group", "other-server-group");
        result.get("rollback-across-groups").set(true);
        return result;
    }

    private class Deployment {
        final String archiveName;
        final JavaArchive archive;
        final File realArchive;

        public Deployment(String archiveName, Package pkg, boolean show) {
            this.archiveName = archiveName;

            ArchivePath metaInf = ArchivePaths.create("META-INF");

            archive = ShrinkWrap.create(JavaArchive.class, archiveName);
            archive.addPackage(pkg);

            File sourceMetaInf = getSourceMetaInfDir();
            addFiles(archive, sourceMetaInf, metaInf);

            System.out.println(archive.toString(show));

            realArchive = new File(getOutputDir(), archive.getName());
            archive.as(ZipExporter.class).exportTo(realArchive, true);
        }

        private void addFiles(JavaArchive archive, File dir, ArchivePath dest) {
            for (String name : dir.list()) {
                File file = new File(dir, name);
                if (file.isDirectory()) {
                    addFiles(archive, file, ArchivePaths.create(dest, name));
                } else {
                    archive.addAsResource(file, ArchivePaths.create(dest, name));
                }
            }
        }

        public synchronized ModelNode addDeployment(OperationBuilder context) throws IOException {


            System.out.println("Deploying " + realArchive.getName());
            Set<String> deployments = getDeploymentNames();
            int index = context.getInputStreamCount();
            context.addInputStream(new FileInputStream(realArchive));
            ModelNode result = new ModelNode();
            if (deployments.contains(archiveName)) {
                result.get("operation").set("full-replace-deployment");
                result.get("name").set(archiveName);
                result.get("content").get(0).get("input-stream-index").set(index);
            }
            else {
                result.get("operation").set("composite");
                ModelNode steps = result.get("steps");
                ModelNode add = steps.add();
                add.get("operation").set("add");
                add.get("address").add("deployment", archiveName);
                add.get("content").get(0).get("input-stream-index").set(index);
                ModelNode mainAdd = steps.add();
                mainAdd.get("operation").set("add");
                mainAdd.get("address").add("server-group", "main-server-group");
                mainAdd.get("address").add("deployment", archiveName);
                ModelNode mainDeploy = steps.add();
                mainDeploy.get("operation").set("deploy");
                mainDeploy.get("address").add("server-group", "main-server-group");
                mainDeploy.get("address").add("deployment", archiveName);
                ModelNode otherAdd = steps.add();
                otherAdd.get("operation").set("add");
                otherAdd.get("address").add("server-group", "other-server-group");
                otherAdd.get("address").add("deployment", archiveName);
                ModelNode otherDeploy = steps.add();
                otherDeploy.get("operation").set("deploy");
                otherDeploy.get("address").add("server-group", "other-server-group");
                otherDeploy.get("address").add("deployment", archiveName);
            }
            return result;
        }

        public synchronized ModelNode removeDeployment() {
            System.out.println("Undeploying " + realArchive.getName());
            ModelNode result = new ModelNode();
            result.get("operation").set("composite");
            ModelNode steps = result.get("steps");
            ModelNode mainUndeploy = steps.add();
            mainUndeploy.get("operation").set("undeploy");
            mainUndeploy.get("address").add("server-group", "main-server-group");
            mainUndeploy.get("address").add("deployment", archiveName);
            ModelNode mainRemove = steps.add();
            mainRemove.get("operation").set("remove");
            mainRemove.get("address").add("server-group", "main-server-group");
            mainRemove.get("address").add("deployment", archiveName);
            ModelNode otherUndeploy = steps.add();
            otherUndeploy.get("operation").set("undeploy");
            otherUndeploy.get("address").add("server-group", "other-server-group");
            otherUndeploy.get("address").add("deployment", archiveName);
            ModelNode otherRemove = steps.add();
            otherRemove.get("operation").set("remove");
            otherRemove.get("address").add("server-group", "other-server-group");
            otherRemove.get("address").add("deployment", archiveName);
            ModelNode domainRemove = steps.add();
            domainRemove.get("operation").set("remove");
            domainRemove.get("address").add("deployment", archiveName);
            return result;
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
    }
}
