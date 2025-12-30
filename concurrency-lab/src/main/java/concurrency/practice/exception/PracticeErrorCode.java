package concurrency.practice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum PracticeErrorCode {
    INVENTORY_NOT_FOUND("Inventory not found", "상품 재고 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ORDER_NOT_FOUND("Order not found", "주문 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    OUT_OF_STOCK("Out of stock", "상품의 재고가 부족합니다.", HttpStatus.CONFLICT),
    OPTIMISTIC_LOCK_CONFLICT("Optimistic lock conflict", "다른 사용자가 데이터를 수정했습니다. 다시 시도해주세요.", HttpStatus.CONFLICT);

    private final String error;
    private final String details;
    private final HttpStatus httpStatus;

    PracticeErrorCode(String error, String details, HttpStatus httpStatus) {
        this.error = error;
        this.details = details;
        this.httpStatus = httpStatus;
    }
}
