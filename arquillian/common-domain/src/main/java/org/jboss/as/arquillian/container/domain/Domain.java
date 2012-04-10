/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
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
package org.jboss.as.arquillian.container.domain;

import java.util.HashSet;
import java.util.Set;

/**
 * Simple Object that holds a flat version of the Domain
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class Domain {

    private Set<Server> servers = new HashSet<Server>();
    private Set<ServerGroup> serverGroups = new HashSet<ServerGroup>();

    public void addServer(Server server) {
        servers.add(server);
    }

    public void addServerGroup(ServerGroup group) {
        serverGroups.add(group);
    }

    public Set<Server> getServers() {
        return servers;
    }

    public Set<ServerGroup> getServerGroups() {
        return serverGroups;
    }

    public Set<String> getHosts() {
        Set<String> unique = new HashSet<String>();

        for (Server server : servers) {
            unique.add(server.host);
        }
        return unique;
    }

    public Set<Server> getServersInGroup(ServerGroup group) {
        return getServersInGroup(group.getName());
    }

    public Set<Server> getServersInGroup(String group) {
        Set<Server> unique = new HashSet<Domain.Server>();

        for (Server server : servers) {
            if (group.equals(server.group)) {
                unique.add(server);
            }
        }
        return unique;
    }

    public Set<Server> getAutoStartServers() {
        Set<Server> auto = new HashSet<Domain.Server>();

        for (Server server : servers) {
            if (server.autostart) {
                auto.add(server);
            }
        }
        return auto;
    }

    public static class ServerGroup {
        private String name;
        private String containerName;

        public ServerGroup(String name) {
            if (name == null) {
                throw new IllegalArgumentException("Name must be specified");
            }
            this.name = name;
        }

        /**
         * @return the Group Name as known to the Domain Controller
         */
        public String getName() {
            return name;
        }

        /**
         * @return the Container name as known to Arquillian
         */
        public String getContainerName() {
            if (containerName != null) {
                return containerName;
            }
            return getName();
        }

        public void setContainerName(String containerName) {
            this.containerName = containerName;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ServerGroup other = (ServerGroup) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "ServerGroup [name=" + name + ", containerName=" + getContainerName() + "]";
        }
    }

    public static class Server {
        private String name;
        private String containerName;
        private String host;
        private String group;
        private boolean autostart = false;

        public Server(String name, String host, String group, boolean autostart) {
            if (name == null || host == null) {
                throw new IllegalArgumentException("Server name and host can not be null. name[" + name + "], host[" + host+ "]");
            }
            this.name = name;
            this.host = host;
            this.group = group;
            this.autostart = autostart;
        }

        /**
         * @return The server group this server belongs to
         */
        public String getGroup() {
            return group;
        }

        /**
         * @return The host of this server
         */
        public String getHost() {
            return host;
        }

        /**
         * @return Server name as known to the Domain Controller
         */
        public String getName() {
            return name;
        }

        public String getUniqueName() {
            return host + ":" + name;
        }

        /**
         * @return Server/Container name as known to Arquillian
         */
        public String getContainerName() {
            if (containerName != null) {
                return containerName;
            }
            return getUniqueName();
        }

        public void setContainerName(String containerName) {
            this.containerName = containerName;
        }

        /**
         * @return Is this server marked to autostart during Domain Controller startup
         */
        public boolean isAutostart() {
            return autostart;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((group == null) ? 0 : group.hashCode());
            result = prime * result + ((host == null) ? 0 : host.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Server other = (Server) obj;
            if (group == null) {
                if (other.group != null)
                    return false;
            } else if (!group.equals(other.group))
                return false;
            if (host == null) {
                if (other.host != null)
                    return false;
            } else if (!host.equals(other.host))
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "[host=" + host + ", name=" + name + ", group=" + group + ", containerName=" + getContainerName() + "]";
        }
    }
}
