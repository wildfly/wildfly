/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ws.wsse.trust.stsholderofkey;

import org.apache.cxf.annotations.EndpointProperties;
import org.apache.cxf.annotations.EndpointProperty;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.operation.TokenIssueOperation;
import org.apache.cxf.sts.service.ServiceMBean;
import org.apache.cxf.sts.service.StaticService;
import org.apache.cxf.sts.token.provider.SAMLTokenProvider;
import org.apache.cxf.ws.security.sts.provider.SecurityTokenServiceProvider;
import org.jboss.as.test.integration.ws.wsse.trust.shared.WSTrustAppUtils;

import javax.xml.ws.WebServiceProvider;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * User: rsearls
 * Date: 3/14/14
 */
@WebServiceProvider(serviceName = "SecurityTokenService",
        portName = "UT_Port",
        targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/",
        wsdlLocation = "WEB-INF/wsdl/holderofkey-ws-trust-1.4-service.wsdl")
//be sure to have dependency on org.apache.cxf module when on AS7, otherwise Apache CXF annotations are ignored
@EndpointProperties(value = {
        @EndpointProperty(key = "ws-security.signature.properties", value = "stsKeystore.properties"),
        @EndpointProperty(key = "ws-security.callback-handler", value = "org.jboss.as.test.integration.ws.wsse.trust.stsholderofkey.STSHolderOfKeyCallbackHandler")
})
public class SampleSTSHolderOfKey extends SecurityTokenServiceProvider {

    public SampleSTSHolderOfKey() throws Exception {
        super();

        StaticSTSProperties props = new StaticSTSProperties();
        props.setSignatureCryptoProperties("stsKeystore.properties");
        props.setSignatureUsername("mystskey");
        props.setCallbackHandlerClass(STSHolderOfKeyCallbackHandler.class.getName());
        props.setEncryptionCryptoProperties("stsKeystore.properties");
        props.setEncryptionUsername("myservicekey");
        props.setIssuer("DoubleItSTSIssuer");

        List<ServiceMBean> services = new LinkedList<ServiceMBean>();
        StaticService service = new StaticService();
        String serverHostRegexp = WSTrustAppUtils.getServerHost().replace("[", "\\[").replace("]", "\\]").replace("127.0.0.1", "localhost");
        service.setEndpoints(Arrays.asList(
                "https://" + serverHostRegexp + ":(\\d)*/jaxws-samples-wsse-policy-trust-holderofkey/HolderOfKeyService"
        ));

        services.add(service);

        TokenIssueOperation issueOperation = new TokenIssueOperation();
        issueOperation.getTokenProviders().add(new SAMLTokenProvider());
        issueOperation.setServices(services);
        issueOperation.setStsProperties(props);
        this.setIssueOperation(issueOperation);

    }
}
