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

package org.jboss.as.domain.http.server;

/**
 * Constants for use within the management HTTP server.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface Constants {

    /*
     * Request Methods
     */

    String GET = "GET";
    String OPTIONS = "OPTIONS";
    String POST = "POST";

    /*
     * Protocols
     */

    String HTTP = "http";
    String HTTPS = "https";

    /*
     * Headers
     */

    String ACCEPT = "Accept";
    String AUTHORIZATION_HEADER = "Authorization";
    String CONTENT_DISPOSITION = "Content-Disposition";
    String CONTENT_TYPE = "Content-Type";
    String HOST = "Host";
    String LOCATION = "Location";
    String ORIGIN = "Origin";
    String RETRY_AFTER = "Retry-After";
    String VIA = "Via";
    String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";

    /*
     * Content Types
     */

    String APPLICATION_DMR_ENCODED = "application/dmr-encoded";
    String APPLICATION_JAVASCRIPT = "application/javascript";
    String APPLICATION_JSON = "application/json";
    String APPLICATION_OCTET_STREAM = "application/octet-stream";
    String IMAGE_GIF = "image/gif";
    String IMAGE_JPEG = "image/jpeg";
    String IMAGE_PNG = "image/png";
    String TEXT_CSS = "text/css";
    String TEXT_HTML = "text/html";

    /*
     * Charsets
     */

    String US_ASCII = "US-ASCII";
    String UTF_8 = "utf-8";

    /*
     * Status Codes
     */

    int CONTINUE = 100;
    int SWITCHING_PROTOCOLS = 101;
    int OK = 200;
    int CREATED = 201;
    int ACCEPTED = 202;
    int NON_AUTHORITATIVE_INFORMATION = 203;
    int NO_CONTENT = 204;
    int RESET_CONTENT = 205;
    int PARTIAL_CONTENT = 206;
    int MULTIPLE_CHOICES = 300;
    int MOVED_PERMENANTLY = 301;
    int FOUND = 302;
    int SEE_OTHER = 303;
    int NOT_MODIFIED = 304;
    int USE_PROXY = 305;
    int TEMPORARY_REDIRECT = 307;
    int BAD_REQUEST = 400;
    int UNAUTHORIZED = 401;
    int PAYMENT_REQUIRED = 402;
    int FORBIDDEN = 403;
    int NOT_FOUND = 404;
    int METHOD_NOT_ALLOWED = 405;
    int NOT_ACCEPTABLE = 406;
    int PROXY_AUTHENTICATION_REQUIRED = 407;
    int REQUEST_TIME_OUT = 408;
    int CONFLICT = 409;
    int GONE = 410;
    int LENGTH_REQUIRED = 411;
    int PRECONDITION_FAILED = 412;
    int REQUEST_ENTITY_TOO_LARGE = 413;
    int REQUEST_URI_TOO_LARGE = 414;
    int UNSUPPORTED_MEDIA_TYPE = 415;
    int REQUEST_RANGE_NOT_SATISFIABLE = 416;
    int EXPECTATION_FAILED = 417;
    int INTERNAL_SERVER_ERROR = 500;
    int NOT_IMPLEMENTED = 501;
    int BAD_GATEWAY = 502;
    int SERVICE_UNAVAILABLE = 503;
    int GATEWAY_TIME_OUT = 504;
    int HTTP_VERSION_NOT_SUPPORTED = 505;

}
