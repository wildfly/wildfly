/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ws.serviceref;

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.xml.ws.WebServiceRef;

/**
 * A test bean that delegates to a web service provided through serviceref injection.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@Stateless(name = "StatelessBean")
@Remote(StatelessRemote.class)
public class StatelessBean implements StatelessRemote {

    @Inject
    private CdiBean cdiBean;

    @WebServiceRef(value = EndpointService.class, mappedName = "jbossws-client/service/TestService", wsdlLocation = "META-INF/wsdl/TestService.wsdl")
    EndpointInterface endpoint1;

    EndpointInterface _endpoint2;

    @WebServiceRef(value = EndpointService.class, mappedName = "jbossws-client/service/TestService", wsdlLocation = "META-INF/wsdl/TestService.wsdl")
    public void setEndpoint2(final EndpointInterface endpoint2) {
        this._endpoint2 = endpoint2;
    }

    // via XML
    EndpointInterface endpoint3;

    // via XML
    EndpointInterface _endpoint4;

    @Resource
    private String id;

    public void setEndpoint4(final EndpointInterface endpoint4) {
        this._endpoint4 = endpoint4;
    }

    public String echo1(final String string) throws Exception {
        if (null == endpoint1) { throw new IllegalArgumentException("Serviceref for property 'endpoint1' not injected"); }

        return endpoint1.echo(id + ":" + string);
    }

    public String echo2(final String string) throws Exception {
        if (null == _endpoint2) { throw new IllegalArgumentException("Serviceref for property 'endpoint2' not injected"); }

        return _endpoint2.echo(id + ":" + string);
    }

    public String echo3(final String string) throws Exception {
        if (null == endpoint3) { throw new IllegalArgumentException("Serviceref for property 'endpoint3' not injected"); }

        return endpoint3.echo(id + ":" + string);
    }

    public String echo4(final String string) throws Exception {
        if (null == _endpoint4) { throw new IllegalArgumentException("Serviceref for property 'endpoint4' not injected"); }

        return _endpoint4.echo(id + ":" + string);
    }

    public String echoCDI(final String string) throws Exception {
        return cdiBean.echo(id + ":" + string);
    }
}
