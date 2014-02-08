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
package org.wildfly.clustering.web.infinispan.session.coarse;

import java.util.Map;

import org.jboss.as.clustering.infinispan.invoker.Mutator;
import org.jboss.as.clustering.marshalling.MarshalledValue;
import org.jboss.as.clustering.marshalling.MarshallingContext;
import org.wildfly.clustering.web.infinispan.session.MutableDetector;
import org.wildfly.clustering.web.infinispan.session.SessionAttributeMarshaller;
import org.wildfly.clustering.web.session.SessionAttributes;

/**
 * Exposes session attributes for a coarse granularity session.
 * @author Paul Ferraro
 */
public class CoarseSessionAttributes extends CoarseImmutableSessionAttributes implements SessionAttributes {
    private final Mutator mutator;

    public CoarseSessionAttributes(MarshalledValue<Map<String, Object>, MarshallingContext> attributes, SessionAttributeMarshaller<Map<String, Object>, MarshalledValue<Map<String, Object>, MarshallingContext>> marshaller, Mutator mutator) {
        super(attributes, marshaller);
        this.mutator = mutator;
    }

    @Override
    public Object removeAttribute(String name) {
        Object value = this.getAttributes().remove(name);
        this.mutator.mutate();
        return value;
    }

    @Override
    public Object setAttribute(String name, Object value) {
        Map<String, Object> attributes = this.getAttributes();
        Object old = (value != null) ? attributes.put(name, value) : attributes.remove(name);
        this.mutator.mutate();
        return old;
    }

    @Override
    public Object getAttribute(String name) {
        Object value = super.getAttribute(name);
        if (MutableDetector.isMutable(value)) {
            this.mutator.mutate();
        }
        return value;
    }
}
