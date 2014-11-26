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

import org.omg.CORBA.Any;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;


/**
 * Constant analysis.
 * <p/>
 * Routines here are conforming to the "Java(TM) Language to IDL Mapping
 * Specification", version 1.1 (01-06-07).
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 * @version $Revision: 81018 $
 */
public class ConstantAnalysis
        extends AbstractAnalysis {
    // Constants -----------------------------------------------------

    // Attributes ----------------------------------------------------

    // Static --------------------------------------------------------

    // Constructors --------------------------------------------------

    ConstantAnalysis(String javaName, Class type, Object value) {
        super(javaName);

        if (type == Void.TYPE ||
                !type.isPrimitive() && type != java.lang.String.class)
            throw IIOPLogger.ROOT_LOGGER.badConstantType(type.getName());

        this.type = type;
        this.value = value;
    }

    // Public --------------------------------------------------------

    /**
     * Return my Java type.
     */
    public Class getType() {
        return type;
    }

    /**
     * Return my value.
     */
    public Object getValue() {
        return value;
    }

    /**
     * Insert the constant value into the argument Any.
     */
    public void insertValue(Any any) {
        if (type == String.class)
            any.insert_wstring((String) value); // 1.3.5.10 Map to wstring
        else
            Util.insertAnyPrimitive(any, value);
    }

    // Protected -----------------------------------------------------

    // Private -------------------------------------------------------

    /**
     * Java type of constant.
     */
    private Class type;

    /**
     * The value of the constant.
     */
    private Object value;

}

