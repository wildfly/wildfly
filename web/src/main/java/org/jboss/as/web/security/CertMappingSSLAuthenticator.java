/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.jboss.as.web.security;


import java.io.IOException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.Request;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.authenticator.SSLAuthenticator;

import org.jboss.security.auth.certs.SubjectDNMapping;
import org.jboss.security.CertificatePrincipal;
import org.jboss.as.web.WebLogger;



/**
 * An <b>Authenticator</b> and <b>Valve</b> implementation of authentication
 * that utilizes SSL certificates to identify client users.
 *
 * This <b>Authenticator</b> allows for the use of a custom principal mapper.
 * class.  The custom principal mapper class can be configured using the
 * setCertificatePrincipalMapper() method.
 *
 * @author Derek Horton
 * @version $Revision$ $Date$
 */

public class CertMappingSSLAuthenticator extends SSLAuthenticator {

    protected CertificatePrincipal certMapping = new SubjectDNMapping();

    // ------------------------------------------------------------- Properties

    /**
     * Descriptive information about this implementation.
     */
    protected static final String info =
        "org.apache.catalina.authenticator.CertMappingSSLAuthenticator/1.0";


    /**
     * Return descriptive information about this Valve implementation.
     */
    public String getInfo() {
        return (info);
    }

    public void setCertificatePrincipalMapper(String className) {
      try {
         ClassLoader loader = Thread.currentThread().getContextClassLoader();
         Class<?> cpClass = loader.loadClass(className);
         certMapping = (CertificatePrincipal) cpClass.newInstance();
      }
      catch (Exception e) {
         WebLogger.WEB_SECURITY_LOGGER.tracef("Failed to load CertificatePrincipal mapper class: " + className, e);
         certMapping = new SubjectDNMapping();
      }
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Authenticate the user by checking for the existence of a certificate
     * chain, and optionally asking a trust manager to validate that we trust
     * this user.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param config    Login configuration describing how authentication
     *              should be performed
     *
     * @exception IOException if an input/output error occurs
     */
    public boolean authenticate(Request request,
                                HttpServletResponse response,
                                LoginConfig config)
        throws IOException {

        System.out.println("inside CertMappingSSLAuthenticator.authenticate()");

        boolean authn_result = super.authenticate(request, response, config);
        if( authn_result ) {
          JBossGenericPrincipal principal = (JBossGenericPrincipal)request.getPrincipal();

          // Use the certificate mapping object to convert the principal name
          X509Certificate[] certs = request.getCertificateChain();
          Principal mappedPrincipal = certMapping.toPrincipal(certs);

          JBossGenericPrincipal gp = new JBossGenericPrincipal(context.getRealm(),
                                                               mappedPrincipal.getName(),
                                                               null, Arrays.asList(principal.getRoles()),
                                                               mappedPrincipal, null, certs,
                                                               null, principal.getSubject());

          // Cache the principal (if requested) and record this authentication
          register(request, response, gp, HttpServletRequest.CLIENT_CERT_AUTH, null, null);
        }

        return authn_result;
    }
}
