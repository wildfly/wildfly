package org.wildfly.clustering.spi;

import org.jboss.msc.service.ServiceName;

public enum ChannelServiceNames implements ChannelServiceNameFactory {
    COMMAND_DISPATCHER() {
        @Override
        public ServiceName getServiceName(String group) {
            return BASE_SERVICE_NAME.append("dispatcher", group);
        }
    },
    NODE_FACTORY() {
        @Override
        public ServiceName getServiceName(String group) {
            return BASE_SERVICE_NAME.append("nodes", group);
        }
    },
    GROUP() {
        @Override
        public ServiceName getServiceName(String group) {
            return BASE_SERVICE_NAME.append("group", group);
        }
    },
}
