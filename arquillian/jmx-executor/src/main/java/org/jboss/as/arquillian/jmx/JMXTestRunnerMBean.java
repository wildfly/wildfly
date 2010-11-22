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
import java.util.Map;

import javax.management.ObjectName;

import org.jboss.arquillian.spi.TestResult;

/**
 * An MBean to run test methods in container.
 *
 * @author thomas.diesler@jboss.com
 * @version $Revision: $
 */
public interface JMXTestRunnerMBean {
    /**
     * The default ObjectName for this service:
     * jboss.arquillian:service=jmx-test-runner
     */
    ObjectName OBJECT_NAME = ObjectNameFactory.create("jboss.arquillian:service=jmx-test-runner");

    /**
     * Runs a test method on the given test class
     *
     * @param className
     *            the test class name
     * @param methodName
     *            the test method name
     * @param props
     *            TODO
     * @return the input stream to read the {@link TestResult} from
     */
    InputStream runTestMethodEmbedded(String className, String methodName, Map<String, String> props);

    /**
     * Runs a test method on the given test class
     *
     * @param className
     *            the test class name
     * @param methodName
     *            the test method name
     * @return the {@link TestResult}
     */
    TestResult runTestMethod(String className, String methodName, Map<String, String> props);
}
