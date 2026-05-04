package com.bancofortaleza.transactions.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class PaginationUtilsTest {

    @Test
    void fromHeadersShouldUseDefaultsWhenHeadersAreMissing() {
        // Arrange / Act
        Pageable pageable = PaginationUtils.fromHeaders(null, null, Sort.by("id"));

        // Assert
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    void fromHeadersShouldConvertOneBasedPageToZeroBasedIndex() {
        // Arrange / Act
        Pageable pageable = PaginationUtils.fromHeaders(3, 15, Sort.unsorted());

        // Assert
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(15);
    }

    @Test
    void fromHeadersShouldClampInvalidAndLargeValues() {
        // Arrange / Act
        Pageable invalidPage = PaginationUtils.fromHeaders(0, 0, Sort.unsorted());
        Pageable largePage = PaginationUtils.fromHeaders(1, 150, Sort.unsorted());

        // Assert
        assertThat(invalidPage.getPageNumber()).isZero();
        assertThat(invalidPage.getPageSize()).isEqualTo(20);
        assertThat(largePage.getPageSize()).isEqualTo(100);
    }
}
