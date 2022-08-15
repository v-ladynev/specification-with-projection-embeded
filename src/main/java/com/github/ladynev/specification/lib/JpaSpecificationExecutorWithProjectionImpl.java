package com.github.ladynev.specification.lib;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.query.JpaEntityGraph;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.data.jpa.repository.support.CrudMethodMetadata;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.QueryHints;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.PluralAttribute;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static javax.persistence.metamodel.Attribute.PersistentAttributeType.ELEMENT_COLLECTION;
import static javax.persistence.metamodel.Attribute.PersistentAttributeType.MANY_TO_MANY;
import static javax.persistence.metamodel.Attribute.PersistentAttributeType.MANY_TO_ONE;
import static javax.persistence.metamodel.Attribute.PersistentAttributeType.ONE_TO_MANY;
import static javax.persistence.metamodel.Attribute.PersistentAttributeType.ONE_TO_ONE;

/**
 * Created by pramoth on 9/29/2016 AD.
 */
public class JpaSpecificationExecutorWithProjectionImpl<T, ID extends Serializable>
        extends SimpleJpaRepository<T, ID> implements JpaSpecificationExecutorWithProjection<T, ID> {

    private static final Map<Attribute.PersistentAttributeType, Class<? extends Annotation>> ASSOCIATION_TYPES;

    static {
        Map<Attribute.PersistentAttributeType, Class<? extends Annotation>> persistentAttributeTypes = new HashMap<>();
        persistentAttributeTypes.put(ONE_TO_ONE, OneToOne.class);
        persistentAttributeTypes.put(ONE_TO_MANY, null);
        persistentAttributeTypes.put(MANY_TO_ONE, ManyToOne.class);
        persistentAttributeTypes.put(MANY_TO_MANY, null);
        persistentAttributeTypes.put(ELEMENT_COLLECTION, null);

        ASSOCIATION_TYPES = Collections.unmodifiableMap(persistentAttributeTypes);
    }

    private final EntityManager entityManager;

    private final ProjectionFactory projectionFactory = new SpelAwareProxyProjectionFactory();

    private final JpaEntityInformation<T, ID> entityInformation;

    public JpaSpecificationExecutorWithProjectionImpl(JpaEntityInformation<T, ID> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
        this.entityInformation = entityInformation;
    }

    @Override
    public <R> Optional<R> findById(ID id, Class<R> projectionType) {
        final ReturnedType returnedType = ReturnedType.of(projectionType, getDomainClass(), projectionFactory);

        CriteriaBuilder builder = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> q = builder.createQuery(Tuple.class);
        Root<T> root = q.from(getDomainClass());
        q.where(builder.equal(root.get(entityInformation.getIdAttribute()), id));

        if (returnedType.needsCustomConstruction()) {
            List<Selection<?>> selections = new ArrayList<>();

            for (String property : returnedType.getInputProperties()) {
                PropertyPath path = PropertyPath.from(property, returnedType.getReturnedType());
                selections.add(toExpressionRecursively(root, path, true).alias(property));
            }

            q.multiselect(selections);
        } else {
            throw new IllegalArgumentException("only except projection");
        }

        final TypedQuery<Tuple> query = this.applyRepositoryMethodMetadataSelf(this.entityManager.createQuery(q));

        try {
            final MyResultProcessor resultProcessor = new MyResultProcessor(projectionFactory, returnedType);
            final R singleResult = resultProcessor.processResult(query.getSingleResult(), new TupleConverter(returnedType));
            return Optional.ofNullable(singleResult);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public <R> Optional<R> findOne(Specification<T> spec, Class<R> projectionType) {
        final ReturnedType returnedType = ReturnedType.of(projectionType, getDomainClass(), projectionFactory);
        final TypedQuery<Tuple> query = getTupleQuery(spec, Sort.unsorted(), returnedType);
        try {
            final MyResultProcessor resultProcessor = new MyResultProcessor(projectionFactory, returnedType);
            final R singleResult = resultProcessor.processResult(query.getSingleResult(), new TupleConverter(returnedType));
            return Optional.ofNullable(singleResult);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public <R> List<R> findAll(Specification<T> spec, Class<R> projectionType) {
        return findAll(spec, projectionType, Sort.unsorted());
    }

    @Override
    public <R> List<R> findAll(Specification<T> spec, Class<R> projectionType, Sort sort) {
        return findAll(spec, projectionType, Pageable.unpaged(), sort).getContent();
    }

    @Override
    public <R> Page<R> findAll(Specification<T> spec, Class<R> projectionType, Pageable pageable) {
        Sort sort = pageable.getSortOr(Sort.unsorted());
        return findAll(spec, projectionType, pageable, sort);
    }

    private <R> Page<R> findAll(Specification<T> spec, Class<R> projectionType, Pageable pageable, Sort sort) {
        final ReturnedType returnedType = ReturnedType.of(projectionType, getDomainClass(), projectionFactory);
        final TypedQuery<Tuple> query = getTupleQuery(spec, sort, returnedType);
        final MyResultProcessor resultProcessor = new MyResultProcessor(projectionFactory, returnedType);
        if (pageable.isPaged()) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }
        final List<R> resultList = resultProcessor.processResult(query.getResultList(), new TupleConverter(returnedType));
        final Page<R> page = PageableExecutionUtils.getPage(
                resultList, pageable, () -> executeCountQuery(this.getCountQuery(spec, getDomainClass()))
        );
        return pageable.isUnpaged() ? new PageImpl<>(resultList) : page;
    }

    private static long executeCountQuery(TypedQuery<Long> query) {
        Assert.notNull(query, "TypedQuery must not be null!");
        List<Long> totals = query.getResultList();
        long total = 0L;

        Long element;
        for (Iterator<Long> it = totals.iterator(); it.hasNext(); total = total + (element == null ? 0L : element)) {
            element = it.next();
        }

        return total;
    }

    @Override
    public <R> Page<R> findAll(Specification<T> spec, Class<R> projectionType,
                               String namedEntityGraph, EntityGraph.EntityGraphType type, Pageable pageable) {
        return findAll(spec, projectionType, pageable);
    }

    @Override
    public <R> Page<R> findAll(Specification<T> spec, Class<R> projectionType,
                               JpaEntityGraph dynamicEntityGraph, Pageable pageable) {
        return findAll(spec, projectionType, pageable);
    }

    protected TypedQuery<Tuple> getTupleQuery(@Nullable Specification<T> spec, Sort sort, ReturnedType returnedType) {
        if (!returnedType.needsCustomConstruction()) {
            return (TypedQuery<Tuple>) getQuery(spec, sort);
        }

        CriteriaBuilder builder = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = builder.createQuery(Tuple.class);
        Root<T> root = applySpecificationToCriteria(spec, getDomainClass(), query, builder);

        List<Selection<?>> selections = new ArrayList<>();
        for (String property : returnedType.getInputProperties()) {
            PropertyPath path = PropertyPath.from(property, returnedType.getReturnedType());
            selections.add(toExpressionRecursively(root, path, true).alias(property));
        }
        query.multiselect(selections);

        if (sort.isSorted()) {
            query.orderBy(QueryUtils.toOrders(sort, root, builder));
        }

        return this.applyRepositoryMethodMetadataSelf(entityManager.createQuery(query));
    }


    private <S, U extends T> Root<U> applySpecificationToCriteria(@Nullable Specification<U> spec, Class<U> domainClass,
                                                                  CriteriaQuery<S> query, CriteriaBuilder builder) {

        Assert.notNull(domainClass, "Domain class must not be null!");
        Assert.notNull(query, "CriteriaQuery must not be null!");

        Root<U> root = query.from(domainClass);

        if (spec == null) {
            return root;
        }

        Predicate predicate = spec.toPredicate(root, query, builder);

        if (predicate != null) {
            query.where(predicate);
        }

        return root;
    }

    private <S> TypedQuery<S> applyRepositoryMethodMetadataSelf(TypedQuery<S> query) {
        CrudMethodMetadata metadata = getRepositoryMethodMetadata();
        if (metadata == null) {
            return query;
        }

        LockModeType type = metadata.getLockModeType();
        TypedQuery<S> toReturn = type == null ? query : query.setLockMode(type);
        applyQueryHintsSelf(toReturn);

        return toReturn;
    }

    private void applyQueryHintsSelf(Query query) {
        QueryHints queryHints = DefaultQueryHints.of(this.entityInformation, getRepositoryMethodMetadata());
        queryHints.withFetchGraphs(this.entityManager).forEach(query::setHint);
    }

    static Expression<Object> toExpressionRecursively(Path<Object> path, PropertyPath property) {
        if (property == null) {
            return path;
        }

        Path<Object> result = path.get(property.getSegment());
        return property.hasNext() ? toExpressionRecursively(result, property.next()) : result;
    }

    static <T> Expression<T> toExpressionRecursively(From<?, ?> from, PropertyPath property, boolean isForSelection) {

        Bindable<?> propertyPathModel;
        Bindable<?> model = from.getModel();
        String segment = property.getSegment();

        if (model instanceof ManagedType) {

            /*
             *  Required to keep support for EclipseLink 2.4.x. TODO: Remove once we drop that (probably Dijkstra M1)
             *  See: https://bugs.eclipse.org/bugs/show_bug.cgi?id=413892
             */
            propertyPathModel = (Bindable<?>) ((ManagedType<?>) model).getAttribute(segment);
        } else {
            propertyPathModel = from.get(segment).getModel();
        }

        if (requiresJoin(propertyPathModel, model instanceof PluralAttribute, !property.hasNext(), isForSelection)
                && !isAlreadyFetched(from, segment)) {
            Join<?, ?> join = getOrCreateJoin(from, segment);
            return (Expression<T>) (property.hasNext() ? toExpressionRecursively(join, property.next(), isForSelection)
                    : join);
        } else {
            Path<Object> path = from.get(segment);
            return (Expression<T>) (property.hasNext() ? toExpressionRecursively(path, property.next()) : path);
        }
    }

    private static boolean requiresJoin(@Nullable Bindable<?> propertyPathModel, boolean isPluralAttribute,
                                        boolean isLeafProperty, boolean isForSelection) {

        if (propertyPathModel == null && isPluralAttribute) {
            return true;
        }

        if (!(propertyPathModel instanceof Attribute)) {
            return false;
        }

        Attribute<?, ?> attribute = (Attribute<?, ?>) propertyPathModel;

        if (!ASSOCIATION_TYPES.containsKey(attribute.getPersistentAttributeType())) {
            return false;
        }

        // if this path is part of the select list we need to generate an explicit outer join in order to prevent Hibernate
        // to use an inner join instead.
        // see https://hibernate.atlassian.net/browse/HHH-12999.
        if (isLeafProperty && !isForSelection && !attribute.isCollection()) {
            return false;
        }

        Class<? extends Annotation> associationAnnotation = ASSOCIATION_TYPES.get(attribute.getPersistentAttributeType());

        if (associationAnnotation == null) {
            return true;
        }

        Member member = attribute.getJavaMember();

        if (!(member instanceof AnnotatedElement)) {
            return true;
        }

        Annotation annotation = AnnotationUtils.getAnnotation((AnnotatedElement) member, associationAnnotation);
        return annotation == null || (boolean) AnnotationUtils.getValue(annotation, "optional");
    }

    private static Join<?, ?> getOrCreateJoin(From<?, ?> from, String attribute) {

        for (Join<?, ?> join : from.getJoins()) {

            boolean sameName = join.getAttribute().getName().equals(attribute);

            if (sameName && join.getJoinType().equals(JoinType.LEFT)) {
                return join;
            }
        }

        return from.join(attribute, JoinType.LEFT);
    }

    private static boolean isAlreadyFetched(From<?, ?> from, String attribute) {

        for (Fetch<?, ?> fetch : from.getFetches()) {

            boolean sameName = fetch.getAttribute().getName().equals(attribute);

            if (sameName && fetch.getJoinType().equals(JoinType.LEFT)) {
                return true;
            }
        }

        return false;
    }
}
