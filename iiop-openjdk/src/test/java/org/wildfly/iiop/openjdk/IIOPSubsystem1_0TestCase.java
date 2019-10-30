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
package org.wildfly.iiop.openjdk;

import java.io.IOException;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.KernelServices;

/**
 * <ṕ>
 * IIOP subsystem tests.
 * </ṕ>
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="sguilhen@jboss.com">Stefan Guilhen</a>
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class IIOPSubsystem1_0TestCase extends AbstractSubsystemBaseTest {

    public IIOPSubsystem1_0TestCase() {
        super(IIOPExtension.SUBSYSTEM_NAME, new IIOPExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem-1.0.xml");
    }

    @Override
    protected KernelServices standardSubsystemTest(String configId, boolean compareXml) throws Exception {
        return super.standardSubsystemTest(configId, false);
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-iiop-openjdk_1_0.xsd";
    }

}
