/*
 *  Copyright 2018 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.gcp.data.spanner.core.mapping;

import java.util.OptionalInt;
import java.util.stream.Collectors;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.StreamUtils;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.StringUtils;

/**
 * Represents an implementation for {@link SpannerPersistentProperty}, which is a property of a
 * {@link SpannerPersistentEntity} stored in a Google Spanner table.
 *
 * @author Ray Tsang
 * @author Chengyuan Zhao
 */
public class SpannerPersistentPropertyImpl
		extends AnnotationBasedPersistentProperty<SpannerPersistentProperty>
		implements SpannerPersistentProperty {

	private FieldNamingStrategy fieldNamingStrategy;

	/**
	 * Creates a new {@link SpannerPersistentPropertyImpl}
	 *
	 * @param property the property to store
	 * @param owner the entity to which this property belongs
	 * @param simpleTypeHolder
	 * @param fieldNamingStrategy the naming strategy used to get the column name of this property
	 */
	public SpannerPersistentPropertyImpl(Property property,
			PersistentEntity<?, SpannerPersistentProperty> owner,
			SimpleTypeHolder simpleTypeHolder, FieldNamingStrategy fieldNamingStrategy) {
		super(property, owner, simpleTypeHolder);
		this.fieldNamingStrategy = fieldNamingStrategy == null
				? PropertyNameFieldNamingStrategy.INSTANCE
				: fieldNamingStrategy;
	}

	/**
	 * Only provides types that are also annotated with {@link Table}.
	 */
	@Override
	public Iterable<? extends TypeInformation<?>> getPersistentEntityTypes() {
		return StreamUtils
				.createStreamFromIterator(super.getPersistentEntityTypes().iterator())
				.filter(typeInfo -> typeInfo.getType().isAnnotationPresent(Table.class))
				.collect(Collectors.toList());
	}

	@Override
	protected Association<SpannerPersistentProperty> createAssociation() {
		return new Association<>(this, null);
	}

	/**
	 * Gets the name of the column in the Google Spanner table mapped to this property.
	 * The column name is resolved using the {@link FieldNamingStrategy} passed in to the
	 * {@link SpannerPersistentPropertyImpl#SpannerPersistentPropertyImpl(Property, PersistentEntity,
	 * SimpleTypeHolder, FieldNamingStrategy)} constructor.
	 * This is by default the by default
	 *
	 * @return the name of the column.
	 * @throws {@link MappingException} if the resolution fails
	 */
	@Override
	public String getColumnName() {
		if (StringUtils.hasText(getAnnotatedColumnName())) {
			return getAnnotatedColumnName();
		}

		String fieldName = this.fieldNamingStrategy.getFieldName(this);

		if (!StringUtils.hasText(fieldName)) {
			throw new MappingException(String.format(
					"Invalid (null or empty) field name returned for property %s by %s!",
					this, this.fieldNamingStrategy.getClass()));
		}

		return fieldName;
	}

	@Override
	public Class getColumnInnerType() {
		ColumnInnerType annotation = findAnnotation(ColumnInnerType.class);
		if (annotation == null) {
			return null;
		}
		return annotation.innerType();
	}

	@Override
	public OptionalInt getPrimaryKeyOrder() {
		PrimaryKey annotation = findAnnotation(
				PrimaryKey.class);
		if (annotation == null) {
			return OptionalInt.empty();
		}
		return OptionalInt.of(annotation.keyOrder());
	}

	@Override
	public boolean isMapped() {
		return findAnnotation(NotMapped.class) == null;
	}

	@Override
	public boolean isIdProperty() {
		return false;
	}

	private String getAnnotatedColumnName() {

		Column annotation = findAnnotation(Column.class);

		if (annotation != null && StringUtils.hasText(annotation.name())) {
			return annotation.name();
		}

		return null;
	}
}
