/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.build.plugin.configassembly;

import java.io.File;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Thomas.Diesler@jboss.com
 * @since 06-Sep-2012
 */
public class GenerateSubsystemsDefinitionTestCase {

    @Test
    public void testSubsystemDefinition() throws Exception {

        String spec = "osgi:eager,web";
        String profiles = "";
        String prefix = "subsystems";
        String filename = "target/subsystems-osgi.xml";
        GenerateSubsystemsDefinition.main(new String[] {spec, profiles, prefix, filename});

        File outfile = new File(filename);
        Assert.assertTrue("File exists: " + outfile, outfile.exists());

        SubsystemsParser parser = new SubsystemsParser(outfile);
        parser.parse();

        SubsystemConfig[] configs = parser.getSubsystemConfigs().get("");
        Assert.assertEquals("Two subsystems", 2, configs.length);
        Assert.assertEquals(prefix + "/osgi.xml", configs[0].getSubsystem());
        Assert.assertEquals("eager", configs[0].getSupplement());
        Assert.assertEquals(prefix + "/web.xml", configs[1].getSubsystem());
        Assert.assertNull(configs[1].getSupplement());
    }
}
