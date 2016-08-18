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

package org.jboss.as.test.integration.jpa.hibernate.classfiletransformertest;

import static org.junit.Assert.assertTrue;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.jpa.hibernate.Employee;
import org.jboss.as.test.integration.jpa.hibernate.SFSB1;
import org.jboss.as.test.integration.jpa.hibernate.SFSBHibernateSession;
import org.jboss.as.test.integration.jpa.hibernate.SFSBHibernateSessionFactory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Hibernate "hibernate.ejb.use_class_enhancer" test that causes hibernate to add a
 * javax.persistence.spi.ClassTransformer to the pu.
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class ClassFileTransformerTestCase {

    private static final String ARCHIVE_NAME = "jpa_ClassFileTransformerTestCase";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(ClassFileTransformerTestCase.class,
                Employee.class,
                SFSB1.class,
                SFSBHibernateSession.class,
                SFSBHibernateSessionFactory.class
        );
        jar.addAsManifestResource(ClassFileTransformerTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    @Test
    public void testhibernate_ejb_use_class_enhancer() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        sfsb1.createEmployee("Kelly Smith", "Watford, England", 10);
        sfsb1.createEmployee("Alex Scott", "London, England", 20);
        Employee emp = sfsb1.getEmployeeNoTX(10);

        assertTrue("was able to read database row with hibernate.ejb.use_class_enhancer enabled", emp != null);
    }

}
