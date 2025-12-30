package concurrency.practice.domain;

import concurrency.practice.exception.PracticeException;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static concurrency.practice.exception.PracticeErrorCode.OUT_OF_STOCK;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;
    private long stock;

    public Inventory(String productName, long stock) {
        this.productName = productName;
        this.stock = stock;
    }

    public void decrease(long quantity) {
        if (this.stock - quantity < 0) {
            throw new PracticeException(OUT_OF_STOCK);
        }
        this.stock -= quantity;
    }
}
