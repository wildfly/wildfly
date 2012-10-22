/*
 * Copyright (c) OSGi Alliance (2000, 2008). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.osgi.service.http;

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This interface defines methods that the Http Service may call to get information about a registration.
 *
 * <p>
 * Servlets and resources may be registered with an <code>HttpContext</code> object; if no <code>HttpContext</code> object is
 * specified, a default <code>HttpContext</code> object is used. Servlets that are registered using the same
 * <code>HttpContext</code> object will share the same <code>ServletContext</code> object.
 *
 * <p>
 * This interface is implemented by users of the <code>HttpService</code>.
 *
 * @version $Revision: 5673 $
 */
public interface HttpContext {
    /**
     * <code>HttpServletRequest</code> attribute specifying the name of the authenticated user. The value of the attribute can
     * be retrieved by <code>HttpServletRequest.getRemoteUser</code>. This attribute name is
     * <code>org.osgi.service.http.authentication.remote.user</code>.
     *
     * @since 1.1
     */
    String REMOTE_USER = "org.osgi.service.http.authentication.remote.user";
    /**
     * <code>HttpServletRequest</code> attribute specifying the scheme used in authentication. The value of the attribute can be
     * retrieved by <code>HttpServletRequest.getAuthType</code>. This attribute name is
     * <code>org.osgi.service.http.authentication.type</code>.
     *
     * @since 1.1
     */
    String AUTHENTICATION_TYPE = "org.osgi.service.http.authentication.type";
    /**
     * <code>HttpServletRequest</code> attribute specifying the <code>Authorization</code> object obtained from the
     * <code>org.osgi.service.useradmin.UserAdmin</code> service. The value of the attribute can be retrieved by
     * <code>HttpServletRequest.getAttribute(HttpContext.AUTHORIZATION)</code>. This attribute name is
     * <code>org.osgi.service.useradmin.authorization</code>.
     *
     * @since 1.1
     */
    String AUTHORIZATION = "org.osgi.service.useradmin.authorization";

    /**
     * Handles security for the specified request.
     *
     * <p>
     * The Http Service calls this method prior to servicing the specified request. This method controls whether the request is
     * processed in the normal manner or an error is returned.
     *
     * <p>
     * If the request requires authentication and the Authorization header in the request is missing or not acceptable, then
     * this method should set the WWW-Authenticate header in the response object, set the status in the response object to
     * Unauthorized(401) and return <code>false</code>. See also RFC 2617: <i>HTTP Authentication: Basic and Digest Access
     * Authentication </i> (available at http://www.ietf.org/rfc/rfc2617.txt).
     *
     * <p>
     * If the request requires a secure connection and the <code>getScheme</code> method in the request does not return 'https'
     * or some other acceptable secure protocol, then this method should set the status in the response object to Forbidden(403)
     * and return <code>false</code>.
     *
     * <p>
     * When this method returns <code>false</code>, the Http Service will send the response back to the client, thereby
     * completing the request. When this method returns <code>true</code>, the Http Service will proceed with servicing the
     * request.
     *
     * <p>
     * If the specified request has been authenticated, this method must set the {@link #AUTHENTICATION_TYPE} request attribute
     * to the type of authentication used, and the {@link #REMOTE_USER} request attribute to the remote user (request attributes
     * are set using the <code>setAttribute</code> method on the request). If this method does not perform any authentication,
     * it must not set these attributes.
     *
     * <p>
     * If the authenticated user is also authorized to access certain resources, this method must set the {@link #AUTHORIZATION}
     * request attribute to the <code>Authorization</code> object obtained from the
     * <code>org.osgi.service.useradmin.UserAdmin</code> service.
     *
     * <p>
     * The servlet responsible for servicing the specified request determines the authentication type and remote user by calling
     * the <code>getAuthType</code> and <code>getRemoteUser</code> methods, respectively, on the request.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @return <code>true</code> if the request should be serviced, <code>false</code> if the request should not be serviced and
     *         Http Service will send the response back to the client.
     * @throws java.io.IOException may be thrown by this method. If this occurs, the Http Service will terminate the request and
     *         close the socket.
     */
    boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException;

    /**
     * Maps a resource name to a URL.
     *
     * <p>
     * Called by the Http Service to map a resource name to a URL. For servlet registrations, Http Service will call this method
     * to support the <code>ServletContext</code> methods <code>getResource</code> and <code>getResourceAsStream</code>. For
     * resource registrations, Http Service will call this method to locate the named resource. The context can control from
     * where resources come. For example, the resource can be mapped to a file in the bundle's persistent storage area via
     * <code>bundleContext.getDataFile(name).toURL()</code> or to a resource in the context's bundle via
     * <code>getClass().getResource(name)</code>
     *
     * @param name the name of the requested resource
     * @return URL that Http Service can use to read the resource or <code>null</code> if the resource does not exist.
     */
    URL getResource(String name);

    /**
     * Maps a name to a MIME type.
     *
     * Called by the Http Service to determine the MIME type for the name. For servlet registrations, the Http Service will call
     * this method to support the <code>ServletContext</code> method <code>getMimeType</code>. For resource registrations, the
     * Http Service will call this method to determine the MIME type for the Content-Type header in the response.
     *
     * @param name determine the MIME type for this name.
     * @return MIME type (e.g. text/html) of the name or <code>null</code> to indicate that the Http Service should determine
     *         the MIME type itself.
     */
    String getMimeType(String name);
}
