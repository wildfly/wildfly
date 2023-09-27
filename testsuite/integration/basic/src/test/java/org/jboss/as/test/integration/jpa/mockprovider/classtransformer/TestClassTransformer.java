/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.mockprovider.classtransformer;

import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import jakarta.persistence.spi.ClassTransformer;

/**
 * TestClassTransformer
 *
 * @author Scott Marlow
 */
public class TestClassTransformer implements ClassTransformer {

    // track class names that would of been transformed of this class was capable of doing so.
    private static final List<String> transformedClasses = Collections.synchronizedList(new ArrayList<String>());

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        transformedClasses.add(className);
        return classfileBuffer;
    }

    public static Collection getTransformedClasses() {
        return transformedClasses;
    }

    public static void clearTransformedClasses() {
        transformedClasses.clear();
    }
}
