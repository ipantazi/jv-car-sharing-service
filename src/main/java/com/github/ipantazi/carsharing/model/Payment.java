package com.github.ipantazi.carsharing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.Arrays;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Table(
        name = "payments",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"rental_id", "type"})}
)
@Entity
@SQLDelete(sql = "UPDATE payments SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long rentalId;

    @Column(length = 1024)
    private String sessionUrl;

    @Column(nullable = false, unique = true, length = 128)
    private String sessionId;

    @Column(nullable = false, precision = 10, scale = 2)
    @Positive
    private BigDecimal amountToPay;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private Status status = Status.PENDING;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private Type type;

    private boolean isDeleted = false;

    public enum Status {
        PENDING, PAID, EXPIRED
    }

    public enum Type {
        PAYMENT,
        FINE;

        public static Type valueOfType(String value) {
            return Arrays.stream(Type.values())
                    .filter(element -> element.name().equalsIgnoreCase(value))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid payment type: "
                            + value));
        }
    }
}
