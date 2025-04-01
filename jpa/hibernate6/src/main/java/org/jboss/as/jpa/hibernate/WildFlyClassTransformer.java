package org.jboss.as.jpa.hibernate;

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

            public boolean isEntityClass(UnloadedClass classDescriptor) {
                return super.isEntityClass(classDescriptor);
            }

            public boolean isCompositeClass(UnloadedClass classDescriptor) {
                return super.isCompositeClass(classDescriptor);
            }

            public boolean doBiDirectionalAssociationManagement(UnloadedField field) {
                return super.doBiDirectionalAssociationManagement(field);
            }

            public boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
                return super.doDirtyCheckingInline(classDescriptor);
            }

            public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
                return super.hasLazyLoadableAttributes(classDescriptor);
            }

            public boolean isLazyLoadable(UnloadedField field) {
                return super.isLazyLoadable(field);
            }

            public boolean doExtendedEnhancement(UnloadedClass classDescriptor) {
                // doesn't make any sense to have extended enhancement enabled at runtime. we only enhance entities anyway.
                return false;
            }

        });
    }
}

