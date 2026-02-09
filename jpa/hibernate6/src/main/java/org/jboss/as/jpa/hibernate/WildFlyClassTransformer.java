/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.hibernate;

import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.jpa.internal.enhance.EnhancingClassTransformerImpl;

/**
 * WildFlyClassTransformer is simple wrapper of Hibernate ORM EnhancingClassTransformerImpl that provides
 * bytecode enhancing of application deployment classes (e.g. entity classes).
 *
 * @author Scott Marlow
 */
public class WildFlyClassTransformer extends EnhancingClassTransformerImpl {

    public WildFlyClassTransformer() {
        super(new DefaultEnhancementContext() {

            @Override
            public boolean doExtendedEnhancement(UnloadedClass classDescriptor) {
                // return false to disable extended enhancement so that non-entity classes are not enhanced.
                return false;
            }

        });
    }
}

