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

import static org.wildfly.build.plugin.configassembly.GenerateModulesDefinition.NO_MODULE_DEPENENCIES;
import static org.wildfly.build.plugin.configassembly.GenerateModulesDefinition.SKIP_SUBSYSTEMS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Thomas.Diesler@jboss.com
 * @since 06-Sep-2012
 */
public class GenerateModulesDefinitionTestCase {

    @Test
    public void testModulesSplit() throws Exception {

        String[] split = "org.acme.foo".split(GenerateModulesDefinition.SPLIT_PATTERN);
        Assert.assertEquals(1, split.length);
        Assert.assertEquals("org.acme.foo", split[0]);

        split = "org.acme.foo,org.acme.bar".split(GenerateModulesDefinition.SPLIT_PATTERN);
        Assert.assertEquals(2, split.length);
        Assert.assertEquals("org.acme.foo", split[0]);
        Assert.assertEquals("org.acme.bar", split[1]);

        split = "org.acme.foo, org.acme.bar".split(GenerateModulesDefinition.SPLIT_PATTERN);
        Assert.assertEquals(2, split.length);
        Assert.assertEquals("org.acme.foo", split[0]);
        Assert.assertEquals("org.acme.bar", split[1]);

        split = "\n  \torg.acme.foo, \torg.acme.bar".trim().split(GenerateModulesDefinition.SPLIT_PATTERN);
        Assert.assertEquals(2, split.length);
        Assert.assertEquals("org.acme.foo", split[0]);
        Assert.assertEquals("org.acme.bar", split[1]);
    }

    @Test
    public void testModuleDefinition() throws Exception {

        String spec = "osgi:eager,web";
        String prefix = "configuration/subsystems";
        String inputname = "target/subsystems-osgi.xml";
        GenerateSubsystemsDefinition.main(new String[] { spec, "", prefix, inputname });

        File inputFile = new File(inputname);
        Assert.assertTrue("File exists: " + inputname, inputFile.exists());

        String resdir = "src/test/resources";
        String moddir = resdir + "/modules";
        String outputname = "target/subsystem-modules.txt";
        GenerateModulesDefinition.main(new String[] { inputname, "", resdir, moddir, null, outputname });

        File outfile = new File(outputname);
        Assert.assertTrue("File exists: " + outfile, outfile.exists());

        File xmlfile = new File("target/subsystem-modules.xml");
        Assert.assertTrue("File exists: " + xmlfile, xmlfile.exists());

        BufferedReader br = new BufferedReader(new FileReader(outfile));
        Assert.assertEquals("javax/servlet/api/main/**", br.readLine());
        Assert.assertEquals("org/apache/commons/logging/main/**", br.readLine());
        Assert.assertEquals("org/jboss/as/osgi/main/**", br.readLine());
        Assert.assertEquals("org/jboss/as/web/main/**", br.readLine());
        Assert.assertEquals("org/jboss/osgi/framework/main/**", br.readLine());
        Assert.assertEquals("org/osgi/core/main/**", br.readLine());
        Assert.assertEquals("org/slf4j/jcl-over-slf4j/main/**", br.readLine());
        Assert.assertEquals("org/slf4j/main/**", br.readLine());
        Assert.assertNull(br.readLine());
    }

    @Test
    public void testSkipSubsystems() throws Exception {

        String resdir = "src/test/resources";
        String moddir = resdir + "/modules";
        String outputname = "target/" + SKIP_SUBSYSTEMS + ".txt";
        GenerateModulesDefinition.main(new String[] { SKIP_SUBSYSTEMS, "", resdir, moddir, "org.jboss.osgi.framework", outputname });

        File outfile = new File(outputname);
        Assert.assertTrue("File exists: " + outfile, outfile.exists());

        BufferedReader br = new BufferedReader(new FileReader(outfile));
        Assert.assertEquals("org/jboss/osgi/framework/main/**", br.readLine());
        Assert.assertEquals("org/osgi/core/main/**", br.readLine());
        Assert.assertNull(br.readLine());
    }

    @Test
    public void testNoModuleDependencies() throws Exception {

        String resdir = "src/test/resources";
        String moddir = resdir + "/modules";
        String outputname = "target/no-modules.txt";
        GenerateModulesDefinition.main(new String[] { SKIP_SUBSYSTEMS, "", resdir, moddir, null, outputname });

        File outfile = new File(outputname);
        Assert.assertTrue("File exists: " + outfile, outfile.exists());

        BufferedReader br = new BufferedReader(new FileReader(outfile));
        Assert.assertEquals(NO_MODULE_DEPENENCIES, br.readLine());
        Assert.assertNull(br.readLine());
    }
}
