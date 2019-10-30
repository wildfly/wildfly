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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;

import org.omg.CORBA.Any;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * This is a RMI/IIOP metadata conversion utility class.
 * <p/>
 * Routines here are conforming to the "Java(TM) Language to IDL Mapping
 * Specification", version 1.1 (01-06-07).
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 * @version $Revision: 81018 $
 */
public class Util {

    /**
     * Return the IDL type name for the given class.
     * Here we use the mapping for parameter types and return values.
     */
    public static String getTypeIDLName(Class cls)
            throws RMIIIOPViolationException {

        if (cls.isPrimitive())
            return PrimitiveAnalysis.getPrimitiveAnalysis(cls).getIDLName();

        if (cls.isArray()) {
            // boxedRMI 1.3.6
            Class componentClass = cls;
            int sequence = 0;
            while (componentClass.isArray()) {
                componentClass = componentClass.getComponentType();
                ++sequence;
            }

            String idlName = getTypeIDLName(componentClass);
            int idx = idlName.lastIndexOf("::");
            String idlModule = idlName.substring(0, idx + 2);
            String baseName = idlName.substring(idx + 2);
            return "::org::omg::boxedRMI" + idlModule + "seq" + sequence + "_" + baseName;
        }

        // special classes
        if (cls == java.lang.String.class)
            return "::CORBA::WStringValue";
        if (cls == java.lang.Object.class)
            return "::java::lang::_Object";
        if (cls == java.lang.Class.class)
            return "::javax::rmi::CORBA::ClassDesc";
        if (cls == java.io.Serializable.class)
            return "::java::io::Serializable";
        if (cls == java.io.Externalizable.class)
            return "::java::io::Externalizable";
        if (cls == java.rmi.Remote.class)
            return "::java::rmi::Remote";
        if (cls == org.omg.CORBA.Object.class)
            return "::CORBA::Object";


        // remote interface?
        if (cls.isInterface() && java.rmi.Remote.class.isAssignableFrom(cls)) {
            InterfaceAnalysis ia = InterfaceAnalysis.getInterfaceAnalysis(cls);

            return ia.getIDLModuleName() + "::" + ia.getIDLName();
        }

        // IDL interface?
        if (cls.isInterface() &&
                org.omg.CORBA.Object.class.isAssignableFrom(cls) &&
                org.omg.CORBA.portable.IDLEntity.class.isAssignableFrom(cls)) {
            InterfaceAnalysis ia = InterfaceAnalysis.getInterfaceAnalysis(cls);

            return ia.getIDLModuleName() + "::" + ia.getIDLName();
        }

        // exception?
        if (Throwable.class.isAssignableFrom(cls)) {
            if (Exception.class.isAssignableFrom(cls) &&
                    !RuntimeException.class.isAssignableFrom(cls)) {
                ExceptionAnalysis ea = ExceptionAnalysis.getExceptionAnalysis(cls);

                return ea.getIDLModuleName() + "::" + ea.getIDLName();
            }
        }

        // got to be value
        ValueAnalysis va = ValueAnalysis.getValueAnalysis(cls);

        return va.getIDLModuleName() + "::" + va.getIDLName();
    }

    /**
     * Check if this class is valid for RMI/IIOP mapping.
     * This method will either throw an exception or return true.
     */
    public static boolean isValidRMIIIOP(Class cls)
            throws RMIIIOPViolationException {
        if (cls.isPrimitive())
            return true;

        if (cls.isArray())
            return isValidRMIIIOP(cls.getComponentType());

        // special interfaces
        if (cls == Serializable.class || cls == Externalizable.class)
            return true;

        // interface?
        if (cls.isInterface() && java.rmi.Remote.class.isAssignableFrom(cls)) {
            InterfaceAnalysis.getInterfaceAnalysis(cls);
            return true;
        }

        // exception?
        if (Throwable.class.isAssignableFrom(cls)) {
            if (Exception.class.isAssignableFrom(cls) &&
                    !RuntimeException.class.isAssignableFrom(cls)) {
                ExceptionAnalysis.getExceptionAnalysis(cls);
            }
            return true;
        }

        // special values
        if (cls == Object.class || cls == String.class || cls == Class.class)
            return true;

        // got to be value
        ValueAnalysis.getValueAnalysis(cls);
        return true;
    }

