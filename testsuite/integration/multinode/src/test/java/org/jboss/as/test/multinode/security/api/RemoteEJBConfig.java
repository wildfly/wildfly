/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.multinode.security.api;

import java.io.Serializable;

import javax.servlet.ServletRequest;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.jboss.as.test.multinode.security.api.ServerConfigs.ServerConfig;
import org.jboss.as.test.multinode.security.api.TestConfig.Credentials;

/**
 * @author bmaxwell
 *
 */
@XmlRootElement(name="remote-ejb-config")
@XmlAccessorType(XmlAccessType.FIELD)
public class RemoteEJBConfig implements Serializable {

    @XmlAttribute(name="host")
    private String host = "localhost";
    @XmlAttribute(name="port")
    private Integer port = 8080;
    @XmlAttribute(name="username")
    private String username;
    @XmlAttribute(name="password")
    private String password;
    @XmlAttribute(name="remote")
    private boolean remote = true;
    @XmlAttribute(name="jboss-ejb-client-xml")
    private boolean jbossEjbClientXml = false;

    public RemoteEJBConfig() {
    }

    public RemoteEJBConfig(ServerConfig remoteServerConfig, Credentials credentials) {
        this(remoteServerConfig.getHost(), remoteServerConfig.getRemotingPort(), credentials.getUsername(), credentials.getPassword());
    }

    public RemoteEJBConfig(String host, Integer port, String username, String password) {
        if(host != null)
            this.host = host;
        if(port != null)
            this.port = port;
        if(username != null)
            this.username = username;
        if(password != null)
            this.password = password;
    }

    public RemoteEJBConfig(ServletRequest request) {
        String value = request.getParameter("ejbRemote");
        if(value != null)
            remote = true;
        value = request.getParameter("jbossEjbClientXml");
        if(value != null)
            jbossEjbClientXml = true;
        value = request.getParameter("ejbRemoteHost");
        if(value != null)
            host = value;
        value = request.getParameter("ejbRemotePort");
        if(value != null)
            port = Integer.parseInt(value);
        value = request.getParameter("ejbRemoteUsername");
        if(value != null)
            username = value;
        value = request.getParameter("ejbRemotePassword");
        if(value != null)
            password = value;
    }

    public boolean isJbossEjbClientXml() {
        return jbossEjbClientXml;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isRemote() {
        return remote;
    }

    public String getURLParams() {
        StringBuilder sb = new StringBuilder();
        sb.append("ejbRemote=true&");
        if(host != null)
            sb.append("ejbRemoteHost=" + host + "&");
        if(port != null)
            sb.append("ejbRemotePort=" + port + "&");
        if(username != null)
            sb.append("ejbRemoteUsername=" + username + "&");
        if(password != null)
            sb.append("ejbRemotePassword=" + password + "&");
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("remote=%s host=%s port=%d user=%s pass=%s jbossEjbClientXml=%s", remote, host, port, username, password, jbossEjbClientXml);
    }
}