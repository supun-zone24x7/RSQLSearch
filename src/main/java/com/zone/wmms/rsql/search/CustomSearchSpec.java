package com.zone.wmms.rsql.search;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;
import java.util.Set;

public class CustomSearchSpec<T> {


    public Optional<Specification<T>> createSearchSpecification(String searchCriteria) {
        if (searchCriteria == null || searchCriteria.equals("")) {
            return Optional.empty();
        }

        Set<ComparisonOperator> operators = RSQLOperators.defaultOperators();
        operators.add(RsqlSearchOperation.IS_NULL.getOperator());
        operators.add(RsqlSearchOperation.IS_EMPTY.getOperator());

        Node rootNode = new RSQLParser(operators).parse(searchCriteria);
        Specification<T> specification = rootNode.accept(new CustomRsqlVisitor<>());

        return Optional.of(specification);
    }

    public Optional<Specification<T>> createDistinctSearchSpecification(String searchCriteria) {
        return createSearchSpecification(searchCriteria).map(this::distinctSpecification);
    }

    public Specification<T> distinctSpecification(Specification<T> initialSpec) {
        return (final Root<T> root, final CriteriaQuery<?> query, final CriteriaBuilder builder) -> {

            query.distinct(true);
            return initialSpec.toPredicate(root, query, builder);
        };
    }
}
