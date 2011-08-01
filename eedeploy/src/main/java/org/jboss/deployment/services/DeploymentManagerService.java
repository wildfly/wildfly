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
package org.jboss.deployment.services;

import org.jboss.logging.Logger;

/**
 * A service that supports the JSR-88 DeploymentManager operations.
 *
 * [TODO remove]
 *
 * @author Scott.Stark@jboss.org
 * @author adrian@jboss.org
 */
public class DeploymentManagerService implements DeploymentManagerServiceMBean {
    private static Logger log = Logger.getLogger(DeploymentManagerService.class);

    /*
     * VFSDeploymentFactory deploymentFactory = VFSDeploymentFactory.getInstance();
     *
     * private Map<String, SerializableTargetModuleID> moduleMap = Collections.synchronizedMap(new HashMap<String,
     * SerializableTargetModuleID>());
     *
     * private Class carDeployerType; private Class earDeployerType; private Class ejbDeployerType; private Class
     * ejb3DeployerType; private Class rarDeployerType; private Class warDeployerType; private MainDeployerImpl mainDeployer;
     * private Controller controller;
     *
     * private File uploadDir; private boolean failOnCollision; private boolean deleteOnUndeploy;
     *
     * public static String getDeploymentName(VirtualFile file) { if (file == null) throw new
     * IllegalArgumentException("Null file"); try { URI uri = file.asFileURI(); // gets the file path without trailing slash
     * return uri.toString(); } catch (Exception e) { throw new IllegalArgumentException("File does not have a valid uri: " +
     * file, e); } }
     *
     * public MainDeployerImpl getMainDeployer() { return mainDeployer; } public void setMainDeployer(MainDeployerImpl
     * mainDeployer) { this.mainDeployer = mainDeployer; }
     *
     * public Controller getController() { return controller; }
     *
     * public void setController(Controller controller) { this.controller = controller; }
     *
     * public Class getCarDeployerType() { return carDeployerType; } public void setCarDeployerType(Class carDeployerType) {
     * this.carDeployerType = carDeployerType; } public Class getEarDeployerType() { return earDeployerType; } public void
     * setEarDeployerType(Class earDeployerType) { this.earDeployerType = earDeployerType; } public Class getEjbDeployerType() {
     * return ejbDeployerType; } public void setEjbDeployerType(Class ejbDeployerType) { this.ejbDeployerType = ejbDeployerType;
     * }
     *
     * public Class getEjb3DeployerType() { return ejb3DeployerType; } public void setEjb3DeployerType(Class ejb3DeployerType) {
     * this.ejb3DeployerType = ejb3DeployerType; }
     *
     * public Class getRarDeployerType() { return rarDeployerType; } public void setRarDeployerType(Class rarDeployerType) {
     * this.rarDeployerType = rarDeployerType; } public Class getWarDeployerType() { return warDeployerType; } public void
     * setWarDeployerType(Class warDeployerType) { this.warDeployerType = warDeployerType; }
     *
     * public void setModuleMap(Map<String, SerializableTargetModuleID> moduleMap) { this.moduleMap = moduleMap; } public File
     * getUploadDir() { return this.uploadDir; }
     *
     * public void setUploadDir(File uploadDir) { this.uploadDir = uploadDir; }
     *
     * public boolean isDeleteOnUndeploy() { return deleteOnUndeploy; }
     *
     * public void setDeleteOnUndeploy(boolean deleteOnUndeploy) { this.deleteOnUndeploy = deleteOnUndeploy; }
     *
     * public boolean isFailOnCollision() { return failOnCollision; }
     *
     * public void setFailOnCollision(boolean failOnCollision) { this.failOnCollision = failOnCollision; }
     *
     * public Map getModuleMap() { return Collections.unmodifiableMap(moduleMap); }
     *
     * public void deploy(SerializableTargetModuleID moduleID) throws Exception { String url = moduleID.getModuleID();
     *
     * // Create a status object URL deployURL = new URL(url); int contentLength =
     * deployURL.openConnection().getContentLength(); log.debug("Begin deploy, url: " + deployURL + ", contentLength: " +
     * contentLength);
     *
     * // Create the local path File path = new File(deployURL.getFile()); String archive = path.getName(); File deployFile =
     * new File(uploadDir, archive); if (failOnCollision && deployFile.exists()) throw new IOException("deployURL(" + deployURL
     * + ") collides with: " + deployFile.getPath());
     *
     * if (deployFile.exists()) Files.delete(deployFile);
     *
     * File parentFile = deployFile.getParentFile(); if (parentFile.exists() == false) { if (parentFile.mkdirs() == false) throw
     * new IOException("Failed to create local path: " + parentFile); }
     *
     * InputStream is = moduleID.getContentIS(); if( is == null ) { is = deployURL.openStream(); } BufferedInputStream bis = new
     * BufferedInputStream(is); byte[] buffer = new byte[4096]; FileOutputStream fos = new FileOutputStream(deployFile);
     * BufferedOutputStream bos = new BufferedOutputStream(fos); int count = 0; while ((count = bis.read(buffer)) > 0) {
     * bos.write(buffer, 0, count); } bis.close(); bos.close();
     *
     * moduleMap.put(url, moduleID);
     *
     * // TODO: arrange deploy_phase2 (and TC glue code). deploy_phase2(url); }
     *
     * public void hack(SerializableTargetModuleID moduleID, DeploymentContext info, String method) { try { MBeanServer server =
     * MBeanServerLocator.locateJBoss(); String objNames = getComponentName(info);
     *
     * String contexts = null; if (objNames != null) { String token = ":war="; int i = objNames.indexOf(token); if (i == -1)
     * return; i = i + token.length(); contexts = objNames.substring(i); }
     *
     * String onStr = "*:j2eeType=WebModule,*,J2EEApplication=none,J2EEServer=none"; ObjectName objectName = new
     * ObjectName(onStr); Set mbeans = server.queryNames(objectName, null); if (mbeans != null && mbeans.size () > 0 && contexts
     * != null) { for (Iterator iter = mbeans.iterator(); iter.hasNext();) { ObjectName inst = (ObjectName) iter.next(); String
     * name = inst.toString(); String token = ".*" + contexts + ".*"; if (name.matches(token)) { server.invoke(inst, method, new
     * Object[] {}, new String[] {}); } } }
     *
     *
     * } catch (Throwable e) { log.info("hack: failed" + e); } } public void start(String url) throws Exception {
     * SerializableTargetModuleID moduleID = (SerializableTargetModuleID)moduleMap.get(url); if (moduleID == null) throw new
     * IOException("deployURL(" + url + ") has not been distributed");
     *
     * if (moduleID.isRunning()) { log.error("start, url: " + url + " Already started"); return; } URL deployURL = new URL(url);
     * // Build the local path File path = new File(deployURL.getFile()); String archive = path.getName(); File deployFile = new
     * File(uploadDir, archive);
     *
     * VirtualFile root = VFS.getChild(deployFile.toURI()); Deployment deployment =
     * mainDeployer.getDeployment(getDeploymentName(root));
     *
     * // TODO: that is a hack if (deployment == null) { deployment =
     * deploymentFactory.createVFSDeployment(getDeploymentName(root), root); mainDeployer.addDeployment(deployment);
     * mainDeployer.process(); moduleID.setRunning(true); moduleID.clearChildModuleIDs(); // Repopulate the child modules
     * DeploymentContext context = mainDeployer.getDeploymentContext(deployment.getName()); fillChildrenTargetModuleID(moduleID,
     * context); }
     *
     * // hack(moduleID, context, "start"); }
     *
     * private String getComponentName(DeploymentContext context) { if (context == null) return null;
     *
     * DeploymentUnit u = context.getDeploymentUnit(); ServiceMetaData data = u.getAttachment(ServiceMetaData.class); if (data
     * == null) return null; ObjectName objectName = data.getObjectName(); return objectName.toString(); }
     *
     * public void deploy_phase2(String url) throws Exception { SerializableTargetModuleID moduleID = moduleMap.get(url); if
     * (moduleID == null) throw new IOException("deployURL(" + url + ") has not been distributed"); if (moduleID.isRunning()) {
     * return; }
     *
     * URL deployURL = new URL(url); // Build the local path File path = new File(deployURL.getFile()); String archive =
     * path.getName(); File deployFile = new File(uploadDir, archive); if (deployFile.exists() == false) throw new
     * IOException("deployURL(" + url + ") has no local archive");
     *
     * VirtualFile root = VFS.getChild(deployFile.toURI()); Deployment deployment =
     * deploymentFactory.createVFSDeployment(getDeploymentName(root), root); mainDeployer.addDeployment(deployment);
     * DeploymentContext context = null; try { mainDeployer.process(); context =
     * mainDeployer.getDeploymentContext(deployment.getName()); mainDeployer.checkComplete(deployment); } catch (Exception e) {
     * // destroy the context hack(moduleID, context, "destroy"); // clear the child moduleID.clearChildModuleIDs(); // remove
     * the deployment mainDeployer.removeDeployment(getDeploymentName(root)); mainDeployer.process();
     *
     * // remove the map if (deleteOnUndeploy) Files.delete(deployFile);
     *
     * moduleMap.remove(url); throw e; }
     *
     * moduleID.setRunning(true); moduleID.clearChildModuleIDs(); // Repopulate the child modules
     * fillChildrenTargetModuleID(moduleID, context);
     *
     * // TODO: invoke the start. // hack(moduleID, context, "start");
     *
     * }
     *
     * public void stop(String url) throws Exception { SerializableTargetModuleID moduleID =
     * (SerializableTargetModuleID)moduleMap.get(url); if (moduleID == null) throw new IOException("deployURL(" + url +
     * ") has not been distributed");
     *
     * URL deployURL = new URL(url); // Build the local path File path = new File(deployURL.getFile()); String archive =
     * path.getName(); File deployFile = new File(uploadDir, archive); if (deployFile.exists() == false) throw new
     * IOException("deployURL(" + url + ") has no local archive");
     *
     * VirtualFile root = VFS.getChild(deployFile.toURI()); mainDeployer.removeDeployment(getDeploymentName(root));
     * mainDeployer.process(); moduleID.setRunning(false); }
     *
     * public void undeploy(String url) throws Exception { SerializableTargetModuleID moduleID =
     * (SerializableTargetModuleID)moduleMap.get(url); if (moduleID == null) return;
     *
     * // If the module is still running, stop it if (moduleID.isRunning()) stop(url);
     *
     * URL deployURL = new URL(url); // Build the local path File path = new File(deployURL.getFile()); String archive =
     * path.getName(); File deployFile = new File(uploadDir, archive); if (deployFile.exists() == false) throw new
     * IOException("deployURL(" + url + ") has not been distributed");
     *
     * if (deleteOnUndeploy) Files.delete(deployFile);
     *
     * moduleMap.remove(url); }
     *
     * public SerializableTargetModuleID[] getAvailableModules(int moduleType) throws TargetException {
     * ArrayList<SerializableTargetModuleID> matches = new ArrayList<SerializableTargetModuleID>();
     * Iterator<SerializableTargetModuleID> modules = moduleMap.values().iterator(); while (modules.hasNext()) {
     * SerializableTargetModuleID module = modules.next(); if (module.getModuleType() == moduleType) matches.add(module); }
     *
     * SerializableTargetModuleID[] ids = new SerializableTargetModuleID[matches.size()]; matches.toArray(ids); return ids; }
     *
     * private void fillChildrenTargetModuleID(SerializableTargetModuleID moduleID, DeploymentContext info) {
     * List<DeploymentContext> children = info.getChildren(); for(DeploymentContext ctx : children) { try { ModuleType type =
     * getModuleType(ctx); // Discard unknown ModuleTypes if (type != null) { String module = ctx.getName(); // TODO, DEPLOYED
     * may not be the same as running? boolean isRunning = ctx.getState() == DeploymentState.DEPLOYED;
     * SerializableTargetModuleID child = new SerializableTargetModuleID(moduleID, module, type.getValue(), isRunning);
     * moduleID.addChildTargetModuleID(child); fillChildrenTargetModuleID(child, ctx); } } catch (UnsupportedOperationException
     * e) { if (log.isTraceEnabled()) log.trace("Ignoring", e); } } }
     *
     * private ModuleType getModuleType(DeploymentContext info) { ModuleType type = null; DeploymentUnit unit =
     * info.getDeploymentUnit();
     *
     * if (unit.getAttachment(carDeployerType) != null) { type = ModuleType.CAR; } else if (unit.getAttachment(ejbDeployerType)
     * != null || unit.getAttachment(ejb3DeployerType) != null) { type = ModuleType.EJB; } else if
     * (unit.getAttachment(earDeployerType) != null) { type = ModuleType.EAR; } else if (unit.getAttachment(rarDeployerType) !=
     * null) { type = ModuleType.RAR; } else if (unit.getAttachment(warDeployerType) != null) { type = ModuleType.WAR; }
     *
     * return type; }
     */
}
