UPDATE payments SET rental_id = 101,
                    session_url = 'https://checkout.stripe.com/pay/session_test_id',
                    session_id = 'session 101',
                    amount_to_pay = 505.00,
                    status = 'PAID',
                    type = 'PAYMENT'
WHERE id = 101;
