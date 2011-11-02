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
package org.jboss.as.cmp.bridge;

import org.jboss.as.cmp.context.CmpEntityBeanContext;

/**
 * FieldBridge represents one field for one entity.
 * <p/>
 * Life-cycle:
 * Tied to the EntityBridge.
 * <p/>
 * Multiplicity:
 * One for each entity bean field.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:alex@jboss.org">Alex Loubyansky</a>
 * @version $Revision: 81030 $
 */
public interface FieldBridge {
    /**
     * Gets the name of this field.
     *
     * @return the name of this field
     */
    String getFieldName();

    /**
     * Gets the value of this field for the specified instance context.
     *
     * @param ctx the context for which this field's value should be fetched
     * @return the value of this field
     */
    Object getValue(CmpEntityBeanContext ctx);

    /**
     * Sets the value of this field for the specified instance context.
     *
     * @param ctx   the context for which this field's value should be set
     * @param value the new value of this field
     */
    void setValue(CmpEntityBeanContext ctx, Object value);
}
