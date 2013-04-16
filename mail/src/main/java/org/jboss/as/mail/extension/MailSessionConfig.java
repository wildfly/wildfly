/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.mail.extension;

import java.util.Arrays;

/**
 * @author <a href="tomaz.cerar@gmail.com">Tomaz Cerar</a>
 * @created 25.7.11 15:48
 */
public class MailSessionConfig {
    private String jndiName;
    private boolean debug = false;
    private String from = null;

    private ServerConfig smtpServer;
    private ServerConfig pop3Server;
    private ServerConfig imapServer;
    private CustomServerConfig[] customServers = new CustomServerConfig[0];


    MailSessionConfig() {
    }

    MailSessionConfig(String jndiName) {
        this.jndiName = jndiName;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getJndiName() {
        return jndiName;
    }

    public void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public ServerConfig getImapServer() {
        return imapServer;
    }

    public void setImapServer(ServerConfig imapServer) {
        this.imapServer = imapServer;
    }

    public ServerConfig getPop3Server() {
        return pop3Server;
    }

    public void setPop3Server(ServerConfig pop3Server) {
        this.pop3Server = pop3Server;
    }

    public ServerConfig getSmtpServer() {
        return smtpServer;
    }

    public void setSmtpServer(ServerConfig smtpServer) {
        this.smtpServer = smtpServer;
    }

    public CustomServerConfig[] getCustomServers() {
        return customServers;
    }

    public void setCustomServers(CustomServerConfig... customServer) {
        this.customServers = customServer;
    }

    public void addCustomServer(CustomServerConfig customServer) {
        final int i = customServers.length;
        customServers = Arrays.copyOf(customServers, i + 1);
        customServers[i] = customServer;
    }

    @Override
    public String toString() {
        return "MailSessionConfig{" +
                "imapServer=" + imapServer +
                ", jndiName='" + jndiName + '\'' +
                ", smtpServer=" + smtpServer +
                ", pop3Server=" + pop3Server +
                '}';
    }
}
