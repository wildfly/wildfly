/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.persistence.jipijapa.hibernate7;

import jakarta.persistence.metamodel.Type;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.bytecode.enhance.spi.UnloadedField;
import org.hibernate.jpa.internal.enhance.EnhancingClassTransformerImpl;

/**
 * WildFlyClassTransformer is simple wrapper of Hibernate ORM EnhancingClassTransformerImpl
 *
 * @author Scott Marlow
 */
public class WildFlyClassTransformer extends EnhancingClassTransformerImpl {

    public WildFlyClassTransformer() {
        super(new DefaultEnhancementContext() {

            @Override
            public boolean isEntityClass(UnloadedClass classDescriptor) {
                return super.isEntityClass(classDescriptor);
            }

            @Override
            public boolean isCompositeClass(UnloadedClass classDescriptor) {
                return super.isCompositeClass(classDescriptor);
            }

            @Override
            public boolean isMappedSuperclassClass(UnloadedClass classDescriptor) {
                return super.isMappedSuperclassClass(classDescriptor);
            }

            @Override
            public boolean doBiDirectionalAssociationManagement(UnloadedField field) {
                return super.doBiDirectionalAssociationManagement(field);
            }

            @Override
            public boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
                return super.doDirtyCheckingInline(classDescriptor);
            }

            @Override
            public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
                return super.hasLazyLoadableAttributes(classDescriptor);
            }

            @Override
            public boolean isLazyLoadable(UnloadedField field) {
                return super.isLazyLoadable(field);
            }

            @Override
            public boolean isPersistentField(UnloadedField ctField) {
                return super.isPersistentField(ctField);
            }

            @Override
            public boolean isMappedCollection(UnloadedField field) {
                return super.isMappedCollection(field);
            }

            @Override
            public UnloadedField[] order(UnloadedField[] persistentFields) {
                return super.order(persistentFields);
            }

            @Override
            public boolean isDiscoveredType(UnloadedClass classDescriptor) {
                return super.isDiscoveredType(classDescriptor);
            }

            @Override
            public void registerDiscoveredType(UnloadedClass classDescriptor, Type.PersistenceType type) {
                super.registerDiscoveredType(classDescriptor, type);
            }

            @Override
            public boolean doExtendedEnhancement(UnloadedClass classDescriptor) {
                // doesn't make any sense to have extended enhancement enabled at runtime. we only enhance entities anyway.
                return false;
            }

        });
    }
}

