/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.hibernate;

import java.util.Set;

/**
 * Represents a planet object.
 *
 * @author Madhumita Sadhukhan
 */
public class Planet {
    // unique id
    private Integer planetId;
    // name of the planet
    private String planetName;
    // name of the galaxy it is situated in
    private String galaxy;
    // name of the star it rotates
    private String star;
    // set of satellites associated with planet
    private Set<Satellite> satellites;

    /**
     * Default constructor
     */
    public Planet() {
    }

    /**
     * Creates a new instance of Planet.
     *
     * @param planetName planetName.
     * @param galaxy     galaxy name.
     * @param star       star name.
     */
    public Planet(String planetName, String galaxy, String star, Set<Satellite> satellites) {
        this.planetName = planetName;
        this.galaxy = galaxy;
        this.star = star;
        this.satellites = satellites;
    }

    /**
     * Gets the planet id for planet.
     *
     * @return planet id.
     */
    public Integer getPlanetId() {
        return planetId;
    }

    /**
     * Sets the planetId id for this student.
     *
     * @return planetId id.
     */
    public void setPlanetId(Integer planetId) {
        this.planetId = planetId;
    }

    public String getPlanetName() {
        return planetName;
    }

    public void setPlanetName(String planetName) {
        this.planetName = planetName;
    }

    public String getGalaxy() {
        return galaxy;
    }

    public void setGalaxy(String galaxy) {
        this.galaxy = galaxy;
    }

    public String getStar() {
        return star;
    }

    public void setStar(String star) {
        this.star = star;
    }

    public Set<Satellite> getSatellites() {
        return satellites;
    }

    public void setSatellites(Set<Satellite> satellites) {
        this.satellites = satellites;
    }

}