/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.order;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public interface LifecycleListenerMBean {

    void mbeanEvent(String id, String event);

    List<Tuple> getAllEvents();

    final class Tuple implements Serializable {

        private static final long serialVersionUID = 1L;

        final String id;
        final String method;

        public Tuple(String id, String method) {
            this.id = id;
            this.method = method;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tuple tuple = (Tuple) o;
            return Objects.equals(id, tuple.id) && Objects.equals(method, tuple.method);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, method);
        }

        @Override
        public String toString() {
            return "Tuple{" +
                    "id='" + id + '\'' +
                    ", method='" + method + '\'' +
                    '}';
        }
    }
}
