package vhu.edu.vn.data.repository;

/** Controlled domain validation error thrown inside Room transactions to trigger clean rollback. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
