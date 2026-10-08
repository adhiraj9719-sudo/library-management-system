package com.library.model;

import java.time.LocalDateTime;

public record Notification(int id, int memberId, String message, LocalDateTime createdAt, boolean read) { }
