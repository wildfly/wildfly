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
package org.wildfly.iiop.openjdk.rmi;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Iterator;

/**
 * This class contains a bunch of methods taken from
 * org.jboss.verifier.strategy.AbstractVerifier. There they are declared
 * as instance methods. I copied them to this class and made them static here
 * in order to call them as utility methods, without having to create a
 * verifier instance,
 *
 * @author <a href="mailto:juha.lindfors@jboss.org">Juha Lindfors</a>
 * @author Aaron Mulder  (ammulder@alumni.princeton.edu)
 * @author Vinay Menon   (menonv@cpw.co.uk)
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @version $Revision: 81018 $
 */
public class RmiIdlUtil {

    public static boolean hasLegalRMIIIOPArguments(Method method) {

        Class[] params = method.getParameterTypes();

        for (int i = 0; i < params.length; ++i)
            if (!isRMIIIOPType(params[i]))
                return false;

        return true;
    }

    public static boolean hasLegalRMIIIOPReturnType(Method method) {
        return isRMIIIOPType(method.getReturnType());
    }

    public static boolean hasLegalRMIIIOPExceptionTypes(Method method) {

        /*
        * All checked exception classes used in method declarations
        * (other than java.rmi.RemoteException) MUST be conforming
        * RMI/IDL exception types.
        *
        * Spec 28.2.3 (4)
        */
        Iterator it = Arrays.asList(method.getExceptionTypes()).iterator();

        while (it.hasNext()) {
            Class exception = (Class) it.next();

            if (!isRMIIDLExceptionType(exception))
                return false;
        }

        return true;
    }

    /*
     * checks if the method's throws clause includes java.rmi.RemoteException
    * or a superclass of it.
    */
    public static boolean throwsRemoteException(Method method) {

        Class[] exception = method.getExceptionTypes();

        for (int i = 0; i < exception.length; ++i)
            if (exception[i].isAssignableFrom(java.rmi.RemoteException.class))
                return true;

        return false;
    }

    /*
    * checks if a class's member (method, constructor or field) has a 'static'
    * modifier.
    */
    public static boolean isStatic(Member member) {
        return (Modifier.isStatic(member.getModifiers()));
    }

    /*
    * checks if the given class is declared as static (inner classes only)
    */
    public static boolean isStatic(Class c) {
        return (Modifier.isStatic(c.getModifiers()));
    }

    /*
    * checks if a class's member (method, constructor or field) has a 'final'
    * modifier.
    */
    public static boolean isFinal(Member member) {
        return (Modifier.isFinal(member.getModifiers()));
    }

    /*
    * checks if the given class is declared as final
    */
    public static boolean isFinal(Class c) {
        return (Modifier.isFinal(c.getModifiers()));
    }

    /*
    * checks if a class's member (method, constructor or field) has a 'public'
    * modifier.
    */
    public static boolean isPublic(Member member) {
        return (Modifier.isPublic(member.getModifiers()));
    }

    /*
    * checks if the given class is declared as public
    */
    public static boolean isPublic(Class c) {
        return (Modifier.isPublic(c.getModifiers()));
    }

