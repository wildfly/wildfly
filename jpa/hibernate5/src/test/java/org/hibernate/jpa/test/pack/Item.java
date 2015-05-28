/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.jpa.test.pack;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.FieldResult;
import javax.persistence.Id;
import javax.persistence.LockModeType;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.QueryHint;
import javax.persistence.SqlResultSetMapping;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.QueryHints;

/**
 * @author Gavin King
 */
@Entity(name = "Item")
@SqlResultSetMapping(name = "getItem", entities =
	@EntityResult(entityClass = Item.class, fields = {
		@FieldResult(name = "name", column = "itemname"),
		@FieldResult(name = "descr", column = "itemdescription")
	})
)
@NamedNativeQueries({
	@NamedNativeQuery(
		name = "nativeItem1",
		query = "select name as itemname, descr as itemdescription from Item",
		resultSetMapping = "getItem"
	),
	@NamedNativeQuery(
		name = "nativeItem2",
		query = "select * from Item",
		resultClass = Item.class
	)
})
@NamedQueries({
		@NamedQuery(
				name = "itemJpaQueryWithLockModeAndHints",
				query = "select i from Item i",
				lockMode = LockModeType.PESSIMISTIC_WRITE,
				hints = {
						@QueryHint( name = QueryHints.TIMEOUT_JPA, value = "3000" ),
						@QueryHint( name = QueryHints.CACHE_MODE, value = "ignore" ),
						@QueryHint( name = QueryHints.CACHEABLE, value = "true" ),
						@QueryHint( name = QueryHints.READ_ONLY, value = "true" ),
						@QueryHint( name = QueryHints.COMMENT, value = "custom static comment" ),
						@QueryHint( name = QueryHints.FETCH_SIZE, value = "512" ),
						@QueryHint( name = QueryHints.FLUSH_MODE, value = "manual" )
				}
		),
		@NamedQuery(name = "query-construct", query = "select new Item(i.name,i.descr) from Item i")
})
public class Item implements Serializable {

	private String name;
	private String descr;
	private Set<Distributor> distributors = new HashSet<Distributor>();

	public Item() {
	}

	public Item(String name, String desc) {
		this.name = name;
		this.descr = desc;
	}

	@Column(length = 200)
	public String getDescr() {
		return descr;
	}

	public void setDescr(String desc) {
		this.descr = desc;
	}

	@Id
	@Column(length = 30)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@OneToMany
	public Set<Distributor> getDistributors() {
		return distributors;
	}

	public void setDistributors(Set<Distributor> distributors) {
		this.distributors = distributors;
	}

	public void addDistributor(Distributor d) {
		if ( distributors == null ) distributors = new HashSet();
		distributors.add( d );
	}
}
