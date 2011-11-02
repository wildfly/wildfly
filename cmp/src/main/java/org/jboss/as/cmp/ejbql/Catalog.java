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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.jboss.as.cmp.bridge.EntityBridge;

/**
 * This class maintains a map of all entities bridges in an application by name.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public final class Catalog {
    private final Map entityByAbstractSchemaName = new HashMap();
    private final Map entityByEJBName = new HashMap();
    private final Map entityByInterface = new HashMap();

    public void addEntity(EntityBridge entityBridge) {
        entityByAbstractSchemaName.put(
                entityBridge.getAbstractSchemaName(),
                entityBridge);
        entityByEJBName.put(
                entityBridge.getEntityName(),
                entityBridge);

        Class remote = entityBridge.getRemoteInterface();
        if (remote != null) {
            entityByInterface.put(remote, entityBridge);
        }
        Class local = entityBridge.getLocalInterface();
        if (local != null) {
            entityByInterface.put(local, entityBridge);
        }
    }

    public EntityBridge getEntityByAbstractSchemaName(
            String abstractSchemaName) {
        return (EntityBridge) entityByAbstractSchemaName.get(abstractSchemaName);
    }

    public EntityBridge getEntityByInterface(Class intf) {
        return (EntityBridge) entityByInterface.get(intf);
    }

    public EntityBridge getEntityByEJBName(String ejbName) {
        return (EntityBridge) entityByEJBName.get(ejbName);
    }

    public int getEntityCount() {
        return entityByEJBName.size();
    }

    public Set getEJBNames() {
        return Collections.unmodifiableSet(entityByEJBName.keySet());
    }
}
