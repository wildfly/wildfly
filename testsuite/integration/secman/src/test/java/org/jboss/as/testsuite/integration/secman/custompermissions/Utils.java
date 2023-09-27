/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
