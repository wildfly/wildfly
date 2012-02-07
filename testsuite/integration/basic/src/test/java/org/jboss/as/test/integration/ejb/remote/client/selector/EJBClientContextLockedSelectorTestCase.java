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

package org.jboss.as.test.integration.ejb.remote.client.selector;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * Tests that server side application code cannot change
 * {@link org.jboss.ejb.client.EJBClientContext#setSelector(org.jboss.ejb.client.ContextSelector) EJB client context selector}
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class EJBClientContextLockedSelectorTestCase {

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb-client-context-locked-selector-test.jar");
        jar.addPackage(EJBClientContextSelectorChangingBean.class.getPackage());
        return jar;
    }

    /**
     * Tests that user application code cannot set EJB client context selector on the server side
     *
     * @throws Exception
     */
    @Test
    public void testChangingLockedEJBClientContextSelector() throws Exception {
        final Context ctx = new InitialContext();
        final EJBClientContextSelectorChangingBean bean = (EJBClientContextSelectorChangingBean) ctx.lookup("java:module/" + EJBClientContextSelectorChangingBean.class.getSimpleName() + "!" + EJBClientContextSelectorChangingBean.class.getName());
        bean.changeLockedSelector();
    }
}
