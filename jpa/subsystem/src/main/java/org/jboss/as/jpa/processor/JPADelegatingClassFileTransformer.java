/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.processor;

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

import java.nio.ByteBuffer;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

import org.jboss.as.jpa.messages.JpaLogger;
import org.jboss.modules.ClassTransformer;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.security.manager.action.GetAccessControlContextAction;


/**
 * Helps implement PersistenceUnitInfo.addClassTransformer() by using DelegatingClassTransformer
 *
 * @author Scott Marlow
 */
class JPADelegatingClassFileTransformer implements ClassTransformer {
    private final PersistenceUnitMetadata persistenceUnitMetadata;

    public JPADelegatingClassFileTransformer(PersistenceUnitMetadata pu) {
        persistenceUnitMetadata = pu;
    }

    @Override
    public ByteBuffer transform(ClassLoader classLoader, String className, ProtectionDomain protectionDomain, ByteBuffer classBytes)
            throws IllegalArgumentException {
        final AccessControlContext accessControlContext =
                AccessController.doPrivileged(GetAccessControlContextAction.getInstance());
        PrivilegedAction<ByteBuffer> privilegedAction =
                new PrivilegedAction<ByteBuffer>() {
                    // run as security privileged action
                    @Override
                    public ByteBuffer run() {

                        byte[] transformedBuffer = getBytes(classBytes);
                        boolean transformed = false;
                        for (jakarta.persistence.spi.ClassTransformer transformer : persistenceUnitMetadata.getTransformers()) {
                            if (ROOT_LOGGER.isTraceEnabled())
                                ROOT_LOGGER.tracef("rewrite entity class '%s' using transformer '%s' for '%s'", className,
                                        transformer.getClass().getName(),
                                        persistenceUnitMetadata.getScopedPersistenceUnitName());
                            byte[] result;
                            try {
                                // parameter classBeingRedefined is always passed as null
                                // because we won't ever be called to transform already loaded classes.
                                result = transformer.transform(classLoader, className, null, protectionDomain, transformedBuffer);
                            } catch (Exception e) {
                                String message = JpaLogger.ROOT_LOGGER.invalidClassFormat(className);
                                // ModuleClassLoader.defineClass discards the cause of the exception we throw, so to ensure it is logged we log it here.
                                ROOT_LOGGER.error(message,e);
                                throw new IllegalStateException(message);
                            }
                            if (result != null) {
                                transformedBuffer = result;
                                transformed = true;
                                if (ROOT_LOGGER.isTraceEnabled())
                                    ROOT_LOGGER.tracef("entity class '%s' was rewritten", className);
                            }
                        }
                        return transformed ? ByteBuffer.wrap(transformedBuffer) : null;
                    }
                };
        return WildFlySecurityManager.doChecked(privilegedAction, accessControlContext);
    }

    private byte[] getBytes(ByteBuffer classBytes) {

        if (classBytes == null) {
            return null;
        }

        final int position = classBytes.position();
        final int limit = classBytes.limit();
        final byte[] bytes;
        if (classBytes.hasArray() && classBytes.arrayOffset() == 0 && position == 0 && limit == classBytes.capacity()) {
            bytes = classBytes.array();
        } else {
            bytes = new byte[limit - position];
            classBytes.get(bytes);
            classBytes.position(position);
        }
        return bytes;
    }
}
