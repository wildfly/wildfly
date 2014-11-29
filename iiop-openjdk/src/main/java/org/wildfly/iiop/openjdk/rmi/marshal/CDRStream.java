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
package org.wildfly.iiop.openjdk.rmi.marshal;

import java.io.Externalizable;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.rmi.CORBA.Util;
import javax.rmi.PortableRemoteObject;

import org.omg.CORBA.portable.IDLEntity;
import org.omg.CORBA_2_3.portable.InputStream;
import org.omg.CORBA_2_3.portable.OutputStream;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;
import org.wildfly.iiop.openjdk.rmi.RmiIdlUtil;

/**
 * Utility class with static methods to:
 * <ul>
 * <li>get the <code>CDRStreamReader</code> for a given class</li>
 * <li>get the <code>CDRStreamWriter</code> for a given class</li>
 * </ul>
 * <p/>
 * The <code>CDRStreamReader</code>s and <code>CDRStreamWriter</code>s
 * returned by these methods are instances of static inner classes
 * defined by <code>CDRStream</code>.
 *
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 */
public class CDRStream {
    /**
     * Returns the abbreviated name of the marshaller for given
     * <code>Class</code>.
     * <p/>
     * <p>Abbreviated names of marshallers for basic types follow the usual
     * Java convention:
     * <br>
     * <pre>
     *    type           abbrev name
     *    boolean           "Z"
     *    byte              "B"
     *    char              "C"
     *    double            "D"
     *    float             "F"
     *    int               "I"
     *    long              "J"
     *    short             "S"
     *    void              "V"
     * </pre>
     * <p/>
     * <p>The abbreviated names of marshallers for object types are:
     * <br>
     * <pre>
     *    java.lang.String                     "G" (strinG)
     *    RMI remote interface                 "R" + interfaceName
     *    RMI abstract interface               "A"
     *    serializable                         "E" (sErializablE)
     *    valuetype                            "L" + className
     *    externalizable                       "X" (eXternalizable)
     *    org.omg.CORBA.Object                 "M" (oMg)
     *    IDL interface                        "N" + interfaceName
     *    java.lang.Object                     "O"
     * </pre>
     * <p/>
     * <p>As an example: the abbreviated name of a marshaller for a valuetype
     * class named <code>Foo</code> is the string <code>"LFoo"</code>.
     */
    public static String abbrevFor(Class clz) {
        if (clz == Boolean.TYPE) {
            return "Z";
        } else if (clz == Byte.TYPE) {
            return "B";
        } else if (clz == Character.TYPE) {
            return "C";
        } else if (clz == Double.TYPE) {
            return "D";
        } else if (clz == Float.TYPE) {
            return "F";
        } else if (clz == Integer.TYPE) {
            return "I";
        } else if (clz == Long.TYPE) {
            return "J";
        } else if (clz == Short.TYPE) {
            return "S";
        } else if (clz == Void.TYPE) {
            return "V";
        } else if (clz == String.class) {
            return "G"; // strinG
        } else if (RmiIdlUtil.isRMIIDLRemoteInterface(clz)) {
            return "R" + clz.getName(); // Remote interface
        } else if (clz == org.omg.CORBA.Object.class) {
            return "M"; // oMg (CORBA Object)
        } else if (org.omg.CORBA.Object.class.isAssignableFrom(clz)) {
            return "N" + clz.getName(); // IDL iNterface
        } else if (IDLEntity.class.isAssignableFrom(clz)) {
            return "L" + clz.getName(); // vaLuetype
        } else if (clz == Serializable.class) {
            return "E"; // sErializablE
        } else if (RmiIdlUtil.isAbstractInterface(clz)) {
            return "A"; // Abstract interface
        } else if (Serializable.class.isAssignableFrom(clz)) {
            return "L" + clz.getName(); // vaLuetype
        } else if (Externalizable.class.isAssignableFrom(clz)) {
            return "X"; // eXternalizable
        } else if (clz == Object.class) {
            return "O"; // Object
        } else {
            return "L" + clz.getName(); // vaLuetype
        }
    }

