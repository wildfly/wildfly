/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.transaction.annotation;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;

/**
 * Test that a bean which uses {@link javax.ejb.TransactionManagement} annotation
 * without any explicit value, doesn't cause a deployment failure.
 *
 * @see https://issues.jboss.org/browse/AS7-1506
 *      <p/>
 *      User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class NoExplicitTransactionManagementValueTestCase {

    @EJB(mappedName = "java:module/BeanWithoutTransactionManagementValue")
    private BeanWithoutTransactionManagementValue bean;

    @Deployment
    public static Archive<?> createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "as7-1506.jar");
        jar.addClass(BeanWithoutTransactionManagementValue.class);

        return jar;
    }

    /**
     * Test that the deployment of the bean was successful and invocation on the bean
     * works
     */
    @Test
    public void testSuccessfulDeployment() {
        this.bean.doNothing();
    }
}
