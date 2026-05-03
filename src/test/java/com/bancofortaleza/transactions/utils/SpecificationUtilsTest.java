package com.bancofortaleza.transactions.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bancofortaleza.transactions.repository.transactions.entity.TransactionEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;

class SpecificationUtilsTest {

    @Test
    void equalIfNotNullShouldReturnConjunctionWhenValueIsNull() {
        // Arrange
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Predicate conjunction = mock(Predicate.class);
        when(criteriaBuilder.conjunction()).thenReturn(conjunction);

        // Act
        Predicate result = SpecificationUtils
            .<TransactionEntity>equalIfNotNull("status", null)
            .toPredicate(mockRoot(), null, criteriaBuilder);

        // Assert
        assertThat(result).isSameAs(conjunction);
    }

    @Test
    void equalIfNotNullShouldResolveNestedPathWhenValueExists() {
        // Arrange
        Root<TransactionEntity> root = mockRoot();
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path<Object> accountPath = mockPath();
        Path<Object> idPath = mockPath();
        Predicate equalPredicate = mock(Predicate.class);

        when(root.get("account")).thenReturn(accountPath);
        when(accountPath.get("id")).thenReturn(idPath);
        when(criteriaBuilder.equal(idPath, 10)).thenReturn(equalPredicate);

        // Act
        Predicate result = SpecificationUtils
            .<TransactionEntity>equalIfNotNull("account.id", 10)
            .toPredicate(root, null, criteriaBuilder);

        // Assert
        assertThat(result).isSameAs(equalPredicate);
    }

    @Test
    void containsIgnoreCaseShouldReturnConjunctionWhenSearchIsBlank() {
        // Arrange
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Predicate conjunction = mock(Predicate.class);
        when(criteriaBuilder.conjunction()).thenReturn(conjunction);

        // Act
        Predicate result = SpecificationUtils
            .<TransactionEntity>containsIgnoreCase("   ", "description")
            .toPredicate(mockRoot(), null, criteriaBuilder);

        // Assert
        assertThat(result).isSameAs(conjunction);
    }

    @Test
    void containsIgnoreCaseShouldBuildOrPredicateForSearchableFields() {
        // Arrange
        Root<TransactionEntity> root = mockRoot();
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path<Object> descriptionPath = mockPath();
        Path<Object> statusPath = mockPath();
        Expression<String> descriptionExpression = mockExpression();
        Expression<String> statusExpression = mockExpression();
        Predicate firstPredicate = mock(Predicate.class);
        Predicate secondPredicate = mock(Predicate.class);
        Predicate orPredicate = mock(Predicate.class);

        when(root.get("description")).thenReturn(descriptionPath);
        when(root.get("status")).thenReturn(statusPath);
        when(descriptionPath.as(String.class)).thenReturn(descriptionExpression);
        when(statusPath.as(String.class)).thenReturn(statusExpression);
        when(criteriaBuilder.lower(descriptionExpression)).thenReturn(descriptionExpression);
        when(criteriaBuilder.lower(statusExpression)).thenReturn(statusExpression);
        when(criteriaBuilder.like(descriptionExpression, "%salary%")).thenReturn(firstPredicate);
        when(criteriaBuilder.like(statusExpression, "%salary%")).thenReturn(secondPredicate);
        when(criteriaBuilder.or(any(Predicate[].class))).thenReturn(orPredicate);

        // Act
        Predicate result = SpecificationUtils
            .<TransactionEntity>containsIgnoreCase(" Salary ", "description", "status")
            .toPredicate(root, null, criteriaBuilder);

        // Assert
        assertThat(result).isSameAs(orPredicate);
        verify(criteriaBuilder).or(any(Predicate[].class));
    }

    @SuppressWarnings("unchecked")
    private static Root<TransactionEntity> mockRoot() {
        return mock(Root.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> Path<T> mockPath() {
        return mock(Path.class);
    }

    @SuppressWarnings("unchecked")
    private static Expression<String> mockExpression() {
        return mock(Expression.class);
    }
}
