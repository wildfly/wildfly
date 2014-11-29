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


/**
 * Abstract base class for all analysis classes.
 * <p/>
 * Routines here are conforming to the "Java(TM) Language to IDL Mapping
 * Specification", version 1.1 (01-06-07).
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 * @version $Revision: 81018 $
 */
abstract class AbstractAnalysis {

    /**
     * My unqualified IDL name.
     */
    private String idlName;

    /**
     * My unqualified java name.
     */
    private final String javaName;

    AbstractAnalysis(String idlName, String javaName) {
        this.idlName = idlName;
        this.javaName = javaName;
    }

    AbstractAnalysis(String javaName) {
        this(Util.javaToIDLName(javaName), javaName);
    }

    /**
     * Return my unqualified IDL name.
     */
    public String getIDLName() {
        return idlName;
    }

    /**
     * Return my unqualified java name.
     */
    public String getJavaName() {
        return javaName;
    }

    /**
     * Set my unqualified IDL name.
     */
    void setIDLName(String idlName) {
        this.idlName = idlName;
    }

}

