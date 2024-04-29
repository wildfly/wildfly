/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.dependson.session;

import jakarta.ejb.EJB;

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

    protected static JavaArchive getTestArchiveBase(final String moduleName) throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, Constants.DEPLOYMENT_JAR_NAME_COUNTER);
        jar.addClass(CallCounterSingleton.class);
        jar.addClass(SessionConstants.class);
        jar.addClass(Constants.class);
        jar.addClass(SetupModuleServerSetupTask.class);
        jar.addClass(SessionWhichDependeOnTestCaseBase.class);
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr, "
                + moduleName + "\n"), "MANIFEST.MF");
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
