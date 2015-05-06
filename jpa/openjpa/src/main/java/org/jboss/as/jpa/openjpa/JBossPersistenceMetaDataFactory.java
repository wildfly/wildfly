/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.jpa.openjpa;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;

import org.apache.openjpa.persistence.PersistenceMetaDataFactory;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

/**
 * OpenJPA MetaDataFactory that uses the annotation index provided by PersistenceUnitMetadata
 * to search entity classes from the persistence unit.
 *
 * @author Antti Laisi
 */
public class JBossPersistenceMetaDataFactory extends PersistenceMetaDataFactory {

    private static final ThreadLocal<PersistenceUnitMetadata> PERSISTENCE_UNIT_METADATA = new ThreadLocal<PersistenceUnitMetadata>();
    /** Cache of the type names, used when restarting the persistence unit service */
    private static final Map<PersistenceUnitMetadata, Set<String>> CACHED_TYPENAMES = new HashMap<PersistenceUnitMetadata, Set<String>>();

    @Override
    protected Set<String> parsePersistentTypeNames(ClassLoader loader) {
        PersistenceUnitMetadata pu = PERSISTENCE_UNIT_METADATA.get();
        if (pu == null) {
            return Collections.emptySet();
        }

        return findPersistenceTypeNames(pu);
    }

    static void setThreadLocalPersistenceUnitMetadata(PersistenceUnitMetadata pu) {
        PERSISTENCE_UNIT_METADATA.set(pu);
    }

    static void clearThreadLocalPersistenceUnitMetadata() {
        PERSISTENCE_UNIT_METADATA.remove();
    }

    static void cleanup(PersistenceUnitMetadata pu) {
        synchronized (CACHED_TYPENAMES) {
            CACHED_TYPENAMES.remove(pu);
        }
    }

    private Set<String> findPersistenceTypeNames(PersistenceUnitMetadata pu) {
        synchronized (CACHED_TYPENAMES) {
            Set<String> typeNames = CACHED_TYPENAMES.get(pu);
            if (typeNames != null) {
                return typeNames;
            }
        }

        Set<String> persistenceTypeNames = new HashSet<String>();

        for (Map.Entry<URL, Index> entry : pu.getAnnotationIndex().entrySet()) {
            List<AnnotationInstance> instances = entry.getValue().getAnnotations(DotName.createSimple(Entity.class.getName()));
            for (AnnotationInstance instance : instances) {
                AnnotationTarget target = instance.target();
                if (target instanceof ClassInfo) {
                    ClassInfo classInfo = (ClassInfo) target;
                    persistenceTypeNames.add(classInfo.name().toString());
                }
            }
        }
        synchronized (CACHED_TYPENAMES) {
            CACHED_TYPENAMES.put(pu, persistenceTypeNames);
        }
        return persistenceTypeNames;
    }

}
