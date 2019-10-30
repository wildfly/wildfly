/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.datasources.agroal;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;

import java.io.IOException;

/**
 * This is the bare bones test example that tests subsystem
 * It does same things that {@link SubsystemParsingTestCase} does but most of internals are already done in AbstractSubsystemBaseTest
 * If you need more control over what happens in tests look at {@link SubsystemParsingTestCase}
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class SubsystemBaseParsingTestCase extends AbstractSubsystemBaseTest {

    public SubsystemBaseParsingTestCase() {
        super(AgroalExtension.SUBSYSTEM_NAME, new AgroalExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return "<subsystem xmlns=\"" + AgroalNamespace.CURRENT.getUriString() + "\"/>";
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-agroal_1_0.xsd";
    }
}
