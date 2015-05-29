/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.hibernate.jpa.test.pack;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.jboss.logging.Logger;

/**
 * @author Emmanuel Bernard
 */
@SuppressWarnings({"unchecked", "serial"})
@Entity
@EntityListeners( LastUpdateListener.class )
public class Cat implements Serializable {
	private static final Logger log = Logger.getLogger( Cat.class );

	private static final List ids = new ArrayList(); 	// used for assertions
	public static int postVersion = 0;	// used for assertions

	private Integer id;
	private String name;
	private Date dateOfBirth;
	private int age;
	private long length;
	private Date lastUpdate;
	private int manualVersion = 0;
	private List<Kitten> kittens;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public int getManualVersion() {
		return manualVersion;
	}

	public void setManualVersion(int manualVersion) {
		this.manualVersion = manualVersion;
	}

	@Transient
	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	@Basic
	@Temporal( TemporalType.TIMESTAMP )
	public Date getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	@PostUpdate
	private void someLateUpdateWorking() {
        log.debug("PostUpdate for: " + this.toString());
		postVersion++;
	}

	@PostLoad
	public void calculateAge() {
		Calendar birth = new GregorianCalendar();
		birth.setTime( dateOfBirth );
		Calendar now = new GregorianCalendar();
		now.setTime( new Date() );
		int adjust = 0;
		if ( now.get( Calendar.DAY_OF_YEAR ) - birth.get( Calendar.DAY_OF_YEAR ) < 0 ) {
			adjust = -1;
		}
		age = now.get( Calendar.YEAR ) - birth.get( Calendar.YEAR ) + adjust;
	}

	@PostPersist
	public synchronized void addIdsToList() {
		ids.add( getId() );
	}

	public static synchronized List getIdList() {
		return Collections.unmodifiableList( ids );
	}

	public long getLength() {
		return length;
	}

	public void setLength(long length) {
		this.length = length;
	}

	@OneToMany(cascade = CascadeType.ALL)
	public List<Kitten> getKittens() {
		return kittens;
	}

	public void setKittens(List<Kitten> kittens) {
		this.kittens = kittens;
	}

	/**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation
	 * of this object.
	 */
	@Override
    public String toString()
	{
	    final String TAB = "    ";

	    String retValue = "";

	    retValue = "Cat ( "
	        + super.toString() + TAB
	        + "id = " + this.id + TAB
	        + "name = " + this.name + TAB
	        + "dateOfBirth = " + this.dateOfBirth + TAB
	        + "age = " + this.age + TAB
	        + "length = " + this.length + TAB
	        + "lastUpdate = " + this.lastUpdate + TAB
	        + "manualVersion = " + this.manualVersion + TAB
	        + "postVersion = " + Cat.postVersion + TAB
	        + "kittens = " + this.kittens + TAB
	        + " )";

	    return retValue;
	}
}
