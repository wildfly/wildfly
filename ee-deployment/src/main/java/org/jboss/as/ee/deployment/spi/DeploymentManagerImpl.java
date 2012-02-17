/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ee.deployment.spi;

import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.jboss.as.ee.deployment.spi.configurations.WarConfiguration;
import org.jboss.as.ee.deployment.spi.status.DeploymentStatusImpl;
import org.jboss.as.ee.deployment.spi.status.ProgressObjectImpl;
import org.jboss.util.xml.JBossEntityResolver;

import javax.enterprise.deploy.model.DeployableObject;
import javax.enterprise.deploy.shared.ActionType;
import javax.enterprise.deploy.shared.CommandType;
import javax.enterprise.deploy.shared.DConfigBeanVersionType;
import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.shared.StateType;
import javax.enterprise.deploy.spi.DeploymentConfiguration;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.DConfigBeanVersionUnsupportedException;
import javax.enterprise.deploy.spi.exceptions.InvalidModuleException;
import javax.enterprise.deploy.spi.exceptions.TargetException;
import javax.enterprise.deploy.spi.status.DeploymentStatus;
import javax.enterprise.deploy.spi.status.ProgressObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.jboss.as.ee.deployment.spi.DeploymentLogger.ROOT_LOGGER;
import static org.jboss.as.ee.deployment.spi.DeploymentMessages.MESSAGES;

/**
 * The DeploymentManager object provides the core set of functions a J2EE platform must provide for J2EE application deployment.
 * It provides server related information, such as, a list of deployment targets, and vendor unique runtime configuration
 * information.
 *
 * @author Thomas.Diesler@jboss.com
 */
public class DeploymentManagerImpl implements DeploymentManager {

    /**
     * The URI deployment factory recognizes: http://org.jboss.as.ee.deployment/jsr88
     */
    public static final String DEPLOYER_URI = "http://org.jboss.as.ee.deployment/jsr88";

    // available deployment targets
    private Target[] targets;

    // maps the DD to the archives
    private Map<String, File> mapDeploymentPlan;
    private DeploymentMetaData metaData;

    private final List<File> tmpFiles = new ArrayList<File>();
    private final URI deployURI;
    private boolean isConnected;
    private final String username;
    private final String password;

    /**
     * Create a deployment manager for the given URL and connected mode.
     *
     * @param deployURI
     * @param isConnected
     */
    public DeploymentManagerImpl(URI deployURI, boolean isConnected) {
        this(deployURI, isConnected, null, null);
    }

    /**
     * Create a deployment manager for the given URL and connected mode, username and password.
     *
     * @param deployURI
     * @param isConnected
     * @param username
     * @param password
     */
    public DeploymentManagerImpl(URI deployURI, boolean isConnected, String username, String password) {
        this.deployURI = deployURI;
        this.isConnected = isConnected;
        this.targets = null;
        this.username = username;
        this.password = password;
    }

    /**
     * Get the available targets. This is determined by parsing the deployURI and looking at the targetType query parameter if
     * the URI is not opaque. Supported targetTypes are:
     * <p/>
     * as7 = AS7 Server Management Target
     *
     * @return the available targets
     * @throws IllegalStateException when the manager is disconnected
     */
    public Target[] getTargets() {
        if (isConnected == false)
            throw new IllegalStateException(MESSAGES.deploymentManagerNotConnected());

        if (targets == null) {
            if (deployURI.isOpaque()) {
                throw new UnsupportedOperationException(MESSAGES.opaqueDeploymentUriNotImplemented());
                // log.debug("Opaque URI seen, defaulting to LocalhostTarget");
                // targets = new Target[] { new LocalhostTarget() };
            } else {
                ROOT_LOGGER.debugf("Non-Opaque URI seen, checking query for targetType");

                URIParser parser = new URIParser(deployURI);
                String targetType = parser.getParameter("targetType");
                if ("as7".equals(targetType)) {
                    targets = new Target[]{new DeploymentManagerTarget(deployURI, username, password)};
                } else {
                    throw new IllegalArgumentException(MESSAGES.invalidTargetType(deployURI));
                }
            }
        }
        return targets;
    }

