/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.connector.subsystems.complextestcases;


import org.jboss.as.connector.subsystems.jca.JcaExtension;
import org.junit.Test;

/**
 *
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 */

public class ComplexJcaSubsystemTestCase extends AbstractComplexSubsystemTestCase {

    public ComplexJcaSubsystemTestCase() {
        super(JcaExtension.SUBSYSTEM_NAME, new JcaExtension());
    }

    @Test
    public void testModel() throws Exception {

        getModel("jca.xml");
    }

    @Test
    public void testMinModel() throws Exception {

        getModel("minimal-jca.xml");
    }


}