    /**
     * Returns a <code>CDRStreamReader</code> given an abbreviated name
     * and a <code>ClassLoader</code> for valuetype classes.
     */
    public static CDRStreamReader readerFor(String s, ClassLoader cl) {
        switch (s.charAt(0)) {

            case 'A':
                return AbstractInterfaceReader.instance;
            case 'B':
                return ByteReader.instance;
            case 'C':
                return CharReader.instance;
            case 'D':
                return DoubleReader.instance;
            case 'E':
                return SerializableReader.instance;
            case 'F':
                return FloatReader.instance;
            case 'G':
                return StringReader.instance;
            case 'I':
                return IntReader.instance;
            case 'J':
                return LongReader.instance;
            case 'L':
                try {
                    // Use Class.forName() (rather than cl.loadClass()), because
                    // Class.forName() loads Java array types (which are valuetypes).
                    return new ValuetypeReader(Class.forName(s.substring(1),
                            true,
                            cl));
                } catch (ClassNotFoundException e) {
                    throw IIOPLogger.ROOT_LOGGER.errorLoadingClass(s.substring(1), e);
                }
            case 'M':
                return CorbaObjectReader.instance;
            case 'N':
                try {
                    return new IdlInterfaceReader(cl.loadClass(s.substring(1)));
                } catch (ClassNotFoundException e) {
                    throw IIOPLogger.ROOT_LOGGER.errorLoadingClass(s.substring(1), e);
                }
            case 'O':
                return ObjectReader.instance;
            case 'R':
                try {
                    return new RemoteReader(cl.loadClass(s.substring(1)));
                } catch (ClassNotFoundException e) {
                    throw IIOPLogger.ROOT_LOGGER.errorLoadingClass(s.substring(1), e);
                }
            case 'S':
                return ShortReader.instance;
            case 'V':
                return null;
            case 'X':
                return ExternalizableReader.instance;
            case 'Z':
                return BooleanReader.instance;
            default:
                return null;
        }
    }

    /**
     * Returns a <code>CDRStreamWriter</code> given an abbreviated name
     * and a <code>ClassLoader</code> for valuetype classes.
     */
    public static CDRStreamWriter writerFor(String s, ClassLoader cl) {
        switch (s.charAt(0)) {

            case 'A':
                return AbstractInterfaceWriter.instance;
            case 'B':
                return ByteWriter.instance;
            case 'C':
                return CharWriter.instance;
            case 'D':
                return DoubleWriter.instance;
            case 'E':
                return SerializableWriter.instance;
            case 'F':
                return FloatWriter.instance;
            case 'G':
                return StringWriter.instance;
            case 'I':
                return IntWriter.instance;
            case 'J':
                return LongWriter.instance;
            case 'L':
                try {
                    // Use Class.forName() (rather than cl.loadClass()), because
                    // Class.forName() loads Java array types (which are valuetypes).
                    return new ValuetypeWriter(Class.forName(s.substring(1),
                            true,
                            cl));
                } catch (ClassNotFoundException e) {
                    throw IIOPLogger.ROOT_LOGGER.errorLoadingClass(s.substring(1), e);
                }
            case 'M':
                return CorbaObjectWriter.instance;
            case 'N':
                try {
                    return new IdlInterfaceWriter(cl.loadClass(s.substring(1)));
                } catch (ClassNotFoundException e) {
                    throw IIOPLogger.ROOT_LOGGER.errorLoadingClass(s.substring(1), e);
                }
            case 'O':
                return ObjectWriter.instance;
            case 'R':
                return RemoteWriter.instance;
            case 'S':
                return ShortWriter.instance;
            case 'V':
                return null;
            case 'X':
                return ExternalizableWriter.instance;
            case 'Z':
                return BooleanWriter.instance;
            default:
                return null;
        }
    }