    /**
     * Get the running modules
     *
     * @param moduleType the module type
     * @param targets    the targets
     * @return the target modules
     * @throws javax.enterprise.deploy.spi.exceptions.TargetException
     *                               an invalid target
     * @throws IllegalStateException when the manager is disconnected
     */
    public TargetModuleID[] getRunningModules(ModuleType moduleType, Target[] targets) throws TargetException {
        if (isConnected == false)
            throw new IllegalStateException(MESSAGES.deploymentManagerNotConnected());

        ROOT_LOGGER.debugf("getRunningModules [type=%s,targets=%s]", moduleType, Arrays.asList(targets));

        // get running modules
        Set<JBossTargetModuleID> moduleSet = new HashSet<JBossTargetModuleID>();
        TargetModuleID[] availableModules = getAvailableModules(moduleType, targets);
        if (availableModules == null) {
            ROOT_LOGGER.debugf("No modules available");
            return null;
        }

        for (int i = 0; i < availableModules.length; i++) {
            JBossTargetModuleID moduleID = (JBossTargetModuleID) availableModules[i];
            if (moduleID.isRunning()) {
                moduleSet.add(moduleID);
            }
        }
        ROOT_LOGGER.debugf("Found [%d] running modules", moduleSet.size());

        // convert set to array
        TargetModuleID[] idarr = new TargetModuleID[moduleSet.size()];
        moduleSet.toArray(idarr);
        return idarr;
    }

    /**
     * Get the non running modules
     *
     * @param moduleType the module type
     * @param targets    the targets
     * @return the target modules
     * @throws javax.enterprise.deploy.spi.exceptions.TargetException
     *                               an invalid target
     * @throws IllegalStateException when the manager is disconnected
     */
    public TargetModuleID[] getNonRunningModules(ModuleType moduleType, Target[] targets) throws TargetException {
        if (isConnected == false)
            throw new IllegalStateException(MESSAGES.deploymentManagerNotConnected());

        ROOT_LOGGER.debugf("getNonRunningModules [type=%s,targets=%s]", moduleType, Arrays.asList(targets));

        // get non running modules
        Set<JBossTargetModuleID> moduleSet = new HashSet<JBossTargetModuleID>();
        TargetModuleID[] availableModules = getAvailableModules(moduleType, targets);
        if (availableModules == null) {
            ROOT_LOGGER.debugf("No modules available");
            return null;
        }

        for (int i = 0; i < availableModules.length; i++) {
            JBossTargetModuleID moduleID = (JBossTargetModuleID) availableModules[i];
            if (moduleID.isRunning() == false) {
                moduleSet.add(moduleID);
            }
        }
        ROOT_LOGGER.debugf("Found [%d] non running modules", moduleSet.size());

        // convert set to array
        TargetModuleID[] idarr = new TargetModuleID[moduleSet.size()];
        moduleSet.toArray(idarr);
        return idarr;
    }

    /**
     * Retrieve the list of all J2EE application modules running or not running on the identified targets.
     *
     * @param moduleType the module type
     * @param targets    the targets
     * @return the target modules
     * @throws javax.enterprise.deploy.spi.exceptions.TargetException
     *                               an invalid target
     * @throws IllegalStateException when the manager is disconnected
     */
    public TargetModuleID[] getAvailableModules(ModuleType moduleType, Target[] targets) throws TargetException {
        if (isConnected == false)
            throw new IllegalStateException(MESSAGES.deploymentManagerNotConnected());

        ROOT_LOGGER.debugf("getAvailableModules [type=%s,targets=%s]", moduleType, Arrays.asList(targets));

        // get non running modules
        List<TargetModuleID> targetModules = new ArrayList<TargetModuleID>();
        for (int i = 0; i < targets.length; i++) {
            JBossTarget target = (JBossTarget) targets[i];
            TargetModuleID[] tmids = target.getAvailableModules(moduleType);
            targetModules.addAll(Arrays.asList(tmids));
        }
        ROOT_LOGGER.debugf("Found [%d] available modules", targetModules.size());

        // convert set to array
        if (targetModules.size() > 0) {
            TargetModuleID[] idarr = new TargetModuleID[targetModules.size()];
            targetModules.toArray(idarr);
            return idarr;
        }

        // according to the spec, we have to return null
        return null;
    }

