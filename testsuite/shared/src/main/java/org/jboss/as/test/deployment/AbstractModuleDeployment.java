package org.jboss.as.test.deployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROCESS_STATE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.connector.subsystems.resourceadapters.Namespace;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdapterSubsystemParser;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.dmr.ModelNode;
import org.xnio.IoUtils;

/**
 * AS7-5768 -Support for RA module deployment
 *
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 * @author <a href="istudens@redhat.com">Ivo Studensky</a>
 * @author <a href="hsvabek@redhat.com">Hynek Svabek</a>
 */
public abstract class AbstractModuleDeployment extends AbstractMgmtServerSetupTask{
    private static final Pattern MODULE_SLOT_PATTERN = Pattern.compile("slot=\"main\"");

    protected File testModuleRoot;
    protected File slot;
    public static ModelNode address;
    protected final String defaultPath = "org/jboss/ironjacamar/ra16out";
    private boolean reloadRequired = false;
    private List<Path> toRemove = new LinkedList<>();

    public void addModule(final String moduleName) throws Exception {
        addModule(moduleName, "module.xml");
    }

    public void addModule(final String moduleName, String moduleXml) throws Exception {
        testModuleRoot = new File(getModulePath(), moduleName);
        removeModule(moduleName);
        createTestModule(moduleXml);
    }

    public void removeModule(final String moduleName) throws Exception {
        removeModule(moduleName, false);
    }

    public void removeModule(final String moduleName, boolean deleteParent) throws Exception {
        testModuleRoot = new File(getModulePath(), moduleName);
        File file = testModuleRoot;
        if (deleteParent) {
            while (!getModulePath().equals(file.getParentFile()))
                file = file.getParentFile();
        }
        toRemove.add(file.toPath());
    }

    private void deleteRecursively(Path file) throws IOException {
        if (Files.exists(file)) {
            Files.walkFileTree(file, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }

            });
        }
    }

    private void createTestModule(String moduleXml) throws IOException {
        slot = new File(testModuleRoot, getSlot());
        if (slot.exists()) {
            throw new IllegalArgumentException(slot + " already exists");
        }
        if (!slot.mkdirs()) {
            throw new IllegalArgumentException("Could not create " + slot);
        }
        URL url = this.getClass().getResource(moduleXml);
        if (url == null) {
            throw new IllegalStateException("Could not find " + moduleXml);
        }
        copyModuleXml(slot, url.openStream());
    }

    protected void copyFile(File target, InputStream src) throws IOException {
        final BufferedOutputStream out = new BufferedOutputStream(
                new FileOutputStream(target));
        try {
            int i = src.read();
            while (i != -1) {
                out.write(i);
                i = src.read();
            }
        } finally {
            IoUtils.safeClose(out);
        }
    }

    protected void copyModuleXml(File slot, InputStream src) throws IOException {
        try(BufferedReader in = new BufferedReader(new InputStreamReader(src));
                PrintWriter out = new PrintWriter(new File(slot, "module.xml"));) {
            String line;
            while ((line = in.readLine()) != null) {
                // replace slot name in the module xml file
                line = MODULE_SLOT_PATTERN.matcher(line).replaceAll("slot=\"" + getSlot() + "\"");
                out.println(line);
            }
        }
    }

    protected File getModulePath() {
        String modulePath = System.getProperty("module.path", null);
        if (modulePath == null) {
            String jbossHome = System.getProperty("jboss.home", null);
            if (jbossHome == null) {
                throw new IllegalStateException(
                        "Neither -Dmodule.path nor -Djboss.home were set");
            }
            modulePath = jbossHome + File.separatorChar + "modules";
        } else {
            modulePath = modulePath.split(File.pathSeparator)[0];
        }
        File moduleDir = new File(modulePath);
        if (!moduleDir.exists()) {
            throw new IllegalStateException(
                    "Determined module path does not exist");
        }
        if (!moduleDir.isDirectory()) {
            throw new IllegalStateException(
                    "Determined module path is not a dir");
        }
        return moduleDir;
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
        try {
            remove(address, managementClient);
        } finally {
            removeModule(defaultPath, true);
        }
        if (reloadRequired){
            executeReload();
        }
        for (Path p:toRemove) {
            deleteRecursively(p);
        }
        toRemove.clear();
    }

    protected void remove(final ModelNode address, final ManagementClient managementClient) throws IOException, MgmtOperationException {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("remove");
        operation.get(OP_ADDR).set(address);
        final ModelNode result = managementClient.getControllerClient().execute(operation);

        if (!SUCCESS.equals(result.get(OUTCOME).asString())) {
            throw new MgmtOperationException("Module removal failed: " + result.get(FAILURE_DESCRIPTION), operation, result);
        }
        final ModelNode responseHeaders = result.get(RESPONSE_HEADERS);
        if (responseHeaders.hasDefined(PROCESS_STATE) && ControlledProcessState.State.RELOAD_REQUIRED.toString().equals(responseHeaders.get(PROCESS_STATE).asString())) {
            this.reloadRequired = true;
        }
    }

    private void executeReload() throws Exception {
        final CommandContext ctx = CLITestUtil.getCommandContext();
        try {
            ctx.connectController();
            ctx.handle("reload");
        } finally {
            ctx.terminateSession();
        }
    }


    @Override
    protected void doSetup(ManagementClient managementClient) throws Exception {
        addModule(defaultPath);
    }

    protected void setConfiguration(String fileName) throws Exception {
        String xml = FileUtils.readFile(this.getClass(), fileName);
        // replace slot name in the configuration
        xml = MODULE_SLOT_PATTERN.matcher(xml).replaceAll("slot=\"" + getSlot() + "\"");
        List<ModelNode> operations = xmlToModelOperations(xml,
                Namespace.CURRENT.getUriString(),
                new ResourceAdapterSubsystemParser());
        address = operations.get(1).get("address");
        operations.remove(0);
        for (ModelNode op : operations) {
            executeOperation(op);
        }
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("activate");
        operation.get(OP_ADDR).set(address);
        executeOperation(operation);

        //executeOperation(operationListToCompositeOperation(operations));
    }

    /**
     * Creates module structure for uncompressed RA archive. RA classes are in
     * flat form too
     *
     * @throws Exception
     */
    protected abstract void fillModuleWithFlatClasses(String raFile) throws Exception;

    /**
     * Creates module structure for uncompressed RA archive.
     * RA classes are packed in .jar archive
     *
     * @throws Exception
     */
    protected abstract void fillModuleWithJar(String raFile) throws Exception;

    /**
     * Returns basic address
     *
     * @return address
     */
    public static ModelNode getAddress() {
        return address;
    }

    /**
     * This should be overridden to return a unique slot name for each test-case class / module.
     * We need this since custom modules are not supported to be removing at runtime, see WFLY-1560.
     * @return a name of the slot of the test module
     */
    protected abstract String getSlot();
}
