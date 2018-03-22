package org.jboss.as.ejb3.util;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

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
    public static boolean assertMDBClassValidity(final ClassInfo mdbClass) throws DeploymentUnitProcessingException {
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
        // bean class can not have final method
        List<String> finalMethods = new ArrayList<>(0);
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
        if (!finalMethods.isEmpty()) {
            return false;
        }
        return true;
    }
}