    /**
     * Retrieve server specific configuration for a component
     *
     * @param obj the deployable component
     * @return the configuration
     * @throws javax.enterprise.deploy.spi.exceptions.InvalidModuleException
     *          when the module does not exist or is not supported
     */
    public DeploymentConfiguration createConfiguration(DeployableObject obj) throws InvalidModuleException {
        if (obj == null)
            throw new IllegalArgumentException(MESSAGES.nullArgument("deployableObject"));

        // do some stuff to figure out what kind of config to return.
        if (obj.getType().equals(ModuleType.WAR))
            return new WarConfiguration(obj);

        throw new InvalidModuleException(MESSAGES.moduleTypeNotSupported(obj.getType()));
    }

    /**
     * Validates the configuration, generates all container specific classes and moves the archive to the targets
     *
     * @param targets        the targets
     * @param moduleArchive  the module archive
     * @param deploymentPlan the runtime configuration
     * @return the progress object
     * @throws IllegalStateException when the manager is disconnected
     */
    public ProgressObject distribute(Target[] targets, File moduleArchive, File deploymentPlan) {
        if (isConnected == false)
            throw new IllegalStateException(MESSAGES.deploymentManagerNotConnected());

        InputStream isModuleArchive = null;
        InputStream isDeploymentPlan = null;
        try {
            isModuleArchive = new FileInputStream(moduleArchive);
            isDeploymentPlan = new FileInputStream(deploymentPlan);
            return distribute(targets, isModuleArchive, isDeploymentPlan);
        } catch (FileNotFoundException e) {
            String message = MESSAGES.cannotFindDeploymentFile(e.getMessage());
            DeploymentStatus status = new DeploymentStatusImpl(StateType.FAILED, CommandType.DISTRIBUTE, ActionType.EXECUTE, message);
            ROOT_LOGGER.errorf(message);
            return new ProgressObjectImpl(status, null);
        }
    }

    /**
     * Validates the configuration, generates all container specific classes and moves the archive to the targets
     *
     * @param targets        the targets
     * @param moduleArchive  the module archive
     * @param deploymentPlan the runtime configuration
     * @return the progress object
     * @throws IllegalStateException when the manager is disconnected
     */
    public ProgressObject distribute(Target[] targets, InputStream moduleArchive, InputStream deploymentPlan) {
        return doDistribute(targets, null, moduleArchive, deploymentPlan);
    }

    /**
     * The distribute method performs three tasks; it validates the deployment configuration data, generates all container
     * specific classes and interfaces, and moves the fully baked archive to the designated deployment targets.
     *
     * @param targets        the targets
     * @param moduleArchive  the module archive
     * @param deploymentPlan the runtime configuration
     * @return the progress object
     * @throws IllegalStateException when the manager is disconnected
     */
    public ProgressObject distribute(Target[] targets, ModuleType type, InputStream moduleArchive, InputStream deploymentPlan) throws IllegalStateException {
        return doDistribute(targets, type, moduleArchive, deploymentPlan);
    }

