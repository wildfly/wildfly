/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.jmx.adaptor.model;

import javax.management.MBeanInfo;
import javax.management.ObjectName;

/**
 * An mbean ObjectName and MBeanInfo pair that is ordered by ObjectName.
 *
 * @author Scott.Stark@jboss.org
 * @author Dimitris.Andreadis@jboss.org
 */
public class MBeanData implements Comparable<MBeanData> {
    private ObjectName objectName;
    private MBeanInfo metaData;

    public MBeanData() {
    }

    /** Creates a new instance of MBeanInfo */
    public MBeanData(ObjectName objectName, MBeanInfo metaData) {
        this.objectName = objectName;
        this.metaData = metaData;
    }

    /**
     * Getter for property objectName.
     *
     * @return Value of property objectName.
     */
    public ObjectName getObjectName() {
        return objectName;
    }

    /**
     * Setter for property objectName.
     *
     * @param objectName New value of property objectName.
     */
    public void setObjectName(ObjectName objectName) {
        this.objectName = objectName;
    }

    /**
     * Getter for property metaData.
     *
     * @return Value of property metaData.
     */
    public MBeanInfo getMetaData() {
        return metaData;
    }

    /**
     * Setter for property metaData.
     *
     * @param metaData New value of property metaData.
     */
    public void setMetaData(MBeanInfo metaData) {
        this.metaData = metaData;
    }

    /**
     * @return The ObjectName.toString()
     */
    public String getName() {
        return objectName.toString();
    }

    /**
     * @return The canonical key properties string
     */
    public String getNameProperties() {
        return objectName.getCanonicalKeyPropertyListString();
    }

    /**
     * @return The MBeanInfo.getClassName() value
     */
    public String getClassName() {
        return metaData.getClassName();
    }

    /**
     * Compares MBeanData based on the ObjectName domain name and canonical key properties
     *
     * @param the MBeanData to compare against
     * @return < 0 if this is less than o, > 0 if this is greater than o, 0 if equal.
     */
    public int compareTo(MBeanData md) {
        String d1 = objectName.getDomain();
        String d2 = md.objectName.getDomain();
        int compare = d1.compareTo(d2);
        if (compare == 0) {
            String p1 = objectName.getCanonicalKeyPropertyListString();
            String p2 = md.objectName.getCanonicalKeyPropertyListString();
            compare = p1.compareTo(p2);
        }
        return compare;
    }

    public boolean equals(MBeanData o) {
        if (o == null)
            return false;
        if (this == o)
            return true;
        return (this.compareTo(o) == 0);
    }

    public boolean equals(Object o) {
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((objectName == null) ? 0 : objectName.hashCode());
        return result;
    }

}
