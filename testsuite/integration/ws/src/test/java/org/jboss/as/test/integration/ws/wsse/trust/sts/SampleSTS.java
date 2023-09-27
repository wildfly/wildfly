/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.wsse.trust.sts;

import org.apache.cxf.annotations.EndpointProperties;
import org.apache.cxf.annotations.EndpointProperty;
import org.apache.cxf.interceptor.InInterceptors;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.operation.TokenIssueOperation;
import org.apache.cxf.sts.operation.TokenValidateOperation;
import org.apache.cxf.sts.service.ServiceMBean;
import org.apache.cxf.sts.service.StaticService;
import org.apache.cxf.sts.token.delegation.UsernameTokenDelegationHandler;
import org.apache.cxf.sts.token.provider.SAMLTokenProvider;
import org.apache.cxf.sts.token.validator.SAMLTokenValidator;
import org.apache.cxf.sts.token.validator.UsernameTokenValidator;
import org.apache.cxf.ws.security.sts.provider.SecurityTokenServiceProvider;
import org.jboss.as.test.integration.ws.wsse.trust.shared.WSTrustAppUtils;

import jakarta.xml.ws.WebServiceProvider;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@WebServiceProvider(serviceName = "SecurityTokenService",
        portName = "UT_Port",
        targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/",
        wsdlLocation = "WEB-INF/wsdl/ws-trust-1.4-service.wsdl")
//be sure to have dependency on org.apache.cxf module when on AS7, otherwise Apache CXF annotations are ignored
@EndpointProperties(value = {
        @EndpointProperty(key = "ws-security.signature.username", value = "mystskey"),
        @EndpointProperty(key = "ws-security.signature.properties", value = "stsKeystore.properties"),
        @EndpointProperty(key = "ws-security.callback-handler", value = "org.jboss.as.test.integration.ws.wsse.trust.sts.STSCallbackHandler"),
        @EndpointProperty(key = "ws-security.validate.token", value = "false") //to let the JAAS integration deal with validation through the interceptor below
})
@InInterceptors(interceptors = {"org.jboss.wsf.stack.cxf.security.authentication.SubjectCreatingPolicyInterceptor"})
public class SampleSTS extends SecurityTokenServiceProvider {
    public SampleSTS() throws Exception {
        super();

        StaticSTSProperties props = new StaticSTSProperties();
        props.setSignatureCryptoProperties("stsKeystore.properties");
        props.setSignatureUsername("mystskey");
        props.setCallbackHandlerClass(STSCallbackHandler.class.getName());
        props.setIssuer("DoubleItSTSIssuer");

        List<ServiceMBean> services = new LinkedList<ServiceMBean>();
        StaticService service = new StaticService();
        String serverHostRegexp = WSTrustAppUtils.getServerHost().replace("[", "\\[").replace("]", "\\]").replace("127.0.0.1", "localhost");
        service.setEndpoints(Arrays.asList(
                "http://" + serverHostRegexp + ":(\\d)*/jaxws-samples-wsse-policy-trust/SecurityService",
                "http://" + serverHostRegexp + ":(\\d)*/jaxws-samples-wsse-policy-trust-actas/ActAsService",
                "http://" + serverHostRegexp + ":(\\d)*/jaxws-samples-wsse-policy-trust-onbehalfof/OnBehalfOfService"
        ));
        services.add(service);

        TokenIssueOperation issueOperation = new TokenIssueOperation();
        issueOperation.setServices(services);
        issueOperation.getTokenProviders().add(new SAMLTokenProvider());
        // required for OnBehalfOf
        issueOperation.getTokenValidators().add(new UsernameTokenValidator());
        // added for OnBehalfOf and ActAs
        issueOperation.getDelegationHandlers().add(new UsernameTokenDelegationHandler());
        issueOperation.setStsProperties(props);

        TokenValidateOperation validateOperation = new TokenValidateOperation();
        validateOperation.getTokenValidators().add(new SAMLTokenValidator());
        validateOperation.setStsProperties(props);

        this.setIssueOperation(issueOperation);
        this.setValidateOperation(validateOperation);
    }
}
