/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.integration.interceptor.clientside;

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
