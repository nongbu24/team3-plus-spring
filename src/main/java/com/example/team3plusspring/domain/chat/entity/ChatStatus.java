package com.example.team3plusspring.domain.chat.entity;

public enum ChatStatus {
    WAITING,
    IN_PROGRESS,
    COMPLETED;

    public boolean canChange(ChatStatus nextStatus) {
        return switch (this) {
            case WAITING -> nextStatus == IN_PROGRESS;
            case IN_PROGRESS -> nextStatus == COMPLETED;
            case COMPLETED -> false;
        };
    }
}
