/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.injection.multiple.view;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;

/**
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class WSViewEJBInjectionTestCase {

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "multiple-view-ejb-injection");
        jar.addPackage(WSViewEJBInjectionTestCase.class.getPackage());
        return jar;
    }

    @Test
    public void testInjection() throws Exception {
        final InjectingBean injectingBean = InitialContext.doLookup("java:module/" + InjectingBean.class.getSimpleName() + "!" + InjectingBean.class.getName());
        Assert.assertTrue("@EJB injection did not happen in bean", injectingBean.isBeanInjected());
        // try an invocation on the bean
        injectingBean.invokeInjectedBean();
    }
}
