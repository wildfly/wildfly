/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.health;

import java.io.IOException;
import java.util.Properties;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class Subsystem_1_0_ParsingTestCase extends AbstractSubsystemBaseTest {

    public Subsystem_1_0_ParsingTestCase() {
        super(MicroProfileHealthExtension.SUBSYSTEM_NAME, new MicroProfileHealthExtension());
    }


    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem_1_0.xml");
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[] {
                "/subsystem-templates/microprofile-health-smallrye.xml"
        };
    }

    @Override
    protected String getSubsystemXsdPath() throws IOException {
        return "schema/wildfly-microprofile-health-smallrye_1_0.xsd";
    }

    protected Properties getResolvedProperties() {
        return System.getProperties();
    }



}