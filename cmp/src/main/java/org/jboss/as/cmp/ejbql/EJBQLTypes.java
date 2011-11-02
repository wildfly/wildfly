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
package org.jboss.as.cmp.ejbql;

import java.util.Date;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;

/**
 * This class contains a list of the reconized EJB-QL types.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version $Revision: 81030 $
 */
public final class EJBQLTypes {
    public static final int UNKNOWN_TYPE = -1;
    public static final int NUMERIC_TYPE = 1;
    public static final int STRING_TYPE = 2;
    public static final int DATETIME_TYPE = 3;
    public static final int BOOLEAN_TYPE = 4;
    public static final int ENTITY_TYPE = 5;
    public static final int VALUE_CLASS_TYPE = 6;

    public static int getEJBQLType(Class type) {
        int result;
        if (type == Boolean.class || type == Boolean.TYPE) {
            result = BOOLEAN_TYPE;
        } else if (type.isPrimitive()
                || type == Character.class
                || Number.class.isAssignableFrom(type)) {
            result = NUMERIC_TYPE;
        } else if (type == String.class) {
            result = STRING_TYPE;
        } else if (Date.class.isAssignableFrom(type)) {
            result = DATETIME_TYPE;
        } else if (EJBObject.class.isAssignableFrom(type) ||
                EJBLocalObject.class.isAssignableFrom(type)) {
            result = ENTITY_TYPE;
        } else {
            result = VALUE_CLASS_TYPE;
        }
        return result;
    }
}
