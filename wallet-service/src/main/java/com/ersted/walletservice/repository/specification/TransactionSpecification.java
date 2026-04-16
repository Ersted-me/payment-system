package com.ersted.walletservice.repository.specification;

import com.ersted.walletservice.entity.PaymentType;
import com.ersted.walletservice.entity.Transaction;
import com.ersted.walletservice.entity.TransactionStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.UUID;

public class TransactionSpecification {

    public static Specification<Transaction> filter(
            UUID userUuid,
            UUID walletUuid,
            PaymentType type,
            TransactionStatus status,
            OffsetDateTime dateFrom,
            OffsetDateTime dateTo
    ) {
        Specification<Transaction> spec = Specification.where(eq("userUuid", userUuid));
        if (walletUuid != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("wallet").get("uuid"), walletUuid));
        }
        if (type != null) {
            spec = spec.and(eq("type", type));
        }
        if (status != null) {
            spec = spec.and(eq("status", status));
        }
        if (dateFrom != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("created"), dateFrom));
        }
        if (dateTo != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("created"), dateTo));
        }
        return spec;
    }

    private static <T> Specification<Transaction> eq(String field, T value) {
        return value == null ? null : (root, q, cb) -> cb.equal(root.get(field), value);
    }

}
