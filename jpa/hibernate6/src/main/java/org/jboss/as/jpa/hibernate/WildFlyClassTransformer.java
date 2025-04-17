package org.jboss.as.jpa.hibernate;

import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.bytecode.enhance.spi.UnloadedField;
import org.hibernate.jpa.internal.enhance.EnhancingClassTransformerImpl;

/**
 * WildFlyClassTransformer
 *
 * @author Scott Marlow
 */
public class WildFlyClassTransformer extends EnhancingClassTransformerImpl {

    public WildFlyClassTransformer() {
        super(new DefaultEnhancementContext() {

            public boolean isEntityClass(UnloadedClass classDescriptor) {
                return super.isEntityClass(classDescriptor);
                // return managedResources.getAnnotatedClassNames().contains( classDescriptor.getName() )
                // && super.isEntityClass( classDescriptor );

            }

            public boolean isCompositeClass(UnloadedClass classDescriptor) {
                return super.isCompositeClass(classDescriptor);
                // return managedResources.getAnnotatedClassNames().contains( classDescriptor.getName() )
                // && super.isCompositeClass( classDescriptor );
            }

            public boolean doBiDirectionalAssociationManagement(UnloadedField field) {
                return super.doBiDirectionalAssociationManagement(field);
                // return associationManagementEnabled;
            }

            public boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
                return super.doDirtyCheckingInline(classDescriptor);
                // return dirtyTrackingEnabled;
            }

            public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
                return super.hasLazyLoadableAttributes(classDescriptor);
                // return lazyInitializationEnabled;
            }

            public boolean isLazyLoadable(UnloadedField field) {
                return super.isLazyLoadable(field);
                // return lazyInitializationEnabled;
            }

            public boolean doExtendedEnhancement(UnloadedClass classDescriptor) {
                return super.doExtendedEnhancement(classDescriptor);
                // doesn't make any sense to have extended enhancement enabled at runtime. we only enhance entities anyway.

            }

        });
    }
}

