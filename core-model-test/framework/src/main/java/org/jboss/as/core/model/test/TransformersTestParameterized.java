/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.core.model.test;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class TransformersTestParameterized extends Suite {
        /**
         * Annotation for a method which provides parameters to be injected into the
         * test class constructor by <code>Parameterized</code>
         */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public static @interface TransformersParameter {
            /**
             * <p>
             * Optional pattern to derive the test's name from the parameters. Use
             * numbers in braces to refer to the parameters or the additional data
             * as follows:
             * </p>
             *
             * <pre>
             * {index} - the current parameter index
             * {0} - the first parameter value
             * {1} - the second parameter value
             * etc...
             * </pre>
             * <p>
             * Default value is "{index}" for compatibility with previous JUnit
             * versions.
             * </p>
             *
             * @return {@link MessageFormat} pattern string, except the index
             *         placeholder.
             * @see MessageFormat
             */
            String name() default "{index}";
        }


        private class TestClassRunnerForParameters extends BlockJUnit4ClassRunner {
            private final ClassloaderParameter fParameters;

            private final String fName;

            TestClassRunnerForParameters(Class<?> type, ClassloaderParameter parameters,
                    String name) throws InitializationError {
                super(type);
                fParameters = parameters;
                fName = name;
            }

            @Override
            public Object createTest() throws Exception {
                Object o =  createTestUsingConstructorInjection();
                if (o instanceof AbstractCoreModelTest) {
                    CoreModelTestDelegate delegate = ((AbstractCoreModelTest)o).getDelegate();
                    delegate.setCurrentTransformerClassloaderParameter(fParameters);
                }
                return o;
            }

            private Object createTestUsingConstructorInjection() throws Exception {
                return getTestClass().getOnlyConstructor().newInstance(fParameters);
            }


            @Override
            protected String getName() {
                return fName;
            }

            @Override
            protected String testName(FrameworkMethod method) {
                return method.getName() + getName();
            }

            @Override
            protected void validateConstructor(List<Throwable> errors) {
                validateOnlyOneConstructor(errors);
            }

            @Override
            protected void validateFields(List<Throwable> errors) {
                super.validateFields(errors);
            }

            @Override
            protected Statement classBlock(RunNotifier notifier) {
                return childrenInvoker(notifier);
            }

            @Override
            protected Annotation[] getRunnerAnnotations() {
                return new Annotation[0];
            }
        }

        private static final List<Runner> NO_RUNNERS = Collections
                .<Runner>emptyList();

        private final ArrayList<Runner> runners = new ArrayList<Runner>();

        /**
         * Only called reflectively. Do not use programmatically.
         */
        public TransformersTestParameterized(Class<?> klass) throws Throwable {
            super(klass, NO_RUNNERS);
            TransformersParameter parameters = getParametersMethod().getAnnotation(
                    TransformersParameter.class);
            createRunnersForParameters(allParameters(), parameters.name());
        }

        @Override
        protected List<Runner> getChildren() {
            return runners;
        }

        @SuppressWarnings("unchecked")
        private Iterable<ClassloaderParameter> allParameters() throws Throwable {
            Object parameters = getParametersMethod().invokeExplosively(null);
            if (parameters instanceof Iterable) {
                return (Iterable<ClassloaderParameter>) parameters;
            } else {
                throw parametersMethodReturnedWrongType();
            }
        }

        private FrameworkMethod getParametersMethod() throws Exception {
            List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(
                    TransformersParameter.class);
            for (FrameworkMethod each : methods) {
                if (each.isStatic() && each.isPublic()) {
                    return each;
                }
            }

            throw new Exception("No public static parameters method on class "
                    + getTestClass().getName());
        }

        private void createRunnersForParameters(Iterable<ClassloaderParameter> allParameters,
                String namePattern) throws Exception {
            try {
                int i = 0;
                for (ClassloaderParameter parametersOfSingleTest : allParameters) {
                    String name = nameFor(namePattern, i, parametersOfSingleTest);
                    TestClassRunnerForParameters runner = new TestClassRunnerForParameters(
                            getTestClass().getJavaClass(), parametersOfSingleTest,
                            name);
                    runners.add(runner);
                    ++i;
                }
            } catch (ClassCastException e) {
                throw parametersMethodReturnedWrongType();
            }
        }

        private String nameFor(String namePattern, int index, ClassloaderParameter parameters) {
            String finalPattern = namePattern.replaceAll("\\{index\\}",
                    Integer.toString(index));
            String name = MessageFormat.format(finalPattern, parameters);
            return "[" + name + "]";
        }

        private Exception parametersMethodReturnedWrongType() throws Exception {
            String className = getTestClass().getName();
            String methodName = getParametersMethod().getName();
            String message = MessageFormat.format(
                    "{0}.{1}() must return an Iterable of arrays.",
                    className, methodName);
            return new Exception(message);
        }
    }