    private ProgressObject doDistribute(Target[] targets, ModuleType type, InputStream moduleArchive, InputStream deploymentPlan) throws IllegalStateException {
        if (isConnected == false)
            throw new IllegalStateException(MESSAGES.deploymentManagerNotConnected());

        TargetModuleID[] targetModuleIDs = new TargetModuleID[targets.length];
        try {
            // create for each entry in the deploymentPlan a temp file
            mapDeploymentPlan = unpackDeploymentPlan(deploymentPlan);
            initDeploymentMetaData();

            // create the temp file that contains the deployment
            TargetModuleInfo moduleInfo = createDeployment(moduleArchive, metaData.getDeploymentName());
            URL deployment = moduleInfo.getModuleID();
            if (type == null)
                type = moduleInfo.getModuleType();

            // create the target module ids
            for (int i = 0; i < targets.length; i++) {
                JBossTarget target = (JBossTarget) targets[i];
                String moduleID = deployment.toExternalForm();
                targetModuleIDs[i] = new TargetModuleIDImpl(target, moduleID, null, type);
            }

            // delete all temp files, except the deployment
            for (int i = 0; i < tmpFiles.size(); i++) {
                File file = tmpFiles.get(i);
                if (file.equals(deployment) == false)
                    file.delete();
            }
        } catch (IOException e) {
            String message = MESSAGES.deploymentValidationFailed();
            DeploymentStatus status = new DeploymentStatusImpl(StateType.FAILED, CommandType.DISTRIBUTE, ActionType.EXECUTE, message);
            ROOT_LOGGER.errorf(e, message);
            return new ProgressObjectImpl(status, targetModuleIDs);
        }

        // start the deployment process
        DeploymentStatus status = new DeploymentStatusImpl(StateType.RUNNING, CommandType.DISTRIBUTE, ActionType.EXECUTE, null);
        ProgressObject progress = new ProgressObjectImpl(status, targetModuleIDs);

        DeploymentWorker worker = new DeploymentWorker(progress);
        worker.start();

        return progress;
    }

    /**
     * Initialize the deployment meta data
     */
    private void initDeploymentMetaData() throws IOException {
        File metaTmpFile = mapDeploymentPlan.get(DeploymentMetaData.ENTRY_NAME);
        if (metaTmpFile == null)
            throw new IOException(MESSAGES.deploymentPlanDoesNotContainEntry(DeploymentMetaData.ENTRY_NAME));

        try {
            SAXReader reader = new SAXReader();
            reader.setEntityResolver(new JBossEntityResolver());

            Document metaDoc = reader.read(metaTmpFile);
            metaData = new DeploymentMetaData(metaDoc);
            ROOT_LOGGER.debugf(DeploymentMetaData.ENTRY_NAME + "\n" + metaData.toXMLString());
        } catch (Exception e) {
            ROOT_LOGGER.errorf(e, MESSAGES.cannotObtainMetaData());
        }
    }

    /**
     * Create the JBoss deployment, by enhancing the content of the module archive with the entries in the deployment plan.
     */
    private TargetModuleInfo createDeployment(InputStream moduleArchive, String moduleName) throws IOException {
        File tmpFile = File.createTempFile("jboss_deployment_", ".zip");
        ROOT_LOGGER.debugf("temporary deployment file: %s", tmpFile);

        JarInputStream jis = new JarInputStream(moduleArchive);

        // make sure we don't loose the manifest when creating a new JarOutputStream
        JarOutputStream jos;
        FileOutputStream fos = new FileOutputStream(tmpFile);
        Manifest manifest = jis.getManifest();
        if (manifest != null)
            jos = new JarOutputStream(fos, manifest);
        else
            jos = new JarOutputStream(fos);

        // process all modules
        TargetModuleInfo moduleInfo = new TargetModuleInfo();
        ModuleType moduleType = null;
        JarEntry entry = jis.getNextJarEntry();
        while (entry != null) {
            String entryName = entry.getName();

            // only process file entries
            if (entryName.endsWith("/") == false) {
                moduleType = ifNotNull(determineModuleType(entryName), moduleType);

                // process a sub module
                if (entryName.endsWith(".jar") || entryName.endsWith(".war")) {
                    File tmpSubModule = processSubModule(entryName, jis);
                    FileInputStream fis = new FileInputStream(tmpSubModule);
                    JarUtils.addJarEntry(jos, entryName, fis);
                    fis.close();
                } else {
                    if (mapDeploymentPlan.get("!/" + entryName) == null)
                        JarUtils.addJarEntry(jos, entryName, jis);
                    else
                        ROOT_LOGGER.debugf("Skip entry found in deployment plan: %s", entryName);
                }
            }
            entry = jis.getNextJarEntry();
        }

        if (moduleType == null) {
            if (moduleName.endsWith(ModuleType.EAR.getModuleExtension()))
                moduleType = ModuleType.EAR;
            else if (moduleName.endsWith(ModuleType.WAR.getModuleExtension()))
                moduleType = ModuleType.WAR;
            else if (moduleName.endsWith(ModuleType.RAR.getModuleExtension()))
                moduleType = ModuleType.RAR;
            else
                throw new RuntimeException(MESSAGES.cannotObtainModuleType(moduleName));
        }

        moduleInfo.setModuleType(moduleType);
        // there may be top level deployment plan entries, add them
        addDeploymentPlanEntry(jos, null);
        jos.close();

        // rename the deployment
        String deploymentName = tmpFile.getParent() + File.separator + metaData.getDeploymentName();
        File deployment = new File(deploymentName);
        if (deployment.exists() && deployment.delete() == false)
            throw new IOException(MESSAGES.cannotDeleteExistingDeployment(deployment));

        tmpFile.renameTo(deployment);
        moduleInfo.setModuleID(deployment.toURI().toURL());
        return moduleInfo;
    }