    /**
     * Checks whether all the fields in the class are declared as public.
     */
    public static boolean isAllFieldsPublic(Class c) {
        try {
            final Field[] list = c.getFields();
            for (int i = 0; i < list.length; i++)
                if (!Modifier.isPublic(list[i].getModifiers()))
                    return false;
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /*
    * checks if the given class is declared as abstract
    */
    public static boolean isAbstract(Class c) {
        return (Modifier.isAbstract(c.getModifiers()));
    }

    public static boolean isRMIIIOPType(Class type) {

        /*
        *  Java Language to IDL Mapping
        *  ftp://ftp.omg.org/pub/docs/ptc/99-03-09.pdf
        *
        *  A conforming RMI/IDL type is a Java type whose values
        *  may be transmitted across an RMI/IDL remote interface at
        *  run-time. A Java data type is a conforming RMI/IDL type
        *  if it is:
        *
        *  - one of the Java primitive types (see Primitive Types on page 28-2).
        *  - a conforming remote interface (as defined in RMI/IDL Remote Interfaces on page 28-2).
        *  - a conforming value type (as defined in RMI/IDL Value Types on page 28-4).
        *  - an array of conforming RMI/IDL types (see RMI/IDL Arrays on page 28-5).
        *  - a conforming exception type (see RMI/IDL Exception Types on page 28-5).
        *  - a conforming CORBA object reference type (see CORBA Object Reference Types on page 28-6).
        *  - a conforming IDL entity type see IDL Entity Types on page 28-6).
        */

        /*
        * Primitive types.
        *
        * Spec 28.2.2
        */
        if (type.isPrimitive())
            return true;

        /*
        * Conforming array.
        *
        * Spec 28.2.5
        */
        if (type.isArray())
            return isRMIIIOPType(type.getComponentType());

        /*
        * Conforming CORBA reference type
        *
        * Spec 28.2.7
        */
        if (org.omg.CORBA.Object.class.isAssignableFrom(type))
            return true;

        /*
        * Conforming IDL Entity type
        *
        * Spec 28.2.8
        */
        if (org.omg.CORBA.portable.IDLEntity.class.isAssignableFrom(type))
            return true;

        /*
        * Conforming remote interface.
        *
        * Spec 28.2.3
        */
        if (isRMIIDLRemoteInterface(type))
            return true;

        /*
        * Conforming exception.
        *
        * Spec 28.2.6
        */
        if (isRMIIDLExceptionType(type))
            return true;

        /*
        * Conforming value type.
        *
        * Spec 28.2.4
        */
        if (isRMIIDLValueType(type))
            return true;

        return false;
    }

    public static boolean isRMIIDLRemoteInterface(Class type) {

        /*
        * If does not implement java.rmi.Remote, cannot be valid RMI-IDL
        * remote interface.
        */
        if (!java.rmi.Remote.class.isAssignableFrom(type))
            return false;

        Iterator methodIterator = Arrays.asList(type.getMethods()).iterator();

        while (methodIterator.hasNext()) {
            Method m = (Method) methodIterator.next();

            /*
            * All methods in the interface MUST throw
            * java.rmi.RemoteException or a superclass of it.
            *
            * Spec 28.2.3 (2)
            */
            if (!throwsRemoteException(m)) {
                return false;
            }

            /*
            * All checked exception classes used in method declarations
            * (other than java.rmi.RemoteException) MUST be conforming
            * RMI/IDL exception types.
            *
            * Spec 28.2.3 (4)
            */
            Iterator it = Arrays.asList(m.getExceptionTypes()).iterator();

            while (it.hasNext()) {
                Class exception = (Class) it.next();

                if (!isRMIIDLExceptionType(exception))
                    return false;
            }
        }

        /*
        * The constant values defined in the interface MUST be
        * compile-time types of RMI/IDL primitive types or String.
        *
        * Spec 28.2.3 (6)
        */
        Iterator fieldIterator = Arrays.asList(type.getFields()).iterator();

        while (fieldIterator.hasNext()) {

            Field f = (Field) fieldIterator.next();

            if (f.getType().isPrimitive())
                continue;

            if (f.getType().equals(java.lang.String.class))
                continue;

            return false;
        }

        return true;
    }

    public static boolean isAbstractInterface(Class type) {

        /*
        * It must be a Java interface.
        */
        if (!type.isInterface())
            return false;

        /*
        * It must not be the interface of a CORBA object.
        */
        if (org.omg.CORBA.Object.class.isAssignableFrom(type))
            return false;

        /*
        * It must not extend java.rmi.Remote directly or indirectly.
        */
        if (java.rmi.Remote.class.isAssignableFrom(type))
            return false;

        Iterator methodIterator = Arrays.asList(type.getMethods()).iterator();

        while (methodIterator.hasNext()) {
            Method m = (Method) methodIterator.next();

            /*
            * All methods MUST throw java.rmi.RemoteException
            * or a superclass of it.
            */
            if (!throwsRemoteException(m)) {
                return false;
            }

        }

        return true;
    }

    public static boolean isRMIIDLExceptionType(Class type) {

        /*
        * A conforming RMI/IDL Exception class MUST be a checked
        * exception class and MUST be a valid RMI/IDL value type.
        *
        * Spec 28.2.6
        */
        if (!Throwable.class.isAssignableFrom(type))
            return false;

        if (Error.class.isAssignableFrom(type))
            return false;

        if (RuntimeException.class.isAssignableFrom(type))
            return false;

        if (!isRMIIDLValueType(type))
            return false;

        return true;
    }

    public static boolean isRMIIDLValueType(Class type) {

        /*
        * A value type MUST NOT either directly or indirectly implement the
        * java.rmi.Remote interface.
        *
        * Spec 28.2.4 (4)
        */
        if (java.rmi.Remote.class.isAssignableFrom(type))
            return false;


        /*
        * It cannot be a CORBA object.
        */
        if (org.omg.CORBA.Object.class.isAssignableFrom(type))
            return false;

        /*
        * If class is a non-static inner class then its containing class must
        * also be a conforming RMI/IDL value type.
        *
        * Spec 2.8.4 (3)
        */
        if (type.getDeclaringClass() != null && isStatic(type))
            if (!isRMIIDLValueType(type.getDeclaringClass()))
                return false;

        return true;
    }

    public static boolean isAbstractValueType(Class type) {

        if (!type.isInterface())
            return false;

        if (org.omg.CORBA.Object.class.isAssignableFrom(type))
            return false;

        boolean cannotBeRemote = false;
        boolean cannotBeAbstractInterface = false;

        if (java.rmi.Remote.class.isAssignableFrom(type)) {
            cannotBeAbstractInterface = true;
        } else {
            cannotBeRemote = true;
        }

        Iterator methodIterator = Arrays.asList(type.getMethods()).iterator();

        while (methodIterator.hasNext()) {
            Method m = (Method) methodIterator.next();

            if (!throwsRemoteException(m)) {
                cannotBeAbstractInterface = true;
                cannotBeRemote = true;
                break;
            }

            Iterator it = Arrays.asList(m.getExceptionTypes()).iterator();

            while (it.hasNext()) {
                Class exception = (Class) it.next();

                if (!isRMIIDLExceptionType(exception)) {
                    cannotBeRemote = true;
                    break;
                }
            }
        }

        if (!cannotBeRemote) {
            Iterator fieldIterator = Arrays.asList(type.getFields()).iterator();

            while (fieldIterator.hasNext()) {
                Field f = (Field) fieldIterator.next();

                if (f.getType().isPrimitive())
                    continue;
                if (f.getType().equals(java.lang.String.class))
                    continue;
                cannotBeRemote = true;
                break;
            }
        }
        return cannotBeRemote && cannotBeAbstractInterface;
    }

    public static void rethrowIfCorbaSystemException(Throwable e) {
        RuntimeException re;
        if (e instanceof java.rmi.MarshalException)
            re = new org.omg.CORBA.MARSHAL(e.toString());
        else if (e instanceof java.rmi.NoSuchObjectException)
            re = new org.omg.CORBA.OBJECT_NOT_EXIST(e.toString());
        else if (e instanceof java.rmi.AccessException)
            re = new org.omg.CORBA.NO_PERMISSION(e.toString());
        else if (e instanceof javax.transaction.TransactionRequiredException)
            re = new org.omg.CORBA.TRANSACTION_REQUIRED(e.toString());
        else if (e instanceof javax.transaction.TransactionRolledbackException)
            re = new org.omg.CORBA.TRANSACTION_ROLLEDBACK(e.toString());
        else if (e instanceof javax.transaction.InvalidTransactionException)
            re = new org.omg.CORBA.INVALID_TRANSACTION(e.toString());
        else if (e instanceof org.omg.CORBA.SystemException)
            re = (org.omg.CORBA.SystemException) e;
        else return;
        re.setStackTrace(e.getStackTrace());
        throw re;
    }
}
