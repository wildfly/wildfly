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

import java.io.Externalizable;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.omg.CORBA.portable.IDLEntity;
import org.omg.CORBA.portable.ValueBase;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * Value analysis.
 * <p/>
 * Routines here are conforming to the "Java(TM) Language to IDL Mapping
 * Specification", version 1.1 (01-06-07).
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 */
public class ValueAnalysis extends ContainerAnalysis {

    private static WorkCacheManager cache = new WorkCacheManager(ValueAnalysis.class);

        /**
     * Analysis of our superclass, of null if our superclass is
     * java.lang.Object.
     */
    private ValueAnalysis superAnalysis;

    /**
     * Flags that this is an abstract value.
     */
    private boolean abstractValue = false;

    /**
     * Flags that this implements <code>java.io.Externalizable</code>.
     */
    private boolean externalizable = false;

    /**
     * Flags that this has a <code>writeObject()</code> method.
     */
    private boolean hasWriteObjectMethod = false;

    /**
     * The <code>serialPersistentFields of the value, or <code>null</code>
     * if the value does not have this field.
     */
    private ObjectStreamField[] serialPersistentFields;

    /**
     * The value members of this value class.
     */
    private ValueMemberAnalysis[] members;

    public static ValueAnalysis getValueAnalysis(Class cls)  throws RMIIIOPViolationException {
        return (ValueAnalysis) cache.getAnalysis(cls);
    }

    public static void clearCache(final ClassLoader classLoader) {
        cache.clearClassLoader(classLoader);
    }

    protected ValueAnalysis(final Class cls) {
        super(cls);
    }

    public String getIDLModuleName() {
        String result = super.getIDLModuleName();

        // Checked for boxedIDL 1.3.9
        Class clazz = getCls();
        if (IDLEntity.class.isAssignableFrom(clazz) && ValueBase.class.isAssignableFrom(clazz) == false)
            result = "::org::omg::boxedIDL" + result;
        return result;
    }

    protected void doAnalyze() throws RMIIIOPViolationException {
        super.doAnalyze();

        if (cls == String.class)
            throw IIOPLogger.ROOT_LOGGER.cannotAnalyzeStringType();

        if (cls == Class.class)
            throw IIOPLogger.ROOT_LOGGER.cannotAnalyzeClassType();

        if (Remote.class.isAssignableFrom(cls))
            throw IIOPLogger.ROOT_LOGGER.valueTypeCantImplementRemote(cls.getName(), "1.2.4");

        if (cls.getName().indexOf('$') != -1)
            throw IIOPLogger.ROOT_LOGGER.valueTypeCantBeProxy(cls.getName());

        externalizable = Externalizable.class.isAssignableFrom(cls);

        if (!externalizable) {
            // Look for serialPersistentFields field.
            Field spf = null;
            try {
                spf = cls.getField("serialPersistentFields");
            } catch (NoSuchFieldException ex) {
                // ignore
            }
            if (spf != null) { // Right modifiers?
                int mods = spf.getModifiers();
                if (!Modifier.isFinal(mods) || !Modifier.isStatic(mods) ||
                        !Modifier.isPrivate(mods))
                    spf = null; // wrong modifiers
            }
            if (spf != null) { // Right type?
                Class type = spf.getType();
                if (type.isArray()) {
                    type = type.getComponentType();
                    if (type != ObjectStreamField.class)
                        spf = null; // Array of wrong type
                } else
                    spf = null; // Wrong type: Not an array
            }
            if (spf != null) {
                // We have the serialPersistentFields field

                // Get this constant
                try {
                    serialPersistentFields = (ObjectStreamField[]) spf.get(null);
                } catch (IllegalAccessException ex) {
                    throw IIOPLogger.ROOT_LOGGER.unexpectedException(ex);
                }

                // Mark this in the fields array
                for (int i = 0; i < fields.length; ++i) {
                    if (fields[i] == spf) {
                        f_flags[i] |= F_SPFFIELD;
                        break;
                    }
                }
            }

            // Look for a writeObject Method
            Method wo = null;
            try {
                wo = cls.getMethod("writeObject", new Class[]{java.io.OutputStream[].class});
            } catch (NoSuchMethodException ex) {
                // ignore
            }
            if (wo != null) { // Right return type?
                if (wo.getReturnType() != Void.TYPE)
                    wo = null; // Wrong return type
            }
            if (wo != null) { // Right modifiers?
                int mods = spf.getModifiers();
                if (!Modifier.isPrivate(mods))
                    wo = null; // wrong modifiers
            }
            if (wo != null) { // Right arguments?
                Class[] paramTypes = wo.getParameterTypes();
                if (paramTypes.length != 1)
                    wo = null; // Bad number of parameters
                else if (paramTypes[0] != java.io.OutputStream.class)
                    wo = null; // Bad parameter type
            }
            if (wo != null) {
                // We have the writeObject() method.
                hasWriteObjectMethod = true;

                // Mark this in the methods array
                for (int i = 0; i < methods.length; ++i) {
                    if (methods[i] == wo) {
                        m_flags[i] |= M_WRITEOBJECT;
                        break;
                    }
                }
            }
        }

        // Map all fields not flagged constant or serialPersistentField.
        SortedSet m = new TreeSet(new ValueMemberComparator());

        for (int i = 0; i < fields.length; ++i) {
            if (f_flags[i] != 0)
                continue; // flagged

            int mods = fields[i].getModifiers();
            if (Modifier.isStatic(mods) || Modifier.isTransient(mods))
                continue; // don't map this

            ValueMemberAnalysis vma;
            vma = new ValueMemberAnalysis(fields[i].getName(),
                    fields[i].getType(),
                    Modifier.isPublic(mods));
            m.add(vma);
        }

        members = new ValueMemberAnalysis[m.size()];
        members = (ValueMemberAnalysis[]) m.toArray(members);

        // Get superclass analysis
        Class superClass = cls.getSuperclass();
        if (superClass == java.lang.Object.class)
            superClass = null;
        if (superClass == null)
            superAnalysis = null;
        else {
            superAnalysis = getValueAnalysis(superClass);
        }

        if (!Serializable.class.isAssignableFrom(cls))
            abstractValue = true;

        fixupCaseNames();
    }

    // Public --------------------------------------------------------

    /**
     * Returns the superclass analysis, or null if this inherits from
     * java.lang.Object.
     */
    public ValueAnalysis getSuperAnalysis() {
        return superAnalysis;
    }

    /**
     * Returns true if this value is abstract.
     */
    public boolean isAbstractValue() {
        return abstractValue;
    }

    /**
     * Returns true if this value is custom.
     */
    public boolean isCustom() {
        return externalizable || hasWriteObjectMethod;
    }

    /**
     * Returns true if this value implements java.io.Externalizable.
     */
    public boolean isExternalizable() {
        return externalizable;
    }

    /**
     * Return the value members of this value class.
     */
    public ValueMemberAnalysis[] getMembers() {
        return members.clone();
    }

    /**
     * Analyse attributes.
     * This will fill in the <code>attributes</code> array.
     * Here we override the implementation in ContainerAnalysis and create an
     * empty array, because for valuetypes we don't want to analyse IDL
     * attributes or operations (as in "rmic -idl -noValueMethods").
     */
    protected void analyzeAttributes()  throws RMIIIOPViolationException {
        attributes = new AttributeAnalysis[0];
    }

    /**
     * Return a list of all the entries contained here.
     *
     */
    protected ArrayList getContainedEntries() {
        final ArrayList ret = new ArrayList(constants.length +
                attributes.length +
                members.length);

        for (int i = 0; i < constants.length; ++i)
            ret.add(constants[i]);
        for (int i = 0; i < attributes.length; ++i)
            ret.add(attributes[i]);
        for (int i = 0; i < members.length; ++i)
            ret.add(members[i]);

        return ret;
    }


    /**
     * A <code>Comparator</code> for the field ordering specified at the
     * end of section 1.3.5.6.
     */
    private static class ValueMemberComparator implements Comparator {
        public int compare(final Object o1, final Object o2) {
            if (o1 == o2)
                return 0;

            final ValueMemberAnalysis m1 = (ValueMemberAnalysis) o1;
            final ValueMemberAnalysis m2 = (ValueMemberAnalysis) o2;

            final boolean p1 = m1.getCls().isPrimitive();
            final boolean p2 = m2.getCls().isPrimitive();

            if (p1 && !p2)
                return -1;
            if (!p1 && p2)
                return 1;

            return m1.getJavaName().compareTo(m2.getJavaName());
        }
    }
}
