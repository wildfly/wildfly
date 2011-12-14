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
package org.jboss.as.cmp.jdbc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.ejb.FinderException;
import org.jboss.as.cmp.context.CmpEntityBeanContext;

/**
 * Common interface for all query commands.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public interface JDBCQueryCommand {
    Collection execute(Method finderMethod, Object[] args, CmpEntityBeanContext ctx, EntityProxyFactory factory) throws FinderException;

    JDBCStoreManager getSelectManager();

    interface EntityProxyFactory {
        Object getEntityObject(final Object primaryKey);

        class Util {
            static Collection<Object> getEntityCollection(final EntityProxyFactory factory, final Collection<Object> ids) {
                List<Object> result = new ArrayList<Object>();
                if (!ids.isEmpty()) {
                    for (Object id : ids) {
                        result.add(id != null ? factory.getEntityObject(id) : null);
                    }
                }
                return result;
            }
        }
    }
}
