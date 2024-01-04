/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.extensions.cdiportableextensions;

import java.util.HashSet;
import java.util.Set;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.ProcessBean;

public class FunExtension implements Extension {

    Set<Class<?>> funnyClasses = new HashSet<Class<?>>();
    Set<Bean<?>> funnyBeans = new HashSet<Bean<?>>();

    public Set<Bean<?>> getFunnyBeans() {
        return funnyBeans;
    }

    public void processAnnotatedType(@Observes ProcessAnnotatedType<?> pat) {

        //System.out.println("FunExtension processAnnotatedType " + pat.getAnnotatedType().toString());

        if (pat.getAnnotatedType().getAnnotation(Funny.class) != null) {
            //System.out.println("FunExtension adding funny class " + pat.getAnnotatedType().getJavaClass());
            funnyClasses.add(pat.getAnnotatedType().getJavaClass());
        }
    }

    public void processBean(@Observes ProcessBean<?> pb) {
        //System.out.println("FunExtension processBean " + pb.getBean().getBeanClass());

        if (funnyClasses.contains(pb.getBean().getBeanClass())) {
            funnyBeans.add(pb.getBean());
        }
    }
}
