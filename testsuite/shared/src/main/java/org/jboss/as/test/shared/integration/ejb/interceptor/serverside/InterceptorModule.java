/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.shared.integration.ejb.interceptor.serverside;

import java.net.URL;
import org.jboss.as.test.module.util.TestModule;

/**
 * A simple POJO representing a structure needed for setting up a global interceptor.
 *
 * @author <a href="mailto:szhantem@redhat.com">Sultan Zhantemirov</a> (c) 2019 Red Hat, inc.
 */
public class InterceptorModule {
    private Class interceptorClass;
    private String moduleName;
    private String moduleXmlName;
    private URL moduleXmlPath;
    private String jarName;
    private TestModule testModule;

    /**
     * @param interceptorClass - class with interceptor implementation.
     * @param moduleName - name of interceptor module.
     * @param moduleXmlName - module XML filename, e.g. module.xml
     * @param moduleXmlPath - module XML URL
     * @param jarName - name of interceptor target JAR archive.
     */
    public InterceptorModule(Class interceptorClass, String moduleName, String moduleXmlName, URL moduleXmlPath, String jarName) {
        this.interceptorClass = interceptorClass;
        this.moduleName = moduleName;
        this.moduleXmlName = moduleXmlName;
        this.moduleXmlPath = moduleXmlPath;
        this.jarName = jarName;
    }

    public Class getInterceptorClass() {
        return interceptorClass;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getModuleXmlName() {
        return moduleXmlName;
    }

    public URL getModuleXmlPath() {
        return moduleXmlPath;
    }

    public String getJarName() {
        return jarName;
    }

    public TestModule getTestModule() {
        return testModule;
    }

    public void setTestModule(TestModule testModule) {
        this.testModule = testModule;
    }
}
