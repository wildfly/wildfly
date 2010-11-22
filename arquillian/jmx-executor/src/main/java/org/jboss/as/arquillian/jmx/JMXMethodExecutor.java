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

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

import org.jboss.arquillian.spi.ContainerMethodExecutor;
import org.jboss.arquillian.spi.TestMethodExecutor;
import org.jboss.arquillian.spi.TestResult;
import org.jboss.arquillian.spi.TestResult.Status;

/**
 * JMXMethodExecutor
 *
 * @author thomas.diesler@jboss.com
 * @since 06-Sep-2010
 */
public class JMXMethodExecutor implements ContainerMethodExecutor {
    private final MBeanServerConnection mbeanServer;
    private final ExecutionType executionType;
    private final ObjectName testRunnerName;
    private final Map<String, String> props;

    public enum ExecutionType {
        EMBEDDED, REMOTE
    }

    public JMXMethodExecutor(MBeanServerConnection connection, ExecutionType executionType, ObjectName testRunnerName) {
        this.mbeanServer = connection;
        this.executionType = executionType;
        this.testRunnerName = testRunnerName;
        this.props = new HashMap<String, String>();
        props.put(ExecutionType.class.getName(), executionType.toString());
    }

    public TestResult invoke(TestMethodExecutor testMethodExecutor) {
        if (testMethodExecutor == null)
            throw new IllegalArgumentException("TestMethodExecutor null");

        Object testInstance = testMethodExecutor.getInstance();
        String testClass = testInstance.getClass().getName();
        String testMethod = testMethodExecutor.getMethod().getName();

        TestResult result = null;
        try {
            JMXTestRunnerMBean testRunner = getMBeanProxy(testRunnerName, JMXTestRunnerMBean.class);

            if (executionType == ExecutionType.EMBEDDED) {
                InputStream resultStream = testRunner.runTestMethodEmbedded(testClass, testMethod, props);
                ObjectInputStream ois = new ObjectInputStream(resultStream);
                result = (TestResult) ois.readObject();
            } else if (executionType == ExecutionType.REMOTE) {
                result = testRunner.runTestMethod(testClass, testMethod, props);
            }
        } catch (final Throwable e) {
            result = new TestResult(Status.FAILED);
            result.setThrowable(e);
        } finally {
            result.setEnd(System.currentTimeMillis());
        }
        return result;
    }

    private <T> T getMBeanProxy(ObjectName name, Class<T> interf) {
        return (T) MBeanServerInvocationHandler.newProxyInstance(mbeanServer, name, interf, false);
    }
}