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

import java.lang.reflect.Method;
import java.rmi.Remote;
import java.util.Locale;

import org.omg.CORBA.AttributeMode;

/**
 * Attribute analysis.
 * <p/>
 * Routines here are conforming to the "Java(TM) Language to IDL Mapping
 * Specification", version 1.1 (01-06-07).
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 * @version $Revision: 81018 $
 */
public class AttributeAnalysis extends AbstractAnalysis {

    /**
     * Attribute mode.
     */
    private AttributeMode mode;

    /**
     * Java type.
     */
    private Class cls;

    /**
     * Accessor Method.
     */
    private Method accessor = null;

    /**
     * Mutator Method.
     * This is null for read-only attributes.
     */
    private Method mutator = null;

    /**
     * Accessor method analysis.
     */
    private OperationAnalysis accessorAnalysis = null;

    /**
     * Mutator method analysis.
     * This is null for read-only attributes.
     */
    private OperationAnalysis mutatorAnalysis = null;

    /**
     * Create an attribute analysis.
     */
    private AttributeAnalysis(String javaName, AttributeMode mode, Method accessor, Method mutator)
            throws RMIIIOPViolationException {
        super(Util.javaToIDLName(javaName), javaName);

        this.mode = mode;
        this.cls = accessor.getReturnType();
        this.accessor = accessor;
        this.mutator = mutator;

        // Only do operation analysis if the attribute is in a remote interface.
        if (accessor.getDeclaringClass().isInterface() && Remote.class.isAssignableFrom(accessor.getDeclaringClass())) {
            accessorAnalysis = new OperationAnalysis(accessor);
            if (mutator != null) {
                mutatorAnalysis = new OperationAnalysis(mutator);
            }

            setIDLName(getIDLName()); // Fix operation names
        }
    }


    /**
     * Create an attribute analysis for a read-only attribute.
     */
    AttributeAnalysis(String javaName, Method accessor)
            throws RMIIIOPViolationException {
        this(javaName, AttributeMode.ATTR_READONLY, accessor, null);
    }

    /**
     * Create an attribute analysis for a read-write attribute.
     */
    AttributeAnalysis(String javaName, Method accessor, Method mutator)
            throws RMIIIOPViolationException {
        this(javaName, AttributeMode.ATTR_NORMAL, accessor, mutator);
    }

    /**
     * Return my attribute mode.
     */
    public AttributeMode getMode() {
        return mode;
    }

    /**
     * Return my Java type.
     */
    public Class getCls() {
        return cls;
    }

    /**
     * Return my accessor method
     */
    public Method getAccessor() {
        return accessor;
    }

    /**
     * Return my mutator method
     */
    public Method getMutator() {
        return mutator;
    }

    /**
     * Return my accessor operation analysis
     */
    public OperationAnalysis getAccessorAnalysis() {
        return accessorAnalysis;
    }

    /**
     * Return my mutator operation analysis
     */
    public OperationAnalysis getMutatorAnalysis() {
        return mutatorAnalysis;
    }

    /**
     * Set my unqualified IDL name.
     * This also sets the names of the associated operations.
     */
    void setIDLName(String idlName) {
        super.setIDLName(idlName);

        // If the first char is an uppercase letter and the second char is not
        // an uppercase letter, then convert the first char to lowercase.
        if (idlName.charAt(0) >= 0x41 && idlName.charAt(0) <= 0x5a
                && (idlName.length() <= 1
                || idlName.charAt(1) < 0x41 || idlName.charAt(1) > 0x5a)) {
            idlName =
                    idlName.substring(0, 1).toLowerCase(Locale.ENGLISH) + idlName.substring(1);
        }

        if (accessorAnalysis != null)
            accessorAnalysis.setIDLName("_get_" + idlName);
        if (mutatorAnalysis != null)
            mutatorAnalysis.setIDLName("_set_" + idlName);
    }


}

