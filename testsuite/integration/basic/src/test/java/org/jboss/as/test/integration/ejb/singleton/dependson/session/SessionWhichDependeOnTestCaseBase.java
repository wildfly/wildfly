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

package org.jboss.as.test.integration.ejb.singleton.dependson.session;

import javax.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.ejb.singleton.dependson.mdb.CallCounterInterface;
import org.jboss.as.test.integration.ejb.singleton.dependson.mdb.CallCounterSingleton;
import org.jboss.as.test.integration.ejb.singleton.dependson.mdb.Constants;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;

/**
 * @author baranowb
 */

public abstract class SessionWhichDependeOnTestCaseBase {

    protected static final Logger logger = Logger.getLogger(SessionWhichDependeOnTestCaseBase.class);

    @ArquillianResource
    Deployer deployer;

    @EJB
    private CallCounterInterface counter;

    protected abstract Trigger getTrigger() throws Exception;

    protected static JavaArchive getTestArchiveBase() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, Constants.DEPLOYMENT_JAR_NAME_COUNTER);
        jar.addClass(CallCounterSingleton.class);
        jar.addClass(SessionConstants.class);
        jar.addClass(Constants.class);
        jar.addClass(SetupModuleServerSetupTask.class);
        jar.addClass(SessionWhichDependeOnTestCaseBase.class);
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr, "
                + SessionConstants.TEST_MODULE_NAME_FULL + "\n"), "MANIFEST.MF");
        return jar;
    }

    public void testSessionBean() throws Exception {
        this.deployer.deploy(SessionConstants.DEPLOYMENT_NAME_SESSION);
        getTrigger().trigger();
        this.deployer.undeploy(SessionConstants.DEPLOYMENT_NAME_SESSION);
        Assert.assertTrue("PostConstruct not called!", counter.isPostConstruct());
        Assert.assertTrue("Message not called!", counter.isMessage());
        Assert.assertTrue("PreDestroy not called!", counter.isPreDestroy());
    }
}
