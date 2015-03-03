/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;

/**
 * Enumerates the supported cache store types.
 * @author Paul Ferraro
 */
public enum StoreType {
    FILE(ModelKeys.FILE_STORE, ModelKeys.FILE_STORE_NAME, new FileStoreAddHandler(), ReloadRequiredRemoveStepHandler.INSTANCE),
    REMOTE(ModelKeys.REMOTE_STORE, ModelKeys.REMOTE_STORE_NAME, new RemoteStoreAddHandler(), ReloadRequiredRemoveStepHandler.INSTANCE),
    STRING_KEYED_JDBC(ModelKeys.STRING_KEYED_JDBC_STORE, ModelKeys.STRING_KEYED_JDBC_STORE_NAME, new StringKeyedJDBCStoreAddHandler(), ReloadRequiredRemoveStepHandler.INSTANCE),
    BINARY_KEYED_JDBC(ModelKeys.BINARY_KEYED_JDBC_STORE, ModelKeys.BINARY_KEYED_JDBC_STORE_NAME, new BinaryKeyedJDBCStoreAddHandler(), ReloadRequiredRemoveStepHandler.INSTANCE),
    MIXED_KEYED_JDBC(ModelKeys.MIXED_KEYED_JDBC_STORE, ModelKeys.MIXED_KEYED_JDBC_STORE_NAME, new MixedKeyedJDBCStoreAddHandler(), ReloadRequiredRemoveStepHandler.INSTANCE),
    CUSTOM(ModelKeys.STORE, ModelKeys.STORE_NAME, new CustomStoreAddHandler(), ReloadRequiredRemoveStepHandler.INSTANCE),
    ;

    private static final Map<String, StoreType> TYPES = new HashMap<>();
    static {
        for (StoreType type: values()) {
            TYPES.put(type.path.getKey(), type);
        }
    }

    static StoreType forName(String key) {
        return TYPES.get(key);
    }

    private final PathElement path;
    private final StoreAddHandler addHandler;
    private final AbstractRemoveStepHandler removeHandler;

    private StoreType(String key, String value, StoreAddHandler addHandler, AbstractRemoveStepHandler removeHandler) {
        this.path = PathElement.pathElement(key, value);
        this.addHandler = addHandler;
        this.removeHandler = removeHandler;
    }

    public ResourceDescriptionResolver getResourceDescriptionResolver() {
        return new InfinispanResourceDescriptionResolver(this.path.getKey());
    }

    public PathElement pathElement() {
        return this.path;
    }

    public StoreAddHandler getAddHandler() {
        return this.addHandler;
    }

    public AbstractRemoveStepHandler getRemoveHandler() {
        return this.removeHandler;
    }
}
