/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.weld.extensions.cdiportableextensions;

import java.util.HashSet;
import java.util.Set;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;

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
