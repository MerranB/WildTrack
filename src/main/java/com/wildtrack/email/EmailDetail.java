package com.wildtrack.email;

public record EmailDetail(
    String recipient,
    String msgBody,
    String subject,
    String attachment
)
{}