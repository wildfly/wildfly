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
package org.wildfly.clustering.web.sso;

/**
 * Uniquely identifies a web application.
 * @author Paul Ferraro
 */
public class WebApplication {
    private final String context;
    private final String host;

    public WebApplication(String context, String host) {
        this.context = context;
        this.host = host;
    }

    public String getContext() {
        return this.context;
    }

    public String getHost() {
        return this.host;
    }

    @Override
    public int hashCode() {
        return this.context.hashCode() ^ this.host.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if ((object == null) || !(object instanceof WebApplication)) return false;
        WebApplication application = (WebApplication) object;
        return this.context.equals(application.context) && this.host.equals(application.host);
    }

    @Override
    public String toString() {
        return String.format("%s:%s", this.host, this.context);
    }
}
