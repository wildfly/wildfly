/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.xts.suspend;

import javax.xml.namespace.QName;
import jakarta.xml.ws.Service;
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
