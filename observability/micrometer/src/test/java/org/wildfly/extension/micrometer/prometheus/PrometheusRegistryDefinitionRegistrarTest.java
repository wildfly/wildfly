/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer.prometheus;

import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

import org.junit.Test;

/** Verifies that Micrometer subsystem registration does not eagerly require the optional Prometheus module. */
public class PrometheusRegistryDefinitionRegistrarTest {

    private static final String REGISTRAR_CLASS_NAME =
            "org.wildfly.extension.micrometer.prometheus.PrometheusRegistryDefinitionRegistrar";

    /** Loads the registrar while hiding Prometheus classes to model a Micrometer-only installation. */
    @Test
    public void loadsWithoutPrometheusModule() throws Exception {
        URL[] productionClasses = {Path.of("target/classes").toAbsolutePath().toUri().toURL()};
        ClassLoader parent = new ClassLoader(getClass().getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("io.prometheus.")) {
                    throw new ClassNotFoundException(name);
                }
                return super.loadClass(name, resolve);
            }
        };
        try (URLClassLoader classLoader = new URLClassLoader(productionClasses, parent) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (REGISTRAR_CLASS_NAME.equals(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) {
                        loaded = findClass(name);
                    }
                    if (resolve) {
                        resolveClass(loaded);
                    }
                    return loaded;
                }
                return super.loadClass(name, resolve);
            }
        }) {
            assertNotNull(Class.forName(REGISTRAR_CLASS_NAME, true, classLoader));
        }
    }
}
