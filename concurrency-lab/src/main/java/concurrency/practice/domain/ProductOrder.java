package concurrency.practice.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long inventoryId;
    private int quantity;

    @Version
    private Long version; // 낙관적 락을 위한 버전 필드

    public ProductOrder(Long inventoryId, int quantity) {
        this.inventoryId = inventoryId;
        this.quantity = quantity;
    }

    public void updateQuantity(int newQuantity) {
        this.quantity = newQuantity;
    }
}