    /**
     * Returns the <code>CDRStreamReader</code> for a given <code>Class</code>.
     */
    public static CDRStreamReader readerFor(Class clz) {
        if (clz == Boolean.TYPE) {
            return BooleanReader.instance;
        } else if (clz == Byte.TYPE) {
            return ByteReader.instance;
        } else if (clz == Character.TYPE) {
            return CharReader.instance;
        } else if (clz == Double.TYPE) {
            return DoubleReader.instance;
        } else if (clz == Float.TYPE) {
            return FloatReader.instance;
        } else if (clz == Integer.TYPE) {
            return IntReader.instance;
        } else if (clz == Long.TYPE) {
            return LongReader.instance;
        } else if (clz == Short.TYPE) {
            return ShortReader.instance;
        } else if (clz == Void.TYPE) {
            return null;
        } else if (clz == String.class) {
            return StringReader.instance;
        } else if (RmiIdlUtil.isRMIIDLRemoteInterface(clz)) {
            return new RemoteReader(clz);
        } else if (clz == org.omg.CORBA.Object.class) {
            return CorbaObjectReader.instance;
        } else if (org.omg.CORBA.Object.class.isAssignableFrom(clz)) {
            return new IdlInterfaceReader(clz);
        } else if (IDLEntity.class.isAssignableFrom(clz)) {
            return new ValuetypeReader(clz);
        } else if (clz == Serializable.class) {
            return SerializableReader.instance;
        } else if (RmiIdlUtil.isAbstractInterface(clz)) {
            return AbstractInterfaceReader.instance;
        } else if (Serializable.class.isAssignableFrom(clz)) {
            return new ValuetypeReader(clz);
        } else if (Externalizable.class.isAssignableFrom(clz)) {
            return ExternalizableReader.instance;
        } else if (clz == Object.class) {
            return ObjectReader.instance;
        } else {
            return new ValuetypeReader(clz);
        }
    }

    /**
     * Returns the <code>CDRStreamWriter</code> for a given <code>Class</code>.
     */
    public static CDRStreamWriter writerFor(Class clz) {
        if (clz == Boolean.TYPE) {
            return BooleanWriter.instance;
        } else if (clz == Byte.TYPE) {
            return ByteWriter.instance;
        } else if (clz == Character.TYPE) {
            return CharWriter.instance;
        } else if (clz == Double.TYPE) {
            return DoubleWriter.instance;
        } else if (clz == Float.TYPE) {
            return FloatWriter.instance;
        } else if (clz == Integer.TYPE) {
            return IntWriter.instance;
        } else if (clz == Long.TYPE) {
            return LongWriter.instance;
        } else if (clz == Short.TYPE) {
            return ShortWriter.instance;
        } else if (clz == String.class) {
            return StringWriter.instance;
        } else if (clz == Void.TYPE) {
            return null;
        } else if (RmiIdlUtil.isRMIIDLRemoteInterface(clz)) {
            return RemoteWriter.instance;
        } else if (clz == org.omg.CORBA.Object.class) {
            return CorbaObjectWriter.instance;
        } else if (org.omg.CORBA.Object.class.isAssignableFrom(clz)) {
            return new IdlInterfaceWriter(clz);
        } else if (IDLEntity.class.isAssignableFrom(clz)) {
            return new ValuetypeWriter(clz);
        } else if (clz == Serializable.class) {
            return SerializableWriter.instance;
        } else if (RmiIdlUtil.isAbstractInterface(clz)) {
            return AbstractInterfaceWriter.instance;
        } else if (Serializable.class.isAssignableFrom(clz)) {
            return new ValuetypeWriter(clz);
        } else if (Externalizable.class.isAssignableFrom(clz)) {
            return ExternalizableWriter.instance;
        } else if (clz == Object.class) {
            return ObjectWriter.instance;
        } else {
            return new ValuetypeWriter(clz);
        }
    }

    // Private -----------------------------------------------------------------

    // Static inner classes (all of them private) ------------------------------

    /**
     * Singleton class that unmarshals <code>boolean</code>s from a CDR input
     * stream.
     */
    private static final class BooleanReader
            implements CDRStreamReader {
        static final CDRStreamReader instance = new BooleanReader();

        private BooleanReader() {
        }

        public Object read(InputStream in) {
            return new Boolean(in.read_boolean());
        }
    }

    /**
     * Singleton class that unmarshals <code>byte</code>s from a CDR input
     * stream.
     */
    private static final class ByteReader
            implements CDRStreamReader {
        static final CDRStreamReader instance = new ByteReader();

        private ByteReader() {
        }

        public Object read(InputStream in) {
            return new Byte(in.read_octet());
        }
    }

    /**
     * Singleton class that unmarshals <code>char</code>s from a CDR input
     * stream.
     */
    private static final class CharReader
            implements CDRStreamReader {
        static final CDRStreamReader instance = new CharReader();

        private CharReader() {
        }

        public Object read(InputStream in) {
            return new Character(in.read_wchar());
        }
    }

    /**
     * Singleton class that unmarshals <code>double</code>s from a CDR input
     * stream.
     */
    private static final class DoubleReader
            implements CDRStreamReader {
        static final CDRStreamReader instance = new DoubleReader();

        private DoubleReader() {
        }

        public Object read(InputStream in) {
            return new Double(in.read_double());
        }
    }

    /**
     * Singleton class that unmarshals <code>float</code>s from a CDR input
     * stream.
     */
    private static final class FloatReader
            implements CDRStreamReader {
        static final CDRStreamReader instance = new FloatReader();

        private FloatReader() {
        }

        public Object read(InputStream in) {
            return new Float(in.read_float());
        }
    }

    /**
     * Singleton class that unmarshals <code>int</code>s from a CDR input
     * stream.
     */
    private static final class IntReader
            implements CDRStreamReader {
        static final CDRStreamReader instance = new IntReader();

        private IntReader() {
        }

        public Object read(InputStream in) {
            return new Integer(in.read_long());
        }
    }

    /**
     * Singleton class that unmarshals <code>long</code>s from a CDR input
     * stream.
     */
    private static final class LongReader
            implements CDRStreamReader {
        static final CDRStreamReader instance = new LongReader();

        private LongReader() {
        }

        public Object read(InputStream in) {
            return new Long(in.read_longlong());
        }
    }

    /**
     * Singleton class that unmarshals <code>short</code>s from a CDR input
     * stream.
     */
    private static final class ShortReader
            implements CDRStreamReader {
        static final CDRStreamReader instance = new ShortReader();

        private ShortReader() {
        }

        public Object read(InputStream in) {
            return new Short(in.read_short());
        }
    }

    /**
     * Singleton class that unmarshals <code>String</code>s from a CDR input
     * stream.
     */
    private static final class StringReader
            implements CDRStreamReader {
        static final CDRStreamReader instance = new StringReader();

        private StringReader() {
        }

        public Object read(InputStream in) {
            return in.read_value(String.class);
        }
    }

    /**
     * Class that unmarshals <code>java.rmi.Remote</code> objects from a CDR
     * input stream. A <code>RemoteReader</code> is specific for a given
     * remote interface, which is passed as a parameter to the
     * <code>RemoteReader</code> constructor.
     */
    private static final class RemoteReader
            implements CDRStreamReader {
        private Class clz;

        RemoteReader(Class clz) {
            this.clz = clz;
        }

        public Object read(InputStream in) {
            // The narrow() call downloads the stub from the codebase embedded
            // within the IOR of the unmarshalled object.
            return PortableRemoteObject.narrow(in.read_Object(), clz);
        }
    }

    /**
     * Singleton class that unmarshals objects whose declared type is
     * <code>java.lang.Object</code> from a CDR input stream.
     */
    private static final class ObjectReader
            implements CDRStreamReader {
        static final CDRStreamReader instance = new ObjectReader();

        private ObjectReader() {
        }

        public Object read(InputStream in) {
            return Util.readAny(in);
        }
    }

    /**
     * Singleton class that unmarshals objects whose declared type is
     * <code>java.io.Serializable</code> from a CDR input stream.
     */
    private static final class SerializableReader
            implements CDRStreamReader {
        static final CDRStreamReader instance = new SerializableReader();

        private SerializableReader() {
        }

        public Object read(InputStream in) {
            return (Serializable) Util.readAny(in);
        }
    }

    /**
     * Singleton class that unmarshals objects whose declared type is
     * <code>java.io.Externalizable</code> from a CDR input stream.
     */
    private static final class ExternalizableReader
            implements CDRStreamReader {
        static final CDRStreamReader instance = new ExternalizableReader();

        private ExternalizableReader() {
        }

        public Object read(InputStream in) {
            return (Externalizable) Util.readAny(in);
        }
    }

    /**
     * Singleton class that unmarshals objects whose declared type is
     * <code>org.omg.CORBA.Object</code> from a CDR input stream.
     */
    private static final class CorbaObjectReader
            implements CDRStreamReader {
        static final CDRStreamReader instance = new CorbaObjectReader();

        private CorbaObjectReader() {
        }

        public Object read(InputStream in) {
            return (org.omg.CORBA.Object) in.read_Object();
        }
    }

    /**
     * Class that unmarshals IDL interfaces from a CDR input stream.
     * An <code>IdlInterfaceReader</code> is specific for objects that
     * implement the Java interface passed as a parameter to the
     * <code>IdlInterfaceReader</code> constructor. This Java interface
     * must extend <code>org.omg.CORBA.Object</code>.
     */
    private static final class IdlInterfaceReader
            implements CDRStreamReader {
        private static Class[] paramTypes = {org.omg.CORBA.portable.InputStream.class};

        // The readMethod for this IdlInterfaceReader.
        private Method readMethod = null;

        IdlInterfaceReader(Class clz) {
            String helperClassName = clz.getName() + "Helper";

            try {
                Class helperClass =
                        clz.getClassLoader().loadClass(helperClassName);
                readMethod = helperClass.getMethod("read", paramTypes);
            } catch (ClassNotFoundException e) {
                throw IIOPLogger.ROOT_LOGGER.errorLoadingClass(helperClassName, e);
            } catch (NoSuchMethodException e) {
                throw IIOPLogger.ROOT_LOGGER.noReadMethodInHelper(helperClassName, e);
            }
        }

        public Object read(InputStream in) {
            try {
                return readMethod.invoke(null, new Object[]{in});
            } catch (IllegalAccessException e) {
                throw IIOPLogger.ROOT_LOGGER.unexpectedException(e);
            } catch (InvocationTargetException e) {
                throw IIOPLogger.ROOT_LOGGER.errorUnmarshaling(org.omg.CORBA.Object.class, e.getTargetException());
            }
        }
    }

    /**
     * Singleton class that unmarshals from a CDR output stream objects whose
     * declared type is an interface that does not extend
     * <code>java.rmi.Remote</code> and whose declared methods
     * (including inherited ones) all throw
     * <code>java.rmi.RemoteException</code>.
     */
    private static final class AbstractInterfaceReader
            implements CDRStreamReader {
        static final CDRStreamReader instance = new AbstractInterfaceReader();

        private AbstractInterfaceReader() {
        }

        public Object read(InputStream in) {
            return in.read_abstract_interface();
        }
    }

    /**
     * Class that unmarshals valuetypes from a CDR input stream.
     * A <code>ValuetypeReader</code> is specific for objects of a given class,
     * which is passed as a parameter to the <code>ValuetypeReader</code>
     * constructor and must implement <code>java.io.Serializable</code>.
     */
    private static final class ValuetypeReader
            implements CDRStreamReader {
        private Class clz;

        ValuetypeReader(Class clz) {
            this.clz = clz;
        }

        public Object read(InputStream in) {
            return in.read_value(clz);
        }
    }

    /**
     * Singleton class that marshals <code>boolean</code>s into a CDR output
     * stream.
     */
    private static final class BooleanWriter
            implements CDRStreamWriter {
        static final CDRStreamWriter instance = new BooleanWriter();

        private BooleanWriter() {
        }

        public void write(OutputStream out, Object obj) {
            out.write_boolean(((Boolean) obj).booleanValue());
        }
    }

