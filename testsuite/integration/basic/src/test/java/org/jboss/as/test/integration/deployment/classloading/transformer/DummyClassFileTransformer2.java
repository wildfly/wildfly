/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.classloading.transformer;

import org.jboss.modules.ClassTransformer;
import java.nio.ByteBuffer;
import java.security.ProtectionDomain;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author Marius Bogoevici
 */
public class DummyClassFileTransformer2 implements ClassTransformer {

    public static boolean wasActive = false;

    public static Set<String> transformedClassNames = new ConcurrentSkipListSet<String> ();

    @Override
    public ByteBuffer transform(ClassLoader loader, String className, ProtectionDomain protectionDomain, ByteBuffer classfileBuffer) throws IllegalArgumentException {
        transformedClassNames.add(className);
        wasActive = true;
        return classfileBuffer;
    }
}
