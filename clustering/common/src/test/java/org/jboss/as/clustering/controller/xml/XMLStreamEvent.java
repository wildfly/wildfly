/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.xml;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;

/**
 * Encapsulates a simplified StAX event.
 * @author Paul Ferraro
 */
public interface XMLStreamEvent {

    int getType();

    default QName getName() {
        throw new IllegalStateException();
    }

    default List<Map.Entry<String, String>> getAttributes() {
        return List.of();
    }

    default String getText() {
        throw new IllegalStateException();
    }

    public static XMLStreamEvent start(QName name) {
        return start(name, List.of());
    }

    public static XMLStreamEvent start(QName name, List<Map.Entry<String, String>> attributes) {
        return new XMLStreamEvent() {
            @Override
            public int getType() {
                return XMLStreamConstants.START_ELEMENT;
            }

            @Override
            public QName getName() {
                return name;
            }

            @Override
            public List<Map.Entry<String, String>> getAttributes() {
                return attributes;
            }
        };
    }

    public static XMLStreamEvent content(String text) {
        return new XMLStreamEvent() {
            @Override
            public int getType() {
                return XMLStreamConstants.CHARACTERS;
            }

            @Override
            public String getText() {
                return text;
            }
        };
    }

    public static XMLStreamEvent end(QName name) {
        return new XMLStreamEvent() {
            @Override
            public int getType() {
                return XMLStreamConstants.END_ELEMENT;
            }

            @Override
            public QName getName() {
                return name;
            }
        };
    }
}