    /**
     * Singleton class that marshals <code>byte</code>s into a CDR output
     * stream.
     */
    private static final class ByteWriter
            implements CDRStreamWriter {
        static final CDRStreamWriter instance = new ByteWriter();

        private ByteWriter() {
        }

        public void write(OutputStream out, Object obj) {
            out.write_octet(((Byte) obj).byteValue());
        }
    }

    /**
     * Singleton class that marshals <code>char</code>s into a CDR output
     * stream.
     */
    private static final class CharWriter
            implements CDRStreamWriter {

        static final CDRStreamWriter instance = new CharWriter();

        private CharWriter() {
        }

        public void write(OutputStream out, Object obj) {
            out.write_wchar(((Character) obj).charValue());
        }
    }

    /**
     * Singleton class that marshals <code>double</code>s into a CDR output
     * stream.
     */
    private static final class DoubleWriter
            implements CDRStreamWriter {
        static final CDRStreamWriter instance = new DoubleWriter();

        private DoubleWriter() {
        }

        public void write(OutputStream out, Object obj) {
            out.write_double(((Double) obj).doubleValue());
        }
    }

    /**
     * Singleton class that marshals <code>float</code>s into a CDR output
     * stream.
     */
    private static final class FloatWriter
            implements CDRStreamWriter {
        static final CDRStreamWriter instance = new FloatWriter();

        private FloatWriter() {
        }

        public void write(OutputStream out, Object obj) {
            out.write_float(((Float) obj).floatValue());
        }
    }

    /**
     * Singleton class that marshals <code>int</code>s into a CDR output
     * stream.
     */
    private static final class IntWriter
            implements CDRStreamWriter {
        static final CDRStreamWriter instance = new IntWriter();

        private IntWriter() {
        }

        public void write(OutputStream out, Object obj) {
            out.write_long(((Integer) obj).intValue());
        }
    }

    /**
     * Singleton class that marshals <code>long</code>s into a CDR output
     * stream.
     */
    private static final class LongWriter
            implements CDRStreamWriter {
        static final CDRStreamWriter instance = new LongWriter();

        private LongWriter() {
        }

        public void write(OutputStream out, Object obj) {
            out.write_longlong(((Long) obj).longValue());
        }
    }

    /**
     * Singleton class that marshals <code>short</code>s into a CDR output
     * stream.
     */
    private static final class ShortWriter
            implements CDRStreamWriter {
        static final CDRStreamWriter instance = new ShortWriter();

        private ShortWriter() {
        }

        public void write(OutputStream out, Object obj) {
            out.write_short(((Short) obj).shortValue());
        }
    }

    /**
     * Singleton class that marshals <code>String</code>s into a CDR output
     * stream.
     */
    private static final class StringWriter
            implements CDRStreamWriter {
        static final CDRStreamWriter instance = new StringWriter();

        private StringWriter() {
        }

        public void write(OutputStream out, Object obj) {
            out.write_value((String) obj, String.class);
        }
    }

    /**
     * Singleton class that marshals <code>java.rmi.Remote</code> objects into
     * a CDR output stream.
     */
    private static final class RemoteWriter
            implements CDRStreamWriter {
        static final CDRStreamWriter instance = new RemoteWriter();

        private RemoteWriter() {
        }

        public void write(OutputStream out, Object obj) {
            out.write_Object((org.omg.CORBA.Object) obj);
        }
    }

    /**
     * Singleton class that marshals objects whose declared type is
     * <code>java.lang.Object</code> into a CDR output stream.
     */
    private static final class ObjectWriter
            implements CDRStreamWriter {
        static final CDRStreamWriter instance = new ObjectWriter();

        private ObjectWriter() {
        }

        public void write(OutputStream out, Object obj) {
            Util.writeAny(out, obj);
        }
    }

