/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.testsuite.integration.tm.test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import org.jboss.as.testsuite.integration.tm.ejb.BMTCleanUpBean;

/**
 * Tests for BMT CleanUp
 *
 * @author adrian@jboss.com
 * @author istudens@redhat.com
 */
@RunWith(Arquillian.class)
public class BMTCleanUpUnitTestCase
{
    public static final String ARCHIVE_NAME = "bmtcleanuptest"
    @Inject
    BMTCleanUpBean bmtCleanUpBean;

    @Deployment
    public static JavaArchive deploy() {
        return ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar")
                .addClasses(BMTCleanUpBean.class);
    }

    @Test
    public void testIncomplete() throws Exception
    {
        bmtCleanUpBean.testIncomplete();
    }

    @Test
    public void testTxTimeout() throws Exception
    {
        bmtCleanUpBean.testTxTimeout();
    }

}
