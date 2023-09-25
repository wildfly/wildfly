/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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