    /**
     * Insert a java primitive into an Any.
     * The primitive is assumed to be wrapped in one of the primitive
     * wrapper classes.
     */
    public static void insertAnyPrimitive(Any any, Object primitive) {
        Class type = primitive.getClass();

        if (type == Boolean.class)
            any.insert_boolean(((Boolean) primitive).booleanValue());
        else if (type == Character.class)
            any.insert_wchar(((Character) primitive).charValue());
        else if (type == Byte.class)
            any.insert_octet(((Byte) primitive).byteValue());
        else if (type == Short.class)
            any.insert_short(((Short) primitive).shortValue());
        else if (type == Integer.class)
            any.insert_long(((Integer) primitive).intValue());
        else if (type == Long.class)
            any.insert_longlong(((Long) primitive).longValue());
        else if (type == Float.class)
            any.insert_float(((Float) primitive).floatValue());
        else if (type == Double.class)
            any.insert_double(((Double) primitive).doubleValue());
        else
            throw IIOPLogger.ROOT_LOGGER.notAPrimitive(type.getName());
    }

    /**
     * Map Java name to IDL name, as per sections 1.3.2.3, 1.3.2.4 and
     * 1.3.2.2.
     * This only works for a single name component, without a qualifying
     * dot.
     */
    public static String javaToIDLName(String name) {
        if (name == null || "".equals(name) || name.indexOf('.') != -1)
            throw IIOPLogger.ROOT_LOGGER.nameCannotBeNullEmptyOrQualified();

        StringBuffer res = new StringBuffer(name.length());

        if (name.charAt(0) == '_')
            res.append('J'); // 1.3.2.3

        for (int i = 0; i < name.length(); ++i) {
            char c = name.charAt(i);

            if (isLegalIDLIdentifierChar(c))
                res.append(c);
            else // 1.3.2.4
                res.append('U').append(toHexString((int) c));
        }

        String s = res.toString();

        if (isReservedIDLKeyword(s))
            return "_" + s;
        else
            return s;
    }

    /**
     * Return the IR global ID of the given class or interface.
     * This is described in section 1.3.5.7.
     * The returned string is in the RMI hashed format, like
     * "RMI:java.util.Hashtable:C03324C0EA357270:13BB0F25214AE4B8".
     */
    public static String getIRIdentifierOfClass(Class cls) {
        if (cls.isPrimitive())
            throw IIOPLogger.ROOT_LOGGER.primitivesHaveNoIRIds();

        String result = (String) classIRIdentifierCache.get(cls);
        if (result != null)
            return result;

        String name = cls.getName();
        StringBuffer b = new StringBuffer("RMI:");

        for (int i = 0; i < name.length(); ++i) {
            char c = name.charAt(i);

            if (c < 256)
                b.append(c);
            else
                b.append("\\U").append(toHexString((int) c));
        }

        long clsHash = getClassHashCode(cls);

        b.append(':').append(toHexString(clsHash));

        ObjectStreamClass osClass = ObjectStreamClass.lookup(cls);
        if (osClass != null) {
            long serialVersionUID = osClass.getSerialVersionUID();

            if (clsHash != serialVersionUID)
                b.append(':').append(toHexString(serialVersionUID));
        }

        result = b.toString();

        classIRIdentifierCache.put(cls, result);

        return result;
    }


    // Private -------------------------------------------------------

    /**
     * A cache for calculated class hash codes.
     */
    private static Map classHashCodeCache =
            Collections.synchronizedMap(new WeakHashMap());

    /**
     * A cache for class IR identifiers.
     */
    private static Map classIRIdentifierCache =
            Collections.synchronizedMap(new WeakHashMap());

    /**
     * Reserved IDL keywords.
     * <p/>
     * Section 1.3.2.2 says that Java identifiers with these names
     * should have prepended an underscore.
     */
    private static final String[] reservedIDLKeywords = new String[]{
            "abstract",
            "any",
            "attribute",
            "boolean",
            "case",
            "char",
            "const",
            "context",
            "custom",
            "default",
            "double",
            "exception",
            "enum",
            "factory",
            "FALSE",
            "fixed",
            "float",
            "in",
            "inout",
            "interface",
            "local",
            "long",
            "module",
            "native",
            "Object",
            "octet",
            "oneway",
            "out",
            "private",
            "public",
            "raises",
            "readonly",
            "sequence",
            "short",
            "string",
            "struct",
            "supports",
            "switch",
            "TRUE",
            "truncatable",
            "typedef",
            "unsigned",
            "union",
            "ValueBase",
            "valuetype",
            "void",
            "wchar",
            "wstring"
    };

    static {
        // Initialize caches
        classHashCodeCache = Collections.synchronizedMap(new WeakHashMap());

        classIRIdentifierCache = Collections.synchronizedMap(new WeakHashMap());
    }

    /**
     * Convert an integer to a 16-digit hex string.
     */
    private static String toHexString(int i) {
        String s = Integer.toHexString(i).toUpperCase(Locale.ENGLISH);

        if (s.length() < 8)
            return "00000000".substring(8 - s.length()) + s;
        else
            return s;
    }

    /**
     * Convert a long to a 16-digit hex string.
     */
    private static String toHexString(long l) {
        String s = Long.toHexString(l).toUpperCase(Locale.ENGLISH);

        if (s.length() < 16)
            return "0000000000000000".substring(16 - s.length()) + s;
        else
            return s;
    }

    /**
     * Determine if the argument is a reserved IDL keyword.
     */
    private static boolean isReservedIDLKeyword(String s) {
        // TODO: faster lookup
        for (int i = 0; i < reservedIDLKeywords.length; ++i)
            if (reservedIDLKeywords[i].equals(s))
                return true;
        return false;
    }

    /**
     * Determine if a <code>char</code> is a legal IDL identifier character.
     */
    private static boolean isLegalIDLIdentifierChar(char c) {
        if (c >= 0x61 && c <= 0x7a)
            return true; // lower case letter

        if (c >= 0x30 && c <= 0x39)
            return true; // digit

        if (c >= 0x41 && c <= 0x5a)
            return true; // upper case letter

        if (c == '_')
            return true; // underscore

        return false;
    }

    /**
     * Determine if a <code>char</code> is legal start of an IDL identifier.
     */
    private static boolean isLegalIDLStartIdentifierChar(char c) {
        if (c >= 0x61 && c <= 0x7a)
            return true; // lower case letter

        if (c >= 0x41 && c <= 0x5a)
            return true; // upper case letter

        return false;
    }

    /**
     * Return the class hash code, as specified in "The Common Object
     * Request Broker: Architecture and Specification" (01-02-33),
     * section 10.6.2.
     */
    static long getClassHashCode(Class cls) {
        // The simple cases
        if (cls.isInterface())
            return 0;
        if (!Serializable.class.isAssignableFrom(cls))
            return 0;
        if (Externalizable.class.isAssignableFrom(cls))
            return 1;

        // Try cache
        Long l = (Long) classHashCodeCache.get(cls);
        if (l != null)
            return l.longValue();

        // Has to calculate the hash.

        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        DataOutputStream dos = new DataOutputStream(baos);

        // Step 1
        Class superClass = cls.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            try {
                dos.writeLong(getClassHashCode(superClass));
            } catch (IOException ex) {
                throw IIOPLogger.ROOT_LOGGER.unexpectedException(ex);
            }
        }

        // Step 2
        boolean hasWriteObject = false;
        try {
            Method m;
            int mods;

            m = cls.getDeclaredMethod("writeObject",
                    new Class[]{ObjectOutputStream.class});
            mods = m.getModifiers();

            if (!Modifier.isPrivate(mods) && !Modifier.isStatic(mods))
                hasWriteObject = true;
        } catch (NoSuchMethodException ex) {
            // ignore
        }
        try {
            dos.writeInt(hasWriteObject ? 2 : 1);
        } catch (IOException ex) {
            throw IIOPLogger.ROOT_LOGGER.unexpectedException(ex);
        }

