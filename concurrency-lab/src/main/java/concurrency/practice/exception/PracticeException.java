package concurrency.practice.exception;

import lombok.Getter;

@Getter
public class PracticeException extends RuntimeException {
    private final PracticeErrorCode errorCode;

    public PracticeException(PracticeErrorCode errorCode) {
        super(errorCode.getDetails());
        this.errorCode = errorCode;
    }
}
