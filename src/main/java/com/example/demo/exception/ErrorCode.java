package com.example.demo.exception;

public enum ErrorCode {
    INVALID_REQUEST(1000, "Invalid request parameters", 400),
    RESOURCE_NOT_FOUND(1001, "Requested resource not found", 404),
    DEVICE_ALREADY_CLAIMED(1003, "Device has already been claimed by another user", 409),
    DEVICE_NOT_FOUND(1004, "Device not found", 404),
    UNAUTHENTICATED(1005, "Authentication is required", 401),
    USER_NOT_EXISTED(1006, "User does not exist", 404),
    ROLE_NOT_EXISTED(1007, "Role does not exist", 404),
    USER_EXISTED(1008, "User already exists", 409),
    INTERNAL_SERVER_ERROR(1002, "Internal server error occurred", 500);

    private final int code;
    private final String message;
    private final int httpStatus;

    ErrorCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
