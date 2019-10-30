/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
package org.jboss.as.testsuite.integration.secman.custompermissions;

import java.io.File;
import java.io.IOException;

import org.jboss.as.test.module.util.TestModule;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;

public class Utils {

    public static TestModule createTestModule(String jarName, String moduleName, String moduleXmlPath, Class<?>... classes) throws IOException {
        File moduleXml = new File(moduleXmlPath);
        TestModule module = new TestModule(moduleName, moduleXml);
        module.addResource(jarName).addClasses(classes);
        module.create();
        return module;
    }

    public static Asset getJBossDeploymentStructure(String... dependencies) {
        final StringBuilder sb = new StringBuilder();
        sb.append("<jboss-deployment-structure>\n<deployment>\n<dependencies>");
        if (dependencies != null) {
            for (String moduleName : dependencies) {
                sb.append("\n\t<module name='").append(moduleName).append("'/>");
            }
        }
        sb.append("\n</dependencies>\n</deployment>\n</jboss-deployment-structure>");
        return new StringAsset(sb.toString());
    }
}
