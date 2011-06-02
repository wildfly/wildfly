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

package org.jboss.as.test.spec.ejb3;

import java.util.logging.Logger;

import javax.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.demos.ejb3.archive.EchoService;
import org.jboss.as.demos.ejb3.archive.SimpleInterceptor;
import org.jboss.as.demos.ejb3.archive.SimpleStatefulSessionBean;
import org.jboss.as.demos.ejb3.archive.SimpleStatefulSessionLocal;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testcase for testing the basic functionality of a EJB3 stateful session bean.
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class StatefulBeanTestCase {

    private static final Logger log = Logger.getLogger(StatefulBeanTestCase.class.getName());
    
    @Deployment
    public static JavaArchive createDeployment() {
        // create the ejb jar
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb3-sfsb-example.jar");
        jar.addClasses(SimpleStatefulSessionBean.class, SimpleStatefulSessionLocal.class, SimpleInterceptor.class,
                EchoService.class);
        log.info(jar.toString(true));
        return jar;
    }

    @EJB(mappedName="java:global/test/SimpleStatefulSessionBean!org.jboss.as.demos.ejb3.archive.SimpleStatefulSessionLocal")
    private SimpleStatefulSessionLocal localSfsb;
    
    /**
     * Test a basic invocation of SFSB
     *
     * @throws Exception
     */
    @Test
    public void testSFSB() throws Exception {
        String state = "not in a good mood!";
        localSfsb.setState(state);
        String storedState = localSfsb.getState();
        Assert.assertEquals("Unexpected state returned from stateful session bean", SimpleInterceptor.class.getSimpleName() + "#" + state, storedState);

    }
}
