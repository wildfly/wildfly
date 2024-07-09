/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.dependson.session;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.ejb.singleton.dependson.mdb.CallCounterProxy;
import org.jboss.as.test.integration.ejb.singleton.dependson.mdb.Constants;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author baranowb
 */
@ServerSetup({StatelesBeanWhichDependsOnTestCase.SetupTask.class})
@RunWith(Arquillian.class)
public class StatelesBeanWhichDependsOnTestCase extends SessionWhichDependeOnTestCaseBase {
    private static final String MODULE_NAME = StatelesBeanWhichDependsOnTestCase.class.getName();

    public static class SetupTask extends SetupModuleServerSetupTask {

        public SetupTask() {
            super(MODULE_NAME);
        }
    }

    @ArquillianResource
    InitialContext ctx;

    @Override
    protected Trigger getTrigger() throws Exception {
        return (Trigger) ctx.lookup(SessionConstants.EJB_STATELES);
    }

    @Deployment(name = Constants.DEPLOYMENT_NAME_COUNTER, order = 0, managed = true, testable = true)
    public static Archive<?> getTestArchive() throws Exception {
        JavaArchive jar = getTestArchiveBase(MODULE_NAME);
        jar.addClass(StatelesBeanWhichDependsOnTestCase.class);
        return jar;
    }

    @Deployment(name = SessionConstants.DEPLOYMENT_NAME_SESSION, order = 1, managed = false, testable = false)
    public static Archive<?> getSessionArchive() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, SessionConstants.DEPLOYMENT_JAR_NAME_SESSION);
        jar.addClass(Constants.class);
        jar.addClass(CallCounterProxy.class);
        jar.addClass(SessionConstants.class);
        jar.addClass(BeanBase.class);
        jar.addClass(StatelesBeanWhichDependsOn.class);
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr, "
                + MODULE_NAME + "\n"), "MANIFEST.MF");
        return jar;
    }

    @Test
    public void test() throws Exception {
        super.testSessionBean();
    }
}