    /**
     * A simple module type determination taking only the descriptor name into account.
     */
    private static ModuleType determineModuleType(String entryName) {
        ModuleType moduleType = null;
        if (entryName.endsWith("/application.xml")) {
            moduleType = ModuleType.EAR;
        } else if (entryName.endsWith("/application-client.xml")) {
            moduleType = ModuleType.CAR;
        } else if (entryName.endsWith("/ra.xml")) {
            moduleType = ModuleType.RAR;
        } else if (entryName.endsWith("/web.xml")) {
            moduleType = ModuleType.WAR;
        } else if (entryName.endsWith("/ejb-jar.xml") || entryName.endsWith("/jboss.xml")) {
            moduleType = ModuleType.EJB;
        }
        return moduleType;
    }

    private static <T> T ifNotNull(T val, T def) {
        return val != null ? val : def;
    }

    public ProgressObject redeploy(TargetModuleID[] targetModuleIDs, File file, File file1) throws UnsupportedOperationException, IllegalStateException {
        if (isConnected == false)
            throw new IllegalStateException(MESSAGES.deploymentManagerNotConnected());

        throw new UnsupportedOperationException();
    }

    public ProgressObject redeploy(TargetModuleID[] targetModuleIDs, InputStream inputStream, InputStream inputStream1) throws UnsupportedOperationException, IllegalStateException {
        if (isConnected == false)
            throw new IllegalStateException(MESSAGES.deploymentManagerNotConnected());

        throw new UnsupportedOperationException();
    }

    /**
     * Start the modules
     *
     * @param targetModuleIDs the list of modules
     * @return the progress object
     * @throws IllegalStateException when the manager is disconnected
     */
    public ProgressObject start(TargetModuleID[] targetModuleIDs) {
        if (isConnected == false)
            throw new IllegalStateException(MESSAGES.deploymentManagerNotConnected());

        ROOT_LOGGER.debugf("start %s", Arrays.asList(targetModuleIDs));

        // start the deployment process
        DeploymentStatus status = new DeploymentStatusImpl(StateType.RUNNING, CommandType.START, ActionType.EXECUTE, null);
        ProgressObject progress = new ProgressObjectImpl(status, targetModuleIDs);

        DeploymentWorker worker = new DeploymentWorker(progress);
        worker.start();

        return progress;
    }

    /**
     * Stop the modules
     *
     * @param targetModuleIDs the list of modules
     * @return the progress object
     * @throws IllegalStateException when the manager is disconnected
     */
    public ProgressObject stop(TargetModuleID[] targetModuleIDs) {
        if (isConnected == false)
            throw new IllegalStateException(MESSAGES.deploymentManagerNotConnected());

        ROOT_LOGGER.debugf("stop %s", Arrays.asList(targetModuleIDs));

        DeploymentStatus status = new DeploymentStatusImpl(StateType.RUNNING, CommandType.STOP, ActionType.EXECUTE, null);
        ProgressObject progress = new ProgressObjectImpl(status, targetModuleIDs);

        DeploymentWorker worker = new DeploymentWorker(progress);
        worker.start();

        return progress;
    }

