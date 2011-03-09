/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.embedded.demos.ejb3;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.demos.ejb3.archive.SimpleStatefulSessionBean;
import org.jboss.as.demos.ejb3.archive.SimpleStatefulSessionLocal;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * Testcase for testing the basic functionality of a EJB3 stateful session bean.
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@Run(RunModeType.IN_CONTAINER)
public class StatefulBeanTestCase {

    @Deployment
    public static JavaArchive createDeployment() {
        // create the ejb jar
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb3-sfsb-example.jar");
        jar.addManifestResource("archives/ejb3-example.jar/META-INF/MANIFEST.MF", "MANIFEST.MF");
        jar.addPackage(SimpleStatefulSessionBean.class.getPackage());
        return jar;
    }

    /**
     * Test a basic invocation of SFSB
     *
     * @throws Exception
     */
    @Test
    public void testSFSB() throws Exception {
        Context ctx = new InitialContext();
        SimpleStatefulSessionLocal localSfsb = (SimpleStatefulSessionLocal) ctx.lookup("java:global/ejb3-sfsb-example/" + SimpleStatefulSessionBean.class.getSimpleName() + "!" + SimpleStatefulSessionLocal.class.getName());
        String state = "not in a good mood!";
        localSfsb.setState(state);

        String storedState = localSfsb.getState();
        Assert.assertEquals("Unexpected state returned from stateful session bean", state, storedState);

    }
}
