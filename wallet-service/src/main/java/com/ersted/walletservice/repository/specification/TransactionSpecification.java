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
        return Specification
                .where(eq("userUuid", userUuid))
                .and(walletUuid == null ? null : (root, q, cb) ->
                        cb.equal(root.get("wallet").get("uuid"), walletUuid))
                .and(eq("type", type))
                .and(eq("status", status))
                .and(dateFrom == null ? null : (root, q, cb) ->
                        cb.greaterThanOrEqualTo(root.get("created"), dateFrom))
                .and(dateTo == null ? null : (root, q, cb) ->
                        cb.lessThanOrEqualTo(root.get("created"), dateTo));
    }

    private static <T> Specification<Transaction> eq(String field, T value) {
        return value == null ? null : (root, q, cb) -> cb.equal(root.get(field), value);
    }

}
