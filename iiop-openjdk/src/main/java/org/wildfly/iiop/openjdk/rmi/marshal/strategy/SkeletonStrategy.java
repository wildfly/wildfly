/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.iiop.openjdk.rmi.marshal.strategy;

import java.lang.reflect.Method;
import java.rmi.RemoteException;

import org.omg.CORBA.UserException;
import org.omg.CORBA.portable.IDLEntity;
import org.omg.CORBA.portable.UnknownException;
import org.omg.CORBA_2_3.portable.InputStream;
import org.omg.CORBA_2_3.portable.OutputStream;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;
import org.wildfly.iiop.openjdk.rmi.ExceptionAnalysis;
import org.wildfly.iiop.openjdk.rmi.RMIIIOPViolationException;
import org.wildfly.iiop.openjdk.rmi.marshal.CDRStream;
import org.wildfly.iiop.openjdk.rmi.marshal.CDRStreamReader;
import org.wildfly.iiop.openjdk.rmi.marshal.CDRStreamWriter;
import org.jboss.javax.rmi.RemoteObjectSubstitutionManager;

/**
 * A <code>SkeletonStrategy</code> for a given method knows how to
 * unmarshalthe sequence of method parameters from a CDR input stream, how to
 * marshal into a CDR output stream the return value of the method, and how to
 * marshal into a CDR output stream any exception thrown by the method.
 *
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @version $Revision: 81018 $
 */
public class SkeletonStrategy {
    /**
     * Each <code>CDRStreamReader</code> in the array unmarshals a method
     * parameter.
     */
    private final CDRStreamReader[] paramReaders;

    /**
     * A <code>Method</code> instance.
     */
    private final Method m;

    /**
     * Each <code>ExceptionWriter</code> in the array knows how to marshal
     * an exception that may be thrown be the method. The array is sorted so
     * that no writer for a derived exception appears after the base
     * exception writer.
     */
    private final ExceptionWriter[] excepWriters;

    /**
     * A <code>CDRStreamWriter</code> that marshals the return value of the
     * method.
     */
    private final CDRStreamWriter retvalWriter;

    // Public  -----------------------------------------------------------------

    /*
    * Constructs a <code>SkeletonStrategy</code> for a given method.
    */
    public SkeletonStrategy(final Method m) {
        // Keep the method
        this.m = m;

        // Initialize paramReaders
        Class[] paramTypes = m.getParameterTypes();
        int len = paramTypes.length;
        paramReaders = new CDRStreamReader[len];
        for (int i = 0; i < len; i++) {
            paramReaders[i] = CDRStream.readerFor(paramTypes[i]);
        }

        // Initialize excepWriters
        Class[] excepTypes = m.getExceptionTypes();
        len = excepTypes.length;
        int n = 0;
        for (int i = 0; i < len; i++) {
            if (!RemoteException.class.isAssignableFrom(excepTypes[i])) {
                n++;
            }
        }
        excepWriters = new ExceptionWriter[n];
        int j = 0;
        for (int i = 0; i < len; i++) {
            if (!RemoteException.class.isAssignableFrom(excepTypes[i])) {
                excepWriters[j++] = new ExceptionWriter(excepTypes[i]);
            }
        }
        ExceptionWriter.arraysort(excepWriters);

        // Initialize retvalWriter
        retvalWriter = CDRStream.writerFor(m.getReturnType());
    }

    /**
     * Unmarshals the sequence of method parameters from an input stream.
     *
     * @param in a CDR input stream
     * @return an object array with the parameters.
     */
    public Object[] readParams(InputStream in) {
        int len = paramReaders.length;
        Object[] params = new Object[len];
        for (int i = 0; i < len; i++) {
            params[i] = paramReaders[i].read(in);
        }
        return params;
    }

    /**
     * Returns this <code>SkeletonStrategy</code>'s method.
     */
    public Method getMethod() {
        return m;
    }

    /**
     * Returns true if this <code>SkeletonStrategy</code>'s method
     * is non void.
     */
    public boolean isNonVoid() {
        return (retvalWriter != null);
    }

