package org.wildfly.clustering.spi;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.GroupServiceNameFactory;

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
    ;

    public static final String BASE_NAME = "clustering";
    static final ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append(BASE_NAME);
}
