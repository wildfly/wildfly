package org.wildfly.clustering.spi;

import org.jboss.msc.service.ServiceName;

/**
 * Set of {@link ServiceName} factories for group-based services.
 * @author Paul Ferraro
 */
public enum GroupServiceName implements GroupServiceNameFactory {
    COMMAND_DISPATCHER() {
        @Override
        public ServiceName getServiceName(String group) {
            return BASE_SERVICE_NAME.append(this.toString(), group);
        }

        @Override
        public String toString() {
            return "dispatcher";
        }
    },
    NODE_FACTORY() {
        @Override
        public ServiceName getServiceName(String group) {
            return BASE_SERVICE_NAME.append(this.toString(), group);
        }

        @Override
        public String toString() {
            return "nodes";
        }
    },
    GROUP() {
        @Override
        public ServiceName getServiceName(String group) {
            return BASE_SERVICE_NAME.append(this.toString(), group);
        }

        @Override
        public String toString() {
            return "group";
        }
    },
}
