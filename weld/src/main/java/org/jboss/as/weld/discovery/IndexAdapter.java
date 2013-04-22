/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.weld.discovery;

import java.util.Collection;
import java.util.List;

import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

abstract class IndexAdapter {

    public static IndexAdapter forIndex(final Index index) {
        return new IndexAdapter() {
            @Override
            public Collection<ClassInfo> getKnownDirectSubclasses(DotName className) {
                return index.getKnownDirectSubclasses(className);
            }

            @Override
            public List<AnnotationInstance> getAnnotations(DotName annotationName) {
                return index.getAnnotations(annotationName);
            }
        };
    }

    public static IndexAdapter forCompositeIndex(final CompositeIndex index) {
        return new IndexAdapter() {

            @Override
            public Collection<ClassInfo> getKnownDirectSubclasses(DotName className) {
                return index.getKnownDirectSubclasses(className);
            }

            @Override
            public List<AnnotationInstance> getAnnotations(DotName annotationName) {
                return index.getAnnotations(annotationName);
            }
        };
    }

    public abstract List<AnnotationInstance> getAnnotations(DotName annotationName);

    public abstract Collection<ClassInfo> getKnownDirectSubclasses(DotName className);
}
