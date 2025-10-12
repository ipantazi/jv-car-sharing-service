package com.github.ipantazi.carsharing.service.payment.stripe;

public interface StripeSessionMonitorService {
    void checkAndExpireSessions();
}