    /**
     * Singleton class that marshals objects whose declared type is
     * <code>java.io.Serializable</code> into a CDR output stream.
     */
    private static final class SerializableWriter
            implements CDRStreamWriter {
        static final CDRStreamWriter instance = new SerializableWriter();

        private SerializableWriter() {
        }

        public void write(OutputStream out, Object obj) {
            Util.writeAny(out, (Serializable) obj);
        }
    }

    /**
     * Singleton class that marshals objects whose declared type is
     * <code>java.io.Externalizable</code> into a CDR output stream.
     */
    private static final class ExternalizableWriter
            implements CDRStreamWriter {
        static final CDRStreamWriter instance = new ExternalizableWriter();

        private ExternalizableWriter() {
        }

        public void write(OutputStream out, Object obj) {
            Util.writeAny(out, (Externalizable) obj);
        }
    }

    /**
     * Singleton class that marshals objects whose declared type is
     * <code>org.omg.CORBA.Object</code> into a CDR output stream.
     */
    private static final class CorbaObjectWriter
            implements CDRStreamWriter {
        static final CDRStreamWriter instance = new CorbaObjectWriter();

        private CorbaObjectWriter() {
        }

        public void write(OutputStream out, Object obj) {
            out.write_Object((org.omg.CORBA.Object) obj);
        }
    }

    /**
     * Class that marshals IDL interfaces into a CDR output stream.
     * An <code>IdlInterfaceWriter</code> is specific for objects that
     * implement the Java interface is passed as a parameter to the
     * <code>IdlInterfaceWriter</code> constructor. This Java interface
     * must extend <code>org.omg.CORBA.Object</code>.
     */
    private static final class IdlInterfaceWriter
            implements CDRStreamWriter {
        // The writeMethod for this IdlInterfaceReader.
        private Method writeMethod = null;

        IdlInterfaceWriter(Class clz) {
            String helperClassName = clz.getName() + "Helper";

            try {
                Class helperClass =
                        clz.getClassLoader().loadClass(helperClassName);
                Class[] paramTypes = {
                        org.omg.CORBA.portable.OutputStream.class,
                        clz
                };
                writeMethod = helperClass.getMethod("write", paramTypes);
            } catch (ClassNotFoundException e) {
                throw IIOPLogger.ROOT_LOGGER.errorLoadingClass(helperClassName, e);
            } catch (NoSuchMethodException e) {
                throw IIOPLogger.ROOT_LOGGER.noWriteMethodInHelper(helperClassName, e);
            }
        }

        public void write(OutputStream out, Object obj) {
            try {
                writeMethod.invoke(null, new Object[]{out, obj});
            } catch (IllegalAccessException e) {
                throw IIOPLogger.ROOT_LOGGER.unexpectedException(e);
            } catch (InvocationTargetException e) {
                throw IIOPLogger.ROOT_LOGGER.errorMarshaling(org.omg.CORBA.Object.class, e.getTargetException());
            }
        }
    }

    /**
     * Singleton class that marshals into a CDR output stream objects whose
     * declared type is an interface that does not extend
     * <code>java.rmi.Remote</code> and whose declared methods
     * (including inherited ones) all throw
     * <code>java.rmi.RemoteException</code>.
     */
    private static final class AbstractInterfaceWriter
            implements CDRStreamWriter {
        static final CDRStreamWriter instance = new AbstractInterfaceWriter();

        private AbstractInterfaceWriter() {
        }

        public void write(OutputStream out, Object obj) {
            out.write_abstract_interface(obj);
        }
    }

    /**
     * Class that marshals valuetypes into a CDR output stream.
     * A <code>ValuetypeWriter</code> is specific for objects of a given class,
     * which is passed as a parameter to the <code>ValuetypeWriter</code>
     * constructor and must implement <code>java.io.Serializable</code>.
     */
    private static final class ValuetypeWriter
            implements CDRStreamWriter {
        private Class clz;

        ValuetypeWriter(Class clz) {
            this.clz = clz;
        }

        public void write(OutputStream out, Object obj) {
            out.write_value((java.io.Serializable) obj, clz);
        }
    }

}
