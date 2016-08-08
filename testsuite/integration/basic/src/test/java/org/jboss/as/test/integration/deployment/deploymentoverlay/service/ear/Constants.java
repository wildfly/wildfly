/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.deployment.deploymentoverlay.service.ear;

/**
 * @author baranowb
 *
 */
public interface Constants {
    String TEST_MODULE_NAME = "OverlayTest-ear";
    String TEST_MODULE_NAME_FULL = "test." + TEST_MODULE_NAME;
    
    String DEPLOYMENT_NAME_TESTER = "tester";
    String DEPLOYMENT_JAR_NAME_COUNTER = DEPLOYMENT_NAME_TESTER + ".jar";
    
    String DEPLOYMENT_NAME_TESTER_WRAPPER = "tester-wrapper";
    String DEPLOYMENT_EAR_NAME_TESTER_WRAPPER = DEPLOYMENT_NAME_TESTER_WRAPPER + ".ear";
    
    String DEPLOYMENT_NAME_OVERLAYED = "overlayed";
    String DEPLOYMENT_JAR_NAME_OVERLAYED = DEPLOYMENT_NAME_OVERLAYED + ".jar";
    
    String OVERLAY="FINE_COAT";
    String OVERLAY_RESOURCE="/"+DEPLOYMENT_JAR_NAME_OVERLAYED+"//META-INF/services/org.jboss.ejb.client.EJBClientInterceptor";
    String OVERLAYED_CONTENT="org.jboss.as.test.integration.deployment.deploymentoverlay.service.ear.OverlayedInterceptor";
    
    String TESTIMONY_EJB="java:global/"+DEPLOYMENT_NAME_TESTER+"/TestimonyEJB!org.jboss.as.test.integration.deployment.deploymentoverlay.service.jar.InterceptorTestimony";
}
