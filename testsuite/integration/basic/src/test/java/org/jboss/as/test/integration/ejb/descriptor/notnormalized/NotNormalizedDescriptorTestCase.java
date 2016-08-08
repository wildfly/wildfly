/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.descriptor.notnormalized;

import javax.naming.NamingException;


import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.ejb.home.localhome.SimpleLocalHome;
import org.jboss.as.test.integration.ejb.home.remotehome.SimpleHome;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jboss.as.test.integration.ejb.home.localhome.descriptor.SimpleStatelessLocalBean;
import org.jboss.as.test.integration.ejb.home.localhome.descriptor.SimpleStatefulLocalBean;
import org.jboss.as.test.integration.ejb.home.remotehome.descriptor.SimpleStatelessBean;
import org.jboss.as.test.integration.ejb.home.remotehome.descriptor.SimpleStatefulBean;
import org.jboss.as.test.integration.ejb.entity.bmp.SimpleBMPBean;
/**
 * Simple test case to check if AS7-1906 wont come back.
 * @author baranowb
 */
@RunWith(Arquillian.class)
public class NotNormalizedDescriptorTestCase {
    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb-not-normalized-descriptor-test.jar")
            //test class and intercepteros    
            .addPackage(NotNormalizedDescriptorTestCase.class.getPackage())
            //2.1 local and remote
            .addPackage(SimpleLocalHome.class.getPackage())
            .addPackage(SimpleHome.class.getPackage())
            .addClass(SimpleStatelessLocalBean.class)
            .addClass(SimpleStatefulLocalBean.class)
            .addClass(SimpleStatelessBean.class)
            .addClass(SimpleStatefulBean.class)
            //2.1 entity
            .addPackage(SimpleBMPBean.class.getPackage())
            //3 ??
            .addAsManifestResource(NotNormalizedDescriptorTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }


    @Test
    public void testDeploy() throws NamingException {
        //test only if archive deploys properly.
    }
}
