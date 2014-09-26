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

package org.jboss.as.test.integration.deployment.deploymentoverlay.service.war;

import java.util.List;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.deployment.deploymentoverlay.service.jar.InterceptorTestimony;
import org.jboss.as.test.integration.deployment.deploymentoverlay.service.jar.InterceptorType;
import org.jboss.as.test.integration.deployment.deploymentoverlay.service.jar.OverlayUtils;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author baranowb
 *
 */
@RunWith(Arquillian.class)
@ServerSetup(SetupModuleServerSetupTask.class)
public class OverlayExistingServiceTestCase extends ServiceOverlayTestCaseBase{
    private static final Logger logger = Logger.getLogger(OverlayExistingServiceTestCase.class);
    
    @Deployment(name = Constants.DEPLOYMENT_NAME_TESTER, order = 0, managed = true, testable = true)
    public static Archive<?> getTestArchive() throws Exception {
        final Archive<?> jar = createTestArchive(OverlayExistingServiceTestCase.class);
        logger.info(jar.toString(true));
        return jar;
    }
    
    @Deployment(name = Constants.DEPLOYMENT_NAME_TESTER_WRAPPER, order = 1, managed = false, testable = false)
    public static Archive<?> getInterceptedArchive() {
        final Archive<?> war = createWARWithOverlayedArchive(true);
        logger.info(war.toString(true));
        return war;
    }

    /**
     * 
     * @throws Exception
     */
    @Test
    public void testOverlayed() throws Exception {
        try {
            InitialContext ctx = new InitialContext();
            InterceptorTestimony test = (InterceptorTestimony) ctx.lookup(Constants.TESTIMONY_EJB);
            deployer.deploy(Constants.DEPLOYMENT_NAME_TESTER_WRAPPER);
            OverlayUtils.setupOverlay(super.managementClient,Constants.DEPLOYMENT_EAR_NAME_TESTER_WRAPPER,Constants.OVERLAY,Constants.OVERLAY_RESOURCE,Constants.OVERLAYED_CONTENT);
            List<InterceptorType> testimony = test.getTestimony();
            Assert.assertEquals("Wrong size:"+testimony,2,testimony.size());
            Assert.assertEquals("Wrong value:"+testimony.get(0),InterceptorType.DEPLOYED,testimony.get(0));
            Assert.assertEquals("Wrong value:"+testimony.get(1),InterceptorType.OVERLAYED,testimony.get(1));
        } finally {
            OverlayUtils.removeOverlay(super.managementClient,Constants.DEPLOYMENT_EAR_NAME_TESTER_WRAPPER,Constants.OVERLAY,Constants.OVERLAY_RESOURCE);
            deployer.undeploy(Constants.DEPLOYMENT_NAME_TESTER_WRAPPER);
        }
    }
}
