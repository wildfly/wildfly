/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.model;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class PropertiesUpdate extends AbstractModelUpdate<PropertiesElement> {

    private static final long serialVersionUID = 1198626319373912105L;

    private final Kind kind;
    private final String name;
    private final PropertyUpdate propertyUpdate;

    public PropertiesUpdate(final Kind kind, final String name, final PropertyUpdate propertyUpdate) {
        if (kind == null) {
            throw new IllegalArgumentException("kind is null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (kind != Kind.REMOVE && propertyUpdate == null) {
            throw new IllegalArgumentException("propertyUpdate is null");
        }
        this.kind = kind;
        this.name = name;
        this.propertyUpdate = propertyUpdate;
    }

    public enum Kind {
        ADD,
        CHANGE,
        REMOVE,
    }

    public Kind getKind() {
        return kind;
    }

    protected Class<PropertiesElement> getModelElementType() {
        return PropertiesElement.class;
    }

    protected AbstractModelUpdate<PropertiesElement> applyUpdate(final PropertiesElement element) {
        final String value = element.getProperty(name);
        switch (kind) {
            case ADD:
                try {
                    return new PropertiesUpdate(Kind.REMOVE, name, null);
                } finally {
                    element.addProperty(name, propertyUpdate.getValue());
                }
            case CHANGE:
                try {
                    return new PropertiesUpdate(Kind.CHANGE, name, new PropertyUpdate(value));
                } finally {
                    element.changeProperty(name, propertyUpdate.getValue());
                }
            case REMOVE:
                try {
                    return new PropertiesUpdate(Kind.ADD, name, new PropertyUpdate(value));
                } finally {
                    element.removeProperty(name);
                }
            default: throw new IllegalStateException();
        }
    }
}
