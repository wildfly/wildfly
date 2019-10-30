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
@ServerSetup({SetupModuleServerSetupTask.class})
@RunWith(Arquillian.class)
public class StatefulBeanWhichDependsOnTestCase extends SessionWhichDependeOnTestCaseBase {

    @ArquillianResource
    InitialContext ctx;

    @Override
    protected Trigger getTrigger() throws Exception {
        return (Trigger) ctx.lookup(SessionConstants.EJB_STATEFUL);
    }

    @Deployment(name = Constants.DEPLOYMENT_NAME_COUNTER, order = 0, managed = true, testable = true)
    public static Archive<?> getTestArchive() throws Exception {
        JavaArchive jar = getTestArchiveBase();
        jar.addClass(StatefulBeanWhichDependsOnTestCase.class);
        return jar;
    }

    @Deployment(name = SessionConstants.DEPLOYMENT_NAME_SESSION, order = 1, managed = false, testable = false)
    public static Archive<?> getSessionArchive() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, SessionConstants.DEPLOYMENT_NAME_SESSION);
        jar.addClass(Constants.class);
        jar.addClass(CallCounterProxy.class);
        jar.addClass(SessionConstants.class);
        jar.addClass(BeanBase.class);
        jar.addClass(StatefulBeanWhichDependsOn.class);
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr, "
                + SessionConstants.TEST_MODULE_NAME_FULL + "\n"), "MANIFEST.MF");
        return jar;
    }

    @Test
    public void test() throws Exception {
        super.testSessionBean();
    }
}
