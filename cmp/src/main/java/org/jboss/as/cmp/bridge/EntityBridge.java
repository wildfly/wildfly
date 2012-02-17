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

import java.util.List;

/**
 * EntityBridge follows the Bridge pattern [Gamma et. al, 1995].
 * In this implementation of the pattern the Abstract is the entity bean class,
 * and the RefinedAbstraction is the entity bean dynamic proxy. This interface
 * can be considered the implementor. Each implementation of the CMPStoreManager
 * should create a store specific implementation of the bridge.
 * <p/>
 * Life-cycle:
 * Undefined. Should be tied to CMPStoreManager.
 * <p/>
 * Multiplicity:
 * One per cmp entity bean type.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public interface EntityBridge {
    String getEntityName();

    String getAbstractSchemaName();

    FieldBridge getFieldByName(String fieldName);

    Class<?> getRemoteInterface();

    Class<?> getLocalInterface();

    List<FieldBridge> getFields();
}