    /**
     * Removes the modules
     *
     * @param targetModuleIDs the list of modules
     * @return the progress object
     * @throws IllegalStateException when the manager is disconnected
     */
    public ProgressObject undeploy(TargetModuleID[] targetModuleIDs) {
        if (isConnected == false)
            throw new IllegalStateException(MESSAGES.deploymentManagerNotConnected());

        ROOT_LOGGER.debugf("undeploy %s", Arrays.asList(targetModuleIDs));

        // start the deployment process
        DeploymentStatus status = new DeploymentStatusImpl(StateType.RUNNING, CommandType.UNDEPLOY, ActionType.EXECUTE, null);
        ProgressObject progress = new ProgressObjectImpl(status, targetModuleIDs);

        DeploymentWorker worker = new DeploymentWorker(progress);
        worker.start();

        return progress;
    }

    /**
     * Is redeploy supported
     *
     * @return true when redeploy is supported, false otherwise
     */
    public boolean isRedeploySupported() {
        return false;
    }

    /**
     * Redeploys the modules
     *
     * @param moduleIDList the list of modules
     * @return the progress object
     * @throws IllegalStateException         when the manager is disconnected
     * @throws UnsupportedOperationException when redeploy is not supported
     */
    public ProgressObject redeploy(TargetModuleID[] moduleIDList) {
        if (isConnected == false)
            throw new IllegalStateException(MESSAGES.deploymentManagerNotConnected());

        throw new UnsupportedOperationException();
    }

    /**
     * The release method is the mechanism by which the tool signals to the DeploymentManager that the tool does not need it to
     * continue running connected to the platform. The tool may be signaling it wants to run in a disconnected mode or it is
     * planning to shutdown. When release is called the DeploymentManager may close any J2EE resource connections it had for
     * deployment configuration and perform other related resource cleanup. It should not accept any new operation requests
     * (i.e., distribute, start stop, undeploy, redeploy. It should finish any operations that are currently in process. Each
     * ProgressObject associated with a running operation should be marked as released (see the ProgressObject).
     */
    public void release() {
        isConnected = false;
    }

    /**
     * Get the default locale
     *
     * @return the default locale
     */
    public Locale getDefaultLocale() {
        return Locale.getDefault();
    }

    /**
     * Currently we only support the default locale
     */
    public Locale getCurrentLocale() {
        return Locale.getDefault();
    }

    /**
     * Currently we only support the default locale
     */
    public void setLocale(Locale locale) {
        throw new UnsupportedOperationException();
    }

    /**
     * Currently we only support the default locale
     */
    public boolean isLocaleSupported(Locale locale) {
        return Locale.getDefault().equals(locale);
    }

    /**
     * Currently we only support the default locale
     *
     * @return the supported locales
     */
    public Locale[] getSupportedLocales() {
        return new Locale[]{Locale.getDefault()};
    }

    /**
     * Get the J2EE platform version
     *
     * @return the version
     */
    public DConfigBeanVersionType getDConfigBeanVersion() {
        return DConfigBeanVersionType.V1_4;
    }

    public void setDConfigBeanVersion(DConfigBeanVersionType dConfigBeanVersionType) throws DConfigBeanVersionUnsupportedException {
        throw new UnsupportedOperationException();
    }

    public boolean isDConfigBeanVersionSupported(DConfigBeanVersionType dConfigBeanVersionType) {
        return dConfigBeanVersionType == DConfigBeanVersionType.V1_4;
    }

    /**
     * Test whether the version is supported
     *
     * @param version the version
     * @return true when supported, false otherwise
     */
    public boolean isDConfigBeanVersionTypeSupported(DConfigBeanVersionType version) {
        return version == DConfigBeanVersionType.V1_4;
    }

    /**
     * Set the J2EE version
     *
     * @param version the version
     * @throws UnsupportedOperationException when the version is not supported
     */
    public void setDConfigBeanVersionType(DConfigBeanVersionType version) {
        throw new UnsupportedOperationException();
    }

