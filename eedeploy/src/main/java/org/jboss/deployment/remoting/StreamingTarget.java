/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.deployment.remoting;

/**
 * A JBossTarget implementation that uses remoting streaming to upload deployments. This target is selected by including a
 * targetType=remote param in the DeploymentManager deployURI.
 *
 * @author Scott.Stark@jboss.org
 *
 */
public class StreamingTarget { // implements JBossTarget

    /*
     * private static final Logger log = Logger.getLogger(StreamingTarget.class); public static final String DESCRIPTION =
     * "JBoss remoting streaming deployment target";
     *
     * // The default remoting subsystem private static final String REMOTING_SUBSYSTEM = "JSR88";
     *
     * // The deployment target uri private URI deployURI;
     *
     * /* Create a target given a remoting locator URI
     *
     * @param deployURI public StreamingTarget(URI deployURI) { log.debug("new StreamingTarget: " + deployURI); try { String
     * localHostName = InetAddress.getLocalHost().getHostName();
     *
     * String scheme = deployURI.getScheme(); String host = deployURI.getHost(); int port = deployURI.getPort(); String query =
     * deployURI.getRawQuery();
     *
     * String uri = deployURI.toASCIIString(); if (uri.startsWith(DeploymentManagerImpl.DEPLOYER_URI)) { // Using unified
     * invoker remoting defaults scheme = "socket"; host = localHostName; port = 4446; query = null; }
     *
     * StringBuffer tmp = new StringBuffer(scheme + "://"); tmp.append(host != null ? host : localHostName); tmp.append(port > 0
     * ? ":" + port : ""); tmp.append(query != null ? "/?" + query : ""); deployURI = new URI(tmp.toString());
     *
     * log.debug("URI changed to: " + deployURI); this.deployURI = deployURI; } catch (Exception e) { log.error(e); }
     *
     * }
     *
     * /** Get the target's description
     *
     * @return the description public String getDescription() { return DESCRIPTION; }
     *
     * /** Get the target's name
     *
     * @return the name public String getName() { return deployURI.toString(); }
     *
     * /** Get the target's host name public String getHostName() { return deployURI.getHost(); }
     *
     * /** Deploy a given module public void deploy(TargetModuleID targetModuleID) throws Exception { TargetModuleIDImpl
     * moduleID = (TargetModuleIDImpl)targetModuleID; SerializableTargetModuleID smoduleID = new
     * SerializableTargetModuleID(moduleID); Client client = getClient(); log.info("Begin deploy: " + moduleID);
     * transferDeployment(client, smoduleID); log.info("End deploy"); client.disconnect(); }
     *
     * /** Start a given module public void start(TargetModuleID targetModuleID) throws Exception { Client client = getClient();
     * URL url = new URL(targetModuleID.getModuleID()); log.debug("Start: " + url); HashMap<String, String> args = new
     * HashMap<String, String>(1); args.put("moduleID", url.toExternalForm()); log.info("Begin start: " + url); invoke(client,
     * "start", args); log.info("End start"); }
     *
     * /** Stop a given module public void stop(TargetModuleID targetModuleID) throws Exception { Client client = getClient();
     * URL url = new URL(targetModuleID.getModuleID()); log.debug("Start: " + url); HashMap<String, String> args = new
     * HashMap<String, String>(1); args.put("moduleID", url.toExternalForm()); log.info("Begin stop: " + url); invoke(client,
     * "stop", args); log.info("End stop"); }
     *
     * /** Undeploy a given module public void undeploy(TargetModuleID targetModuleID) throws Exception { Client client =
     * getClient(); URL url = new URL(targetModuleID.getModuleID()); log.debug("Start: " + url); HashMap<String, String> args =
     * new HashMap<String, String>(1); args.put("moduleID", url.toExternalForm()); log.info("Begin undeploy: " + url);
     * invoke(client, "undeploy", args); log.info("End undeploy"); }
     *
     * /** Retrieve the list of all J2EE application modules running or not running on the identified targets. public
     * TargetModuleID[] getAvailableModules(ModuleType moduleType) throws TargetException { try { Client client = getClient();
     * HashMap<String, Integer> args = new HashMap<String, Integer>(1); args.put("moduleType", moduleType.getValue());
     * SerializableTargetModuleID[] modules = (SerializableTargetModuleID[]) invoke(client, "getAvailableModules", args);
     * List<TargetModuleID> list = new ArrayList<TargetModuleID>(); for (int n = 0; n < modules.length; n++) {
     * SerializableTargetModuleID id = modules[n]; String moduleID = id.getModuleID(); boolean isRunning = id.isRunning();
     * ModuleType type = ModuleType.getModuleType(id.getModuleType()); TargetModuleIDImpl tmid = new TargetModuleIDImpl(this,
     * moduleID, null, isRunning, type); convertChildren(tmid, id); list.add(tmid); }
     *
     * TargetModuleID[] targetModuleIDs = new TargetModuleID[list.size()]; list.toArray(targetModuleIDs); return
     * targetModuleIDs; } catch (Exception e) { TargetException tex = new TargetException("Failed to get available modules");
     * tex.initCause(e); throw tex; } }
     *
     * private void convertChildren(TargetModuleIDImpl parent, SerializableTargetModuleID parentID) {
     * SerializableTargetModuleID[] children = parentID.getChildModuleIDs(); int length = children != null ? children.length :
     * 0; for (int n = 0; n < length; n++) { SerializableTargetModuleID id = children[n]; String moduleID = id.getModuleID();
     * boolean isRunning = id.isRunning(); ModuleType type = ModuleType.getModuleType(id.getModuleType()); TargetModuleIDImpl
     * child = new TargetModuleIDImpl(this, moduleID, parent, isRunning, type); parent.addChildTargetModuleID(child);
     * convertChildren(child, id); } }
     *
     * /** Get the remoting client connection
     *
     * @return
     *
     * @throws Exception private Client getClient() throws Exception { String locatorURI = deployURI.toString(); InvokerLocator
     * locator = new InvokerLocator(locatorURI); log.debug("Calling remoting server with locator uri of: " + locatorURI);
     *
     * Client remotingClient = new Client(locator, REMOTING_SUBSYSTEM); remotingClient.connect(); return remotingClient; }
     *
     * private void transferDeployment(Client client, SerializableTargetModuleID smoduleID) throws Exception { URL deployURL =
     * new URL(smoduleID.getModuleID()); InputStream is = deployURL.openStream(); try { client.invoke(is, smoduleID); }
     * catch(Exception e) { throw e; } catch(Error e) { throw new RuntimeException(e); } catch(Throwable e) { throw new
     * RuntimeException(e); } finally { is.close(); } }
     *
     * private Object invoke(Client client, String name, HashMap args) throws Exception { try { Object returnValue =
     * client.invoke(name, args); return returnValue; } catch(Exception e) { throw e; } catch(Error e) { throw new
     * RuntimeException(e); } catch(Throwable e) { throw new RuntimeException(e); } }
     */
}