    /**
     * Marshals into an output stream the return value of the method.
     *
     * @param out    a CDR output stream
     * @param retVal the value to be written.
     */
    public void writeRetval(OutputStream out, Object retVal) {
        retvalWriter.write(out, RemoteObjectSubstitutionManager.writeReplaceRemote(retVal));
    }

    /**
     * Marshals into an output stream an exception thrown by the method.
     *
     * @param out a CDR output stream
     * @param e   the exception to be written.
     */
    public void writeException(OutputStream out, Throwable e) {
        int len = excepWriters.length;
        for (int i = 0; i < len; i++) {
            if (excepWriters[i].getExceptionClass().isInstance(e)) {
                excepWriters[i].write(out, e);
                return;
            }
        }
        throw new UnknownException(e);
    }

    // Static inner class (private) --------------------------------------------

    /**
     * An <code>ExceptionWriter</code> knows how to write exceptions of a given
     * class to a CDR output stream.
     */
    private static class ExceptionWriter
            implements CDRStreamWriter {
        /**
         * The exception class.
         */
        private Class clz;

        /*
        * If the exception class corresponds to an IDL-defined exception, this
        * field contains the write method of the associated helper class.
        * A null value indicates that the exception class does not correspond
        * to an IDL-defined exception.
        */
        private java.lang.reflect.Method writeMethod = null;

        /**
         * The CORBA repository id of the exception class. (This field is used
         * if the exception class does not correspond to an IDL-defined
         * exception. An IDL-generated helper class provides the repository id
         * of an IDL-defined exception.)
         */
        private String reposId;

        /**
         * Constructs an <code>ExceptionWriter</code> for a given exception
         * class.
         */
        ExceptionWriter(Class clz) {
            this.clz = clz;
            if (IDLEntity.class.isAssignableFrom(clz)
                    && UserException.class.isAssignableFrom(clz)) {

                // This ExceptionWriter corresponds to an IDL-defined exception
                String helperClassName = clz.getName() + "Helper";
                try {
                    Class helperClass =
                            clz.getClassLoader().loadClass(helperClassName);
                    Class[] paramTypes =
                            {org.omg.CORBA.portable.OutputStream.class, clz};
                    writeMethod = helperClass.getMethod("write", paramTypes);
                } catch (ClassNotFoundException e) {
                    throw IIOPLogger.ROOT_LOGGER.errorLoadingClass(helperClassName, e);
                } catch (NoSuchMethodException e) {
                    throw IIOPLogger.ROOT_LOGGER.noWriteMethodInHelper(helperClassName, e);
                }

            } else {
                // This ExceptionWriter does not correspond to an IDL-defined
                // exception
                try {
                    this.reposId = ExceptionAnalysis.getExceptionAnalysis(clz)
                            .getExceptionRepositoryId();
                } catch (RMIIIOPViolationException e) {
                    throw IIOPLogger.ROOT_LOGGER.cannotObtainExceptionRepositoryID(clz.getName(),  e);
                }
            }
        }

        /**
         * Gets the exception <code>Class</code>.
         */
        Class getExceptionClass() {
            return clz;
        }

        /**
         * Writes an exception to a CDR output stream.
         */
        public void write(OutputStream out, Object excep) {
            if (writeMethod != null) {
                try {
                    writeMethod.invoke(null, new Object[]{out, excep});
                } catch (IllegalAccessException e) {
                    throw IIOPLogger.ROOT_LOGGER.unexpectedException(e);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    throw IIOPLogger.ROOT_LOGGER.errorMarshaling(IDLEntity.class, e.getTargetException());
                }
            } else {
                out.write_string(reposId);
                out.write_value((Exception) excep, clz);
            }
        }

        /**
         * Sorts an <code>ExceptionWriter</code> array so that no derived
         * exception strategy appears after a base exception strategy.
         */
        static void arraysort(ExceptionWriter[] a) {
            int len = a.length;

            for (int i = 0; i < len - 1; i++) {
                for (int j = i + 1; j < len; j++) {
                    if (a[i].clz.isAssignableFrom(a[j].clz)) {
                        ExceptionWriter tmp = a[i];

                        a[i] = a[j];
                        a[j] = tmp;
                    }
                }
            }
        }

    } // end of inner class ExceptionWriter

}
