/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.wildfly.iiop.openjdk.logging.IIOPLogger;


/**
 * Operation analysis.
 * <p/>
 * Routines here are conforming to the "Java(TM) Language to IDL Mapping
 * Specification", version 1.1 (01-06-07).
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 * @version $Revision: 81018 $
 */
public class OperationAnalysis  extends AbstractAnalysis {

    /**
     * The Method that this OperationAnalysis is mapping.
     */
    private final Method method;

    /**
     * The mapped exceptions of this operation.
     */
    private final ExceptionAnalysis[] mappedExceptions;

    /**
     * The parameters of this operation.
     */
    private final ParameterAnalysis[] parameters;

    OperationAnalysis(final Method method)  throws RMIIIOPViolationException {
        super(method.getName());
        this.method = method;

        // Check if valid return type, IF it is a remote interface.
        Class retCls = method.getReturnType();
        if (retCls.isInterface() && Remote.class.isAssignableFrom(retCls))
            Util.isValidRMIIIOP(retCls);

        // Analyze exceptions
        Class[] ex = method.getExceptionTypes();
        boolean gotRemoteException = false;
        ArrayList a = new ArrayList();
        for (int i = 0; i < ex.length; ++i) {
            if (ex[i].isAssignableFrom(RemoteException.class))
                gotRemoteException = true;
            if (Exception.class.isAssignableFrom(ex[i]) &&
                    !RuntimeException.class.isAssignableFrom(ex[i]) &&
                    !RemoteException.class.isAssignableFrom(ex[i]))
                a.add(ExceptionAnalysis.getExceptionAnalysis(ex[i])); // map this
        }
        if (!gotRemoteException && Remote.class.isAssignableFrom(method.getDeclaringClass()))
            throw IIOPLogger.ROOT_LOGGER.badRMIIIOPMethodSignature(getJavaName(), method.getDeclaringClass().getName(),
                    "1.2.3");
        final ExceptionAnalysis[] exceptions = new ExceptionAnalysis[a.size()];
        mappedExceptions = (ExceptionAnalysis[]) a.toArray(exceptions);

        // Analyze parameters
        Class[] params = method.getParameterTypes();
        parameters = new ParameterAnalysis[params.length];
        for (int i = 0; i < params.length; ++i) {
            parameters[i] = new ParameterAnalysis("param" + (i + 1), params[i]);
        }
    }

    /**
     * Return my Java return type.
     */
    public Class getReturnType() {
        return method.getReturnType();
    }

    /**
     * Return my mapped Method.
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Return my mapped exceptions.
     */
    public ExceptionAnalysis[] getMappedExceptions() {
        return (ExceptionAnalysis[]) mappedExceptions.clone();
    }

    /**
     * Return my parameters.
     */
    public ParameterAnalysis[] getParameters() {
        return (ParameterAnalysis[]) parameters.clone();
    }
}
