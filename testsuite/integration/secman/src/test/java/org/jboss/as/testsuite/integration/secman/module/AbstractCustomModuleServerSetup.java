/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.testsuite.integration.secman.module;

import java.io.File;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CustomCLIExecutor;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * Server setup task base, which adds a custom module to the AS configuration. Child classes have to implement
 * {@link #getModuleSuffix()} method, which is used to generate module name and to determine which descriptor file will be used
 * as the final module.xml.
 *
 * @author Josef Cacek
 */
public abstract class AbstractCustomModuleServerSetup implements ServerSetupTask {

    private static Logger LOGGER = Logger.getLogger(AbstractCustomModuleServerSetup.class);

    public static final String MODULE_NAME_BASE = "org.jboss.test.secman";

    private static final String MODULE_JAR = "modperm.jar";
    private static final File WORK_DIR = new File("cust-module-test");
    private static final File MODULE_JAR_FILE = new File(WORK_DIR, MODULE_JAR);

    /**
     * Creates work-directory where JAR containing {@link CheckJSMUtils} is stored. The JAR is then deployed as an AS module.
     */
    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        LOGGER.trace("(Re)Creating workdir: " + WORK_DIR.getAbsolutePath());
        FileUtils.deleteDirectory(WORK_DIR);
        removeModule(getModuleSuffix());
        WORK_DIR.mkdirs();

        ShrinkWrap.create(JavaArchive.class, MODULE_JAR).addClass(CheckJSMUtils.class)
                .as(org.jboss.shrinkwrap.api.exporter.ZipExporter.class).exportTo(MODULE_JAR_FILE, true);
        addModule(getModuleSuffix());
    }

    /**
     * Removes work-directory and removes the AS module.
     */
    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        LOGGER.trace("Removing custom module");
        FileUtils.deleteDirectory(WORK_DIR);
        removeModule(getModuleSuffix());
    }

    /**
     * Returns last part of module name. The name base is {@value #MODULE_NAME_BASE}
     */
    protected abstract String getModuleSuffix();

    private void addModule(String suffix) throws URISyntaxException {
        File tmpFile = new File(getClass().getResource("module-" + suffix + ".xml").toURI());
        CustomCLIExecutor.execute(null, "module add --name=" + MODULE_NAME_BASE + "." + suffix + " --resources="
                + escapePath(MODULE_JAR_FILE.getAbsolutePath()) + " --module-xml=" + escapePath(tmpFile.getAbsolutePath()));
    }

    private void removeModule(String suffix) throws URISyntaxException {
        CustomCLIExecutor.execute(null, "module remove --name=" + MODULE_NAME_BASE + "." + suffix);
    }

    private String escapePath(final String str) {
        return str == null ? null : str.replaceAll("([\\s])", "\\\\$1");
    }
}