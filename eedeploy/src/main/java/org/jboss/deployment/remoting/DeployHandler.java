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
 * A remoting StreamInvocationHandler installed as the JSR88 subsystem handler and used by the StreamingTarget implementation.
 *
 * @author Scott.Stark@jboss.org
 *
 */
public class DeployHandler { // implements StreamInvocationHandler

    /*
     * static Logger log = Logger.getLogger(DeployHandler.class);
     *
     * private DeploymentManagerServiceMBean deployService;
     *
     * public DeploymentManagerServiceMBean getDeployService() { return deployService; }
     *
     * public void setDeployService(DeploymentManagerServiceMBean deployService) { this.deployService = deployService; }
     *
     * /** Handle a deployService deploy invocation
     *
     * @param request - the remoting invocation public Object handleStream(InputStream contentIS, InvocationRequest request)
     * throws Throwable { SerializableTargetModuleID moduleID = (SerializableTargetModuleID) request.getParameter();
     * log.debug("handleStream, moduleID: "+moduleID); moduleID.setContentIS(contentIS); deployService.deploy(moduleID); return
     * null; }
     *
     * public void addListener(InvokerCallbackHandler arg0) { }
     *
     * /** Handle a deployService invocation other than deploy
     *
     * @param request - the remoting invocation public Object invoke(InvocationRequest request) throws Throwable { String name =
     * request.getParameter().toString(); Map payload = request.getRequestPayload(); String url = (String)
     * payload.get("moduleID"); Object returnValue = null; log.debug("invoke, moduleID: "+url+", payload: "+payload);
     *
     * if( name.equals("start") ) { deployService.start(url); } else if( name.equals("stop") ) { deployService.stop(url);
     *
     * } else if( name.equals("undeploy") ) { deployService.undeploy(url); } else if( name.equals("getAvailableModules") ) {
     * Integer moduleType = (Integer) payload.get("moduleType"); SerializableTargetModuleID[] ids =
     * deployService.getAvailableModules(moduleType); returnValue = ids; } return returnValue; }
     *
     * public void removeListener(InvokerCallbackHandler arg0) { }
     *
     * public void setInvoker(ServerInvoker arg0) { }
     *
     * /** Legacy initialzation of the DeploymentManagerServiceMBean via jmx lookup. public void setMBeanServer(MBeanServer
     * server) { try { ObjectName jsr88 = new
     * ObjectName("jboss.management.local:type=JSR88DeploymentManager,name=DefaultManager"); deployService =
     * (DeploymentManagerServiceMBean) MBeanServerInvocationHandler.newProxyInstance(server, jsr88,
     * DeploymentManagerServiceMBean.class, false); log.debug("Initialzied DeploymentManagerServiceMBean"); } catch(Exception e)
     * { log.warn("Failed to lookup DeploymentManagerServiceMBean", e); } }
     */
}
