/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.testsuite.integration.secman;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.testsuite.integration.secman.ejbs.ReadSystemPropertyLocal;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;

/**
 * Test case, which checks PropertyPermissions assigned to a deployed EJB application
 * getting EJBs locally. The application try to do a protected action and it should
 * either complete successfully if {@link java.util.PropertyPermission} is granted, or fail.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
@RunWith(Arquillian.class)
public class LocalEJBPropertyGrantPermissionReadTestCase extends AbstractLocalEJBPropertyPermissionReadTestCase {

    @EJB(lookup="java:module/ReadSystemPropertyBean!org.jboss.as.testsuite.integration.secman.ejbs.ReadSystemPropertyLocal")
    ReadSystemPropertyLocal beanGranted;

    /**
     * Creates archive with a tested application.
     *
     * @return {@link org.jboss.shrinkwrap.api.spec.JavaArchive} instance
     */
    @Deployment
    public static JavaArchive deployment() {
        return grantEjbDeployment();
    }

    /**
     * Check standard java property access in application, where PropertyPermission for all properties is granted.
     *
     * @throws Exception
     */
    @Test
    public void testJavaHomePropertyGrant() throws Exception {
        checkJavaHomeProperty(beanGranted, false);
    }

}
