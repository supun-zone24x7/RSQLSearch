package com.zone.wmms.rsql.search;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Based on: https://www.baeldung.com/rest-api-search-language-rsql-fiql
 *
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GenericRsqlSpecification<T> implements Specification<T> {

    private static final long serialVersionUID = 1;
    private transient ComparisonOperator operator;
    private String property;
    private List<String> arguments;
    private String dateTimeFormat = "yyyy-MM-dd HH:mm:ss.SSS"; //Todo: need to make this configurable


    public GenericRsqlSpecification(final String property, final ComparisonOperator operator, final List<String> arguments) {
        super();
        this.property = property;
        this.operator = operator;
        this.arguments = arguments;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query,
                                 CriteriaBuilder builder) {
        Path<String> propertyExpression = parseProperty(root);

        List<Object> args = castArguments(propertyExpression);
        Object argument = args.get(0);
        switch (RsqlSearchOperation.getSimpleOperator(this.operator)) {
            case EQUAL:
                return getPredicateForEqual(root, builder, propertyExpression, argument);

            case NOT_EQUAL:
                return getPredicateForNotEqual(root, builder, argument, propertyExpression);

            case GREATER_THAN:
                return getPredicateForGreaterThan(root, builder, argument, propertyExpression);

            case GREATER_THAN_OR_EQUAL:
                return getPredicateForGreaterThanOrEqual(root, builder, argument, propertyExpression);

            case LESS_THAN:
                return getPredicateForLessThan(root, builder, argument, propertyExpression);

            case LESS_THAN_OR_EQUAL:
                return getPredicateLessThanOrEqual(root, builder, argument, propertyExpression);

            case IN:
                return propertyExpression.in(args);

            case NOT_IN:
                return builder.not(propertyExpression.in(args));

            case IS_NULL:
                if (argument instanceof String) {
                    String argumentString = argument.toString();
                    if (argumentString.equalsIgnoreCase("true")) {
                        return builder.isNull(propertyExpression);
                    } else if (argumentString.equalsIgnoreCase("false")) {
                        return builder.isNotNull(propertyExpression);
                    }
                }
                break;
            case IS_EMPTY:
                if (argument instanceof String) {
                    String argumentString = argument.toString();
                    if (argumentString.equalsIgnoreCase("true")) {
                        return builder.isEmpty(parsePropertyList(root));
                    } else if (argumentString.equalsIgnoreCase("false")) {
                        return builder.isNotEmpty(parsePropertyList(root));
                    }
                }
                break;
        }

        return null;
    }

    private Predicate getPredicateLessThanOrEqual(Root<T> root, CriteriaBuilder builder, Object argument, Path<String> propertyExpression) {
        if (argument instanceof ZonedDateTime) {
            return builder.lessThanOrEqualTo(parsePropertyZonedDateTime(root), (ZonedDateTime) argument);
        } else if (argument instanceof LocalDate) {
            return builder.lessThanOrEqualTo(parsePropertyLocalDate(root), (LocalDate) argument);

        } else if (argument instanceof LocalDate) {
            return builder.lessThanOrEqualTo(parsePropertyLocalDate(root), (LocalDate) argument);

        } else {
            return builder.lessThanOrEqualTo(propertyExpression, argument.toString());
        }
    }

    private Predicate getPredicateForLessThan(Root<T> root, CriteriaBuilder builder, Object argument, Path<String> propertyExpression) {
        if (argument instanceof ZonedDateTime) {
            return builder.lessThan(parsePropertyZonedDateTime(root), (ZonedDateTime) argument);
        } else if (argument instanceof LocalDate) {
            return builder.lessThan(parsePropertyLocalDate(root), (LocalDate) argument);

        } else {
            return builder.lessThan(propertyExpression, argument.toString());
        }
    }

    private Predicate getPredicateForGreaterThanOrEqual(Root<T> root, CriteriaBuilder builder, Object argument, Path<String> propertyExpression) {
        if (argument instanceof ZonedDateTime) {
            return builder.greaterThanOrEqualTo(parsePropertyZonedDateTime(root), (ZonedDateTime) argument);
        } else if (argument instanceof LocalDate) {
            return builder.greaterThanOrEqualTo(parsePropertyLocalDate(root), (LocalDate) argument);
        } else {
            return builder.greaterThanOrEqualTo(propertyExpression, argument.toString());
        }
    }

    private Predicate getPredicateForGreaterThan(Root<T> root, CriteriaBuilder builder, Object argument, Path<String> propertyExpression) {
        if (argument instanceof ZonedDateTime) {
            return builder.greaterThan(parsePropertyZonedDateTime(root), (ZonedDateTime) argument);
        } else if (argument instanceof LocalDate) {
            return builder.greaterThan(parsePropertyLocalDate(root), (LocalDate) argument);

        } else {
            return builder.greaterThan(propertyExpression, argument.toString());
        }
    }

    private Predicate getPredicateForNotEqual(Root<T> root, CriteriaBuilder builder, Object argument, Path<String> propertyExpression) {
        if (argument instanceof ZonedDateTime) {
            return builder.notEqual(parsePropertyZonedDateTime(root), argument);
        } else if (argument instanceof LocalDate) {
            return builder.notEqual(parsePropertyLocalDate(root), (LocalDate) argument);

        } else if (argument instanceof String) {
            return builder.notLike(propertyExpression,
                    argument.toString().replace('*', '%'));
        } else if (argument == null) {
            return builder.isNotNull(propertyExpression);
        } else {
            return builder.notEqual(propertyExpression, argument);
        }
    }

    private Predicate getPredicateForEqual(Root<T> root, CriteriaBuilder builder, Path<String> propertyExpression, Object argument) {
        if (propertyExpression.getJavaType().equals(List.class)) {
            Expression<String> delimiter = builder.<String>literal("");
            return builder.like(
                    builder.lower(
                            builder.function("array_to_string", String.class, propertyExpression, delimiter)),
                    argument.toString().replace('*', '%').toLowerCase());

        } else if (argument instanceof ZonedDateTime) {
            return builder.equal(parsePropertyZonedDateTime(root), argument);
        } else if (argument instanceof LocalDate) {
            return builder.equal(parsePropertyLocalDate(root), (LocalDate) argument);

        } else if (argument instanceof String) {
            return builder.like(builder.lower(propertyExpression),
                    argument.toString().replace('*', '%').toLowerCase());
        } else if (argument == null) {
            return builder.isNull(propertyExpression);
        } else {
            return builder.equal(propertyExpression, argument);
        }
    }

    private <U> Path<U> parsePropertyGeneric(Root<T> root) {
        String[] pathSteps = this.property.split("\\.");
        Path<U> path = root.get(pathSteps[0]);
        From<?, ?> lastFrom = root;

        for (int i = 1; i < pathSteps.length; i++) {
            Attribute<?, ?> attr = (Attribute<?, ?>) path.getModel();

            if (attr.getPersistentAttributeType() != Attribute.PersistentAttributeType.BASIC) {
                Join<?, ?> join = lastFrom.join(attr.getName(), JoinType.LEFT);
                path = join.get(pathSteps[i]);
                lastFrom = join;
            } else {
                path = path.get(pathSteps[i]);
            }
        }

        return path;
    }

    private <U> Path<U> parseProperty(Root<T> root, Class<U> resultType) {
        if (this.property.contains(".")) {
            return parsePropertyGeneric(root);
        } else {
            return root.get(this.property);
        }
    }

    private Path<String> parseProperty(Root<T> root) {
        return parseProperty(root, String.class);
    }

    private Path<ZonedDateTime> parsePropertyZonedDateTime(Root<T> root) {
        return parseProperty(root, ZonedDateTime.class);
    }

    private Path<LocalDate> parsePropertyLocalDate(Root<T> root) {
        return parseProperty(root, LocalDate.class);
    }

    private Path<List> parsePropertyList(Root<T> root) {
        return parseProperty(root, List.class);
    }

    @SuppressWarnings("java:S6204")
    private List<Object> castArguments(Path<?> propertyExpression) {
        Class<?> type = propertyExpression.getJavaType();

        return this.arguments.stream().map(arg -> {
            if (RsqlSearchOperation.getSimpleOperator(this.operator) == RsqlSearchOperation.IS_NULL) {
                return arg;
            } else if (type.equals(Integer.class)) {
                return Integer.parseInt(arg);
            } else if (type.equals(int.class)) {
                return Integer.parseInt(arg);
            } else if (type.equals(Long.class)) {
                return Long.parseLong(arg);
            } else if (type.equals(long.class)) {
                return Long.parseLong(arg);
            } else if (type.equals(Byte.class)) {
                return Byte.parseByte(arg);
            } else if (type.equals(ZonedDateTime.class)) {
                return ZonedDateTime.parse(arg, DateTimeFormatter.ofPattern(dateTimeFormat));
            } else if (type.equals(LocalDate.class)) {
                return LocalDate.parse(arg, DateTimeFormatter.ofPattern(dateTimeFormat));
            } else if (type.equals(Boolean.class)) {
                return arg.equalsIgnoreCase(Boolean.TRUE.toString());
            } else {
                return arg;
            }
        }).collect(Collectors.toList());
    }


}
