/*
   * JBoss, Home of Professional Open Source.
   * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jca.moduledeployment;


import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;

import org.xnio.IoUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * AS7-5768 -Support for RA module deployment
 *
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
public class ModuleDeploymentTestCaseSetup extends AbstractMgmtServerSetupTask{

       public void addModule(final String moduleName, final String mainResource) throws Exception {
                File testModuleRoot = new File(getModulePath(), moduleName);
                deleteRecursively(testModuleRoot);
                createTestModule(testModuleRoot, mainResource);
            }

            public void removeModule(final String moduleName) throws Exception {
                File testModuleRoot = new File(getModulePath(), moduleName);
                deleteRecursively(testModuleRoot);
            }

            private void deleteRecursively(File file) {
                if (file.exists()) {
                    if (file.isDirectory()) {
                        for (String name : file.list()) {
                            deleteRecursively(new File(file, name));
                        }
                    }
                    file.delete();
                }
            }

            private void createTestModule(File testModuleRoot, final String mainResource) throws IOException {
                if (testModuleRoot.exists()) {
                    throw new IllegalArgumentException(testModuleRoot + " already exists");
                }
                File file = new File(testModuleRoot, "main");
                if (!file.mkdirs()) {
                    throw new IllegalArgumentException("Could not create " + file);
                }


                URL url = this.getClass().getResource("module.xml");
                if (url == null) {
                    throw new IllegalStateException("Could not find module.xml");
                }
                copyFile(new File(file, "module.xml"), url.openStream());

                url = this.getClass().getResource(mainResource);
                if (url == null) {
                    throw new IllegalStateException("Could not find " + mainResource);
                }
                copyFile(new File(file, mainResource), url.openStream());

            }

            private void copyFile(File target, InputStream src) throws IOException {
                final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target));
                try {
                    int i = src.read();
                    while (i != -1) {
                        out.write(i);
                        i = src.read();
                    }
                } finally {
                    IoUtils.safeClose(out);
                }
            }

            private File getModulePath() {
                String modulePath = System.getProperty("module.path", null);
                if (modulePath == null) {
                    String jbossHome = System.getProperty("jboss.home", null);
                    if (jbossHome == null) {
                        throw new IllegalStateException("Neither -Dmodule.path nor -Djboss.home were set");
                    }
                    modulePath = jbossHome + File.separatorChar + "modules";
                } else {
                    modulePath = modulePath.split(File.pathSeparator)[0];
                }
                File moduleDir = new File(modulePath);
                if (!moduleDir.exists()) {
                    throw new IllegalStateException("Determined module path does not exist");
                }
                if (!moduleDir.isDirectory()) {
                    throw new IllegalStateException("Determined module path is not a dir");
                }
                return moduleDir;
            }

			@Override
			public void tearDown(ManagementClient managementClient,
					String containerId) throws Exception {
				// TODO Auto-generated method stub
				
			}

			@Override
			protected void doSetup(ManagementClient managementClient)
					throws Exception {
				// TODO Auto-generated method stub
				
			}
    


}
