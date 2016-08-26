/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.ee.injection.resource.resourceref;

import java.net.URL;

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.Stateless;

/**
 * Let's see what we can do with resources of the URL breed.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 */
@Stateless
@Remote(ResUrlChecker.class)
public class ResUrlCheckerBean implements ResUrlChecker {
    // coming in via res-url
    @Resource(name = "url2")
    private URL url2;

    public URL getURL1() {
        return null;
    }

    public URL getURL2() {
        return url2;
    }

    public URL getURL3() {
        return null;
    }
}
