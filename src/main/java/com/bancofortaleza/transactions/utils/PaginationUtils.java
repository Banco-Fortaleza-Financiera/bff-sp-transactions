package com.bancofortaleza.transactions.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PaginationUtils {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private PaginationUtils() {
    }

    public static Pageable fromHeaders(Integer xPage, Integer xPageSize, Sort sort) {
        return PageRequest.of(toPageIndex(xPage), toPageSize(xPageSize), sort);
    }

    private static int toPageIndex(Integer xPage) {
        if (xPage == null || xPage < DEFAULT_PAGE) {
            return 0;
        }
        return xPage - 1;
    }

    private static int toPageSize(Integer xPageSize) {
        if (xPageSize == null || xPageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(xPageSize, MAX_PAGE_SIZE);
    }
}
