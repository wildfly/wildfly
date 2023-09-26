/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.context.classloader.app;

public interface MBeanAppClassLoaderTCCLCheckServiceMBean {
    // Service lifecycle methods
    void create() throws Exception;
    void start() throws Exception;
    void stop();
    void destroy();
    // Attribute
    void setFile(String path) throws Exception;
    String getFile();
    // A method
    void method();
}
