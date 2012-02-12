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
package org.jboss.as.cmp.jdbc.metadata;

import java.lang.reflect.Method;

/**
 * This interface is used to identify a query that will be invoked in
 * responce to the invocation of a finder method in a home interface or
 * an ejbSelect method in a bean implementation class.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="sebastien.alborini@m4x.org">Sebastien Alborini</a>
 * @author <a href="danch@nvisia.com">danch</a>
 * @author <a href="on@ibis.odessa.ua">Oleg Nitz</a>
 * @version $Revision: 81030 $
 */
public interface JDBCQueryMetaData {
    /**
     * Gets the method which invokes this query.
     *
     * @return the NamedMethodMetaData representing the method on the object which invokes this query
     */
    Method getMethod();

    /**
     * Is the result set of ejbSelect is mapped to local ejb objects or
     * remote ejb objects.
     *
     * @return true, if the result set is to be local objects
     */
    boolean isResultTypeMappingLocal();

    /**
     * Gets the read ahead metadata for the query.
     *
     * @return the read ahead metadata for the query.
     */
    JDBCReadAheadMetaData getReadAhead();

    /**
     * @return EJBQL compiler implementation
     */
    Class<?> getQLCompilerClass();

    boolean isLazyResultSetLoading();


}
