/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.jmx;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.jboss.arquillian.spi.TestResult;
import org.jboss.arquillian.spi.TestRunner;
import org.jboss.arquillian.spi.TestResult.Status;
import org.jboss.arquillian.spi.util.TestRunners;
import org.jboss.logging.Logger;

/**
 * An MBean to run test methods in container.
 *
 * @author thomas.diesler@jboss.com
 * @since 06-Sep-2010
 */
public abstract class JMXTestRunner implements JMXTestRunnerMBean {
    // Provide logging
    private static Logger log = Logger.getLogger(JMXTestRunner.class);

    public interface TestClassLoader {
        Class<?> loadTestClass(String className) throws ClassNotFoundException;

        ClassLoader getServiceClassLoader();
    }

    public ObjectName registerMBean(MBeanServer mbeanServer) throws JMException {
        ObjectName objectName = getObjectName();
        StandardMBean mbean = new StandardMBean(this, JMXTestRunnerMBean.class);
        mbeanServer.registerMBean(mbean, objectName);
        log.debug("JMXTestRunner registered: " + objectName);
        return objectName;
    }

    public void unregisterMBean(MBeanServer mbeanServer) throws JMException {
        ObjectName oname = getObjectName();
        if (mbeanServer.isRegistered(oname)) {
            mbeanServer.unregisterMBean(oname);
            log.debug("JMXTestRunner unregistered: " + oname);
        }
    }

    protected abstract ObjectName getObjectName();

    protected TestClassLoader getTestClassLoader() {
        return new TestClassLoader() {
            @Override
            public Class<?> loadTestClass(String className) throws ClassNotFoundException {
                return getClass().getClassLoader().loadClass(className);
            }

            @Override
            public ClassLoader getServiceClassLoader() {
                return getClass().getClassLoader();
            }
        };
    }

    public TestResult runTestMethod(String className, String methodName, Map<String, String> props) {
        return runTestMethodInternal(className, methodName, props);
    }

    public InputStream runTestMethodEmbedded(String className, String methodName, Map<String, String> props) {
        TestResult result = runTestMethodInternal(className, methodName, props);

        // Marshall the TestResult
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(result);
            oos.close();

            return new ByteArrayInputStream(baos.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot marshall response", ex);
        }
    }

    private TestResult runTestMethodInternal(String className, String methodName, Map<String, String> props) {
        try {
            // Get the TestRunner
            Class<?> testClass = getTestClassLoader().loadTestClass(className);
            ClassLoader testCl = SecurityActions.getClassLoader(testClass);
            ClassLoader serviceClassLoader = getTestClassLoader().getServiceClassLoader();
            TestRunner runner = TestRunners.getTestRunner(serviceClassLoader);

            ClassLoader tccl = SecurityActions.getThreadContextClassLoader();
            try {
                //Use the test cl as the classloader
                SecurityActions.setThreadContextClassLoader(testCl);
                TestResult testResult = runner.execute(testClass, methodName);
                return testResult;
            } finally {
                SecurityActions.setThreadContextClassLoader(tccl);
            }
        } catch (Throwable th) {
            return new TestResult(Status.FAILED, th);
        }
    }
}
