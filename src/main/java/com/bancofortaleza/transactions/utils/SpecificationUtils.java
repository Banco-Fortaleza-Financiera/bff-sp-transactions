package com.bancofortaleza.transactions.utils;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class SpecificationUtils {

    private SpecificationUtils() {
    }

    public static <T> Specification<T> equalIfNotNull(String fieldName, Object value) {
        return (root, query, criteriaBuilder) -> {
            if (value == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(resolvePath(root, fieldName), value);
        };
    }

    public static <T> Specification<T> containsIgnoreCase(String search, String... fieldNames) {
        return (root, query, criteriaBuilder) -> {
            if (search == null || search.isBlank() || fieldNames == null || fieldNames.length == 0) {
                return criteriaBuilder.conjunction();
            }

            String searchPattern = "%" + search.trim().toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();
            for (String fieldName : fieldNames) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(resolvePath(root, fieldName).as(String.class)),
                    searchPattern
                ));
            }
            return criteriaBuilder.or(predicates.toArray(Predicate[]::new));
        };
    }

    private static Path<?> resolvePath(Root<?> root, String fieldName) {
        String[] fieldParts = fieldName.split("\\.");
        Path<?> path = root.get(fieldParts[0]);
        for (int index = 1; index < fieldParts.length; index++) {
            path = path.get(fieldParts[index]);
        }
        return path;
    }
}
