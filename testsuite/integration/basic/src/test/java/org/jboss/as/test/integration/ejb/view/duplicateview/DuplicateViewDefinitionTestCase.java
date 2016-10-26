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
package org.jboss.as.test.integration.ejb.view.duplicateview;

import javax.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@RunWith(Arquillian.class)
public class DuplicateViewDefinitionTestCase {
    @Deployment
    public static Archive<?> deployment() {

        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "ejb3as7-1012.jar")
                .addPackage(AnnotatedDoNothingBean.class.getPackage())
                .addPackage(DoNothingBean.class.getPackage())
                .addAsManifestResource(DuplicateViewDefinitionTestCase.class.getPackage(), "ejb-jar.xml");
        return archive;
    }

    @EJB(mappedName = "java:global/ejb3as7-1012/AnnotatedDoNothingBean!org.jboss.as.test.integration.ejb.view.duplicateview.DoNothing")
    private DoNothing bean;

    @Test
    public void testDoNothing() {
        // if it deploys, we are good to go
        bean.doNothing();
    }
}
