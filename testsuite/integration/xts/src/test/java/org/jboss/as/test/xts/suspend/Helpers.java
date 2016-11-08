/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.xts.suspend;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class Helpers {

    public static ExecutorService getExecutorService(URL url) throws MalformedURLException {
        QName serviceName = new QName(ExecutorService.class.getPackage().getName(), ExecutorService.class.getSimpleName());
        URL wsdlUrl = new URL(url, ExecutorService.class.getSimpleName() + "?wsdl");
        Service service = Service.create(wsdlUrl, serviceName);
        return service.getPort(ExecutorService.class);
    }

    public static RemoteService getRemoteService(URL url) throws MalformedURLException {
        QName serviceName = new QName(RemoteService.class.getPackage().getName(), RemoteService.class.getSimpleName());
        URL wsdlUrl = new URL(url, RemoteService.class.getSimpleName() + "?wsdl");
        Service service = Service.create(wsdlUrl, serviceName);
        return service.getPort(RemoteService.class);
    }

}
