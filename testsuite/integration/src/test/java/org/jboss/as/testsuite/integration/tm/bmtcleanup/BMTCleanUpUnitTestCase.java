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
package org.jboss.as.testsuite.integration.tm.bmtcleanup;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJBException;
import javax.inject.Inject;

/**
 * Tests for BMT CleanUp
 *
 * @author adrian@jboss.com
 * @author istudens@redhat.com
 */
@RunWith(Arquillian.class)
public class BMTCleanUpUnitTestCase
{
    public static final String ARCHIVE_NAME = "bmtcleanuptest";

    @Inject
    private BMTCleanUpBean bean;

    @Deployment
    public static JavaArchive deploy() {
        return ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar")
                .addPackage(BMTCleanUpUnitTestCase.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"));
    }

    @Test
    public void testNormal() throws Exception
    {
        bean.doNormal();
    }

    @Test
    public void testIncomplete() throws Exception
    {
        try
        {
            bean.doIncomplete();
        }
        catch (EJBException expected)
        {
            // expected
        }
        bean.doNormal();
    }

    @Test
    public void testTxTimeout() throws Exception
    {
        try
        {
            bean.doTimeout();
        }
        catch (EJBException expected)
        {
            // expected
        }
        bean.doNormal();
    }

}
