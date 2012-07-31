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

package org.jboss.as.test.integration.jpa.basic.multiplepersistenceunittest;

import java.util.Map;

import javax.ejb.EJB;

import junit.framework.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.jpa.basic.SLSBPU1;
import org.jboss.as.test.integration.jpa.basic.SLSBPU2;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Ensure that both pu definitions can be used.
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class MultiplePuTestCase {

    private static final String ARCHIVE_NAME = "MultiplePuTestCase";

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(MultiplePuTestCase.class, SLSBPU1.class, SLSBPU2.class);
        jar.addAsManifestResource(MultiplePuTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    @EJB(mappedName = "java:global/" + ARCHIVE_NAME + "/SLSBPU1")
    private SLSBPU1 slsbpu1;

    @EJB(mappedName = "java:global/" + ARCHIVE_NAME + "/SLSBPU2")
    private SLSBPU2 slsbpu2;


    @Test
    public void testBothPersistenceUnitDefinitions() throws Exception {
        Map<String, Object> sl1Props = slsbpu1.getEMInfo();
        Map<String, Object> sl2Props = slsbpu2.getEMInfo();

        Assert.assertEquals("wrong pu " ,sl1Props.get("PersistenceUnitName"),"pu1");
        Assert.assertEquals("wrong pu ", sl2Props.get("PersistenceUnitName"),"pu2");
    }

}