    /**
     * Process a potential sub module, adding descriptors from the deployment plan
     */
    private File processSubModule(String entryName, JarInputStream jis) throws IOException {
        // first copy te entry as is to a temp file
        File tmpModule = getTempFile(entryName);
        tmpFiles.add(tmpModule);
        FileOutputStream fos = new FileOutputStream(tmpModule);
        JarUtils.copyStream(fos, jis);
        fos.close();

        // now open the copy we just made and copy again entry by entry
        JarInputStream jisModule = new JarInputStream(new FileInputStream(tmpModule));
        File tmpJBossModule = getTempFile("jboss_" + entryName);
        tmpFiles.add(tmpJBossModule);
        JarOutputStream jos = null;
        fos = new FileOutputStream(tmpJBossModule);
        Manifest manifest = jisModule.getManifest();
        if (manifest != null)
            jos = new JarOutputStream(fos, manifest);
        else
            jos = new JarOutputStream(fos);

        // now copy entry by entry
        JarEntry entry = jisModule.getNextJarEntry();
        while (entry != null) {
            String subEntryName = entry.getName();
            if (mapDeploymentPlan.get(entryName + "!/" + subEntryName) == null)
                JarUtils.addJarEntry(jos, subEntryName, jisModule);
            else
                ROOT_LOGGER.debugf("Skip entry found in deployment plan: %s", subEntryName);

            entry = jisModule.getNextJarEntry();
        }
        jisModule.close();

        addDeploymentPlanEntry(jos, entryName);

        jos.close();

        return tmpJBossModule;
    }

    /**
     * Add an entry from the deployment plan, if found
     *
     * @param moduleName entries will be added for that module, null indicates top level
     */
    private void addDeploymentPlanEntry(JarOutputStream jos, String moduleName) throws IOException {
        if (moduleName == null)
            moduleName = "";

        // ignore deployment plan entries that do not start like this
        String moduleKey = moduleName + "!/";

        Iterator<String> it = mapDeploymentPlan.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            if (key.startsWith(moduleKey)) {
                String dpName = key.substring(moduleKey.length());
                ROOT_LOGGER.debugf("found deployment plan entry: %s", dpName);

                File dpFile = mapDeploymentPlan.get(key);
                FileInputStream dpin = new FileInputStream(dpFile);
                JarUtils.addJarEntry(jos, dpName, dpin);
                dpin.close();
            }
        }
    }

    /**
     * Create a temp file for every entry in the deployment plan
     */
    private Map<String, File> unpackDeploymentPlan(InputStream deploymentPlan) throws IOException {
        Map<String, File> dpMap = new HashMap<String, File>();

        if (deploymentPlan == null)
            return dpMap;

        // process the deployment plan
        try {
            JarInputStream jarDeploymentPlan = new JarInputStream(deploymentPlan);
            JarEntry entry = jarDeploymentPlan.getNextJarEntry();
            while (entry != null) {
                String entryName = entry.getName();
                ROOT_LOGGER.debugf("unpack deployment plan entry: %s", entryName);

                File tempFile = getTempFile(entryName);
                dpMap.put(entryName, tempFile);

                FileOutputStream out = new FileOutputStream(tempFile);
                JarUtils.copyStream(out, jarDeploymentPlan);
                out.close();

                entry = jarDeploymentPlan.getNextJarEntry();
            }
        } finally {
            deploymentPlan.close();
        }

        return dpMap;
    }

    /**
     * Get a temp file for an jar entry name
     */
    private File getTempFile(String entryName) throws IOException {
        entryName = entryName.replace('/', '_');
        int index = entryName.lastIndexOf(".");
        String prefix = entryName.substring(0, index);
        String suffix = entryName.substring(index);

        File tempFile = File.createTempFile(prefix, suffix);
        tmpFiles.add(tempFile);
        return tempFile;
    }

    private static class TargetModuleInfo {
        URL moduleID = null;
        ModuleType moduleType = null;

        public URL getModuleID() {
            return moduleID;
        }

        public void setModuleID(URL url) {
            moduleID = url;
        }

        public ModuleType getModuleType() {
            return moduleType;
        }

        public void setModuleType(ModuleType type) {
            moduleType = type;
        }
    }
}
