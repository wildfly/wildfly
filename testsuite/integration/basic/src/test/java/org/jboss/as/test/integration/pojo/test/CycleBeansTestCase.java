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

package org.jboss.as.test.integration.pojo.test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.pojo.support.TFactory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@RunWith(Arquillian.class)
public class CycleBeansTestCase {
    @Deployment(name = "cycle-beans")
    public static JavaArchive getCycleBeansJar() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "cycle-beans.jar");
        archive.addPackage(TFactory.class.getPackage());
        archive.addAsManifestResource(CycleBeansTestCase.class.getPackage(), "a-jboss-beans.xml", "a-jboss-beans.xml");
        archive.addAsManifestResource(CycleBeansTestCase.class.getPackage(), "b-jboss-beans.xml", "b-jboss-beans.xml");
        return archive;
    }

    @Test
    @OperateOnDeployment("cycle-beans")
    public void testCycleBeans() throws Exception {
        // TODO -- try to get beans?
    }
}
