package com.github.ipantazi.carsharing.util.stripe;

import com.github.ipantazi.carsharing.dto.payment.StripeSessionMetadataDto;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;

public record StripeTestContextHolder(
        Event event,
        Session session,
        StripeSessionMetadataDto metadataDto
) {
}