        // Step 3
        Field[] fields = cls.getDeclaredFields();
        SortedSet set = new TreeSet(new FieldComparator());

        for (int i = 0; i < fields.length; ++i) {
            int mods = fields[i].getModifiers();

            if (!Modifier.isStatic(mods) && !Modifier.isTransient(mods))
                set.add(fields[i]);
        }
        Iterator iter = set.iterator();
        try {
            while (iter.hasNext()) {
                Field f = (Field) iter.next();

                dos.writeUTF(f.getName());
                dos.writeUTF(getSignature(f.getType()));
            }
        } catch (IOException ex) {
            throw IIOPLogger.ROOT_LOGGER.unexpectedException(ex);
        }

        // Convert to byte[]
        try {
            dos.flush();
        } catch (IOException ex) {
            throw IIOPLogger.ROOT_LOGGER.unexpectedException(ex);
        }
        byte[] bytes = baos.toByteArray();

        // Calculate SHA digest
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException ex) {
            throw IIOPLogger.ROOT_LOGGER.unavailableSHADigest(ex);
        }
        digest.update(bytes);
        byte[] sha = digest.digest();

        // Calculate hash as per section 10.6.2
        long hash = 0;
        for (int i = 0; i < Math.min(8, sha.length); i++) {
            hash += (long) (sha[i] & 255) << (i * 8);
        }

        // Save in cache
        classHashCodeCache.put(cls, new Long(hash));

        return hash;
    }

    /**
     * Calculate the signature of a class, according to the Java VM
     * specification, section 4.3.2.
     */
    private static String getSignature(Class cls) {
        if (cls.isArray())
            return "[" + cls.getComponentType();

        if (cls.isPrimitive()) {
            if (cls == Byte.TYPE)
                return "B";
            if (cls == Character.TYPE)
                return "C";
            if (cls == Double.TYPE)
                return "D";
            if (cls == Float.TYPE)
                return "F";
            if (cls == Integer.TYPE)
                return "I";
            if (cls == Long.TYPE)
                return "J";
            if (cls == Short.TYPE)
                return "S";
            if (cls == Boolean.TYPE)
                return "Z";
            throw IIOPLogger.ROOT_LOGGER.unknownPrimitiveType(cls.getName());
        }

        return "L" + cls.getName().replace('.', '/') + ";";
    }

    /**
     * Calculate the signature of a method, according to the Java VM
     * specification, section 4.3.3.
     */
    private static String getSignature(Method method) {
        StringBuffer b = new StringBuffer("(");
        Class[] parameterTypes = method.getParameterTypes();

        for (int i = 0; i < parameterTypes.length; ++i)
            b.append(getSignature(parameterTypes[i]));

        b.append(')').append(getSignature(method.getReturnType()));

        return b.toString();
    }

    /**
     * Handle mappings for primitive types, as per section 1.3.3.
     */
    static String primitiveTypeIDLName(Class type) {
        if (type == Void.TYPE)
            return "void";
        if (type == Boolean.TYPE)
            return "boolean";
        if (type == Character.TYPE)
            return "wchar";
        if (type == Byte.TYPE)
            return "octet";
        if (type == Short.TYPE)
            return "short";
        if (type == Integer.TYPE)
            return "long";
        if (type == Long.TYPE)
            return "long long";
        if (type == Float.TYPE)
            return "float";
        if (type == Double.TYPE)
            return "double";
        throw IIOPLogger.ROOT_LOGGER.notAPrimitive(type.getName());
    }


    // Inner classes -------------------------------------------------

    /**
     * A <code>Comparator</code> for <code>Field</code>s, ordering the
     * fields according to the lexicographic ordering of their Java names.
     */
    private static class FieldComparator
            implements Comparator {
        public int compare(Object o1, Object o2) {
            if (o1 == o2)
                return 0;

            String n1 = ((Field) o1).getName();
            String n2 = ((Field) o2).getName();

            return n1.compareTo(n2);
        }
    }

}
