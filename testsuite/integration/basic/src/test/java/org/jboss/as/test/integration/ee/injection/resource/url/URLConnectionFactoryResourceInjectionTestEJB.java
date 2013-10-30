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

package org.jboss.as.test.integration.ee.injection.resource.url;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import java.net.URL;

/**
 * @author Eduardo Martins
 */
@Stateless
public class URLConnectionFactoryResourceInjectionTestEJB {

    @Resource(name = "overrideLookupURL", lookup = "http://www.wildfly.org")
    private URL url1;

    @Resource(name = "lookupURL", lookup = "http://www.wildfly.org")
    private URL url2;

    /**
     *
     * @throws Exception
     */
    public void validateResourceInjection() throws Exception {
        if (url1 == null) {
           throw new NullPointerException("url1 resource not injected");
        }
        if (url2 == null) {
            throw new NullPointerException("url2 resource not injected");
        }
    }

}
