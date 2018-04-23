/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.util;

import java.lang.reflect.Modifier;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

/**
 * @author Romain Pelisse - romain@redhat.com
 */
public final class MdbValidationsUtil {

    private MdbValidationsUtil() {

    }

    /**
     * Returns true if the passed <code>mdbClass</code> meets the requirements set by the EJB3 spec about bean implementation
     * classes. The passed <code>mdbClass</code> must not be an interface and must be public and not final and not abstract. If
     * it passes these requirements then this method returns true. Else it returns false.
     *
     * @param mdbClass The MDB class
     * @return
     * @throws DeploymentUnitProcessingException
     */
    public static void assertMDBClassValidity(final ClassInfo mdbClass) throws DeploymentUnitProcessingException {
        final String className = mdbClass.name().toString();
        short flags = mdbClass.flags();
        // must *not* be an interface
        if (Modifier.isInterface(flags)) {
            throw EjbLogger.DEPLOYMENT_LOGGER.mdbClassCannotBeAnInterface(className);
        }
        // bean class must be public, must *not* be abstract or final
        if (!Modifier.isPublic(flags) || Modifier.isAbstract(flags) || Modifier.isFinal(flags)) {
            throw EjbLogger.DEPLOYMENT_LOGGER.mdbClassMustBePublicNonAbstractNonFinal(className);
        }
        // bean class can not have onMessage method as final or static or private, neither can the Mdb has a finalize() method
        for (MethodInfo method : mdbClass.methods()) {
            if ("onMessage".equals(method.name())) {
                short methodsFlags = method.flags();
                if (Modifier.isFinal(methodsFlags)) {
                    throw EjbLogger.DEPLOYMENT_LOGGER.mdbOnMessageMethodCantBeFinal(className);
                }
                if (Modifier.isStatic(methodsFlags)) {
                    throw EjbLogger.DEPLOYMENT_LOGGER.mdbOnMessageMethodCantBeStatic(className);
                }
                if (Modifier.isPrivate(methodsFlags)) {
                    throw EjbLogger.DEPLOYMENT_LOGGER.mdbOnMessageMethodCantBePrivate(className);
                }
            }

            if ("finalize".equals(method.name())) {
                throw EjbLogger.DEPLOYMENT_LOGGER.mdbOnMessageMethodCantBePrivate(className);
            }
        }
    }
}
