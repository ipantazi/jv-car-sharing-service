# 💳 Stripe Integration & Webhook Configuration

This document describes how **Stripe payments** and **webhooks** are configured in the **jv-car-sharing-service project**, 
both for local development (**ngrok**) and production deployment (**AWS**).

## 1. Required Stripe Configuration

The application requires the following environment variables:

```properties
    STRIPE_SECRET_KEY=sk_test_...
    STRIPE_WEBHOOK_SECRET=whsec_...
```

### ⚠️ Important

The secret key must never be committed to the repository

It must be stored as:

- environment variable

- **GitHub Actions** secret

- **AWS** environment variable (EC2 / ECS)

## 2. Local Development with Ngrok

### 2.1 Install and Configure Ngrok

a. Create a free account:
URL: [https://ngrok.com](https://ngrok.com)

b. Download ngrok:
[https://ngrok.com/download](https://ngrok.com/download)

c. Connect ngrok to your account:

```bash
ngrok config add-authtoken <YOUR_AUTH_TOKEN>
```

In your dashboard, under ***"Your Authtoken"*** copy your authentication token.

### 2.2 Run Application Locally

Start Spring Boot normally (port 8080).

Make sure your webhook endpoint is accessible at:

    http://localhost:8080/webhook/stripe

### 2.3 Start Ngrok Tunnel

```bash
ngrok http 8080
```
Example output:

    Forwarding https://a1b2c3d4.ngrok.io -> http://localhost:8080

Use the HTTPS **ngrok** URL when registering the **Stripe** webhook.

## 3. Creating a Test STRIPE_SECRET_KEY in Stripe

**Stripe** provides separate test and live **API keys**.
For development and automated testing, this project uses test keys only.

### 3.1 Enable Test Mode in Stripe Dashboard

Log in to Stripe Dashboard:
[https://dashboard.stripe.com/](https://dashboard.stripe.com/)

In the top-right corner, enable ***“Test mode”*** (toggle switch).

Once enabled, all generated keys and payments will be non-real and safe for testing.

### 3.2 Locate API Keys

Navigate to:

***Developers → API keys***

URL: [https://dashboard.stripe.com/apikeys](https://dashboard.stripe.com/apikeys)

You will see:

    Publishable key (starts with pk_test_...)

    Secret key (starts with sk_test_...)

### 3.3 Copy the Test Secret Key

Copy the value starting with:

    sk_test_...

This value is used as:

    STRIPE_SECRET_KEY=sk_test_...

## 4. How to Obtain STRIPE_WEBHOOK_SECRET

### Step 1: Navigate to Webhooks

***Developers → Webhooks***

[https://dashboard.stripe.com/webhooks](https://dashboard.stripe.com/webhooks)

### Step 2: Add a Webhook Endpoint

Click ***“Add endpoint”*** and configure:

#### Endpoint URL:

- Local (**ngrok**):

        https://<your-ngrok-url>/webhook/stripe

- Production (**AWS**):

      https://your-domain.com/webhook/stripe


#### Events to send

Required:

```pgsql
checkout.session.completed
```

### Step 3: Copy the Signing Secret

After creating the endpoint:

- Open the ***Webhook***

- Reveal ***Signing secret***

- Copy value starting with:

      whsec_...

Use this value as `STRIPE_WEBHOOK_SECRET`.

## 5. Stripe Webhook Flow (Local)

1. **Stripe Checkout** session is created
2. User completes payment
3. **Stripe** sends `checkout.session.completed` event
4. Event is delivered via **ngrok**
5. Webhook controller validates signature
6. Payment status is updated to `PAID`

## 6. Production Setup (AWS)

After deploying the application to **AWS** (EC2 / ECS):

1. Use your public HTTPS domain:

        https://your-app.example.com/webhook/stripe

2. Create a new webhook endpoint in **Stripe**

3. Generate a new signing secret

4. Update **AWS** environment variables:

```properties
STRIPE_WEBHOOK_SECRET=whsec_...
```

⚠️ Do not reuse **ngrok** secrets in production.

## 7. Testing

### Testing Payments End-to-End

#### Step 1: Create Payment Session

#### Step 2: Open Stripe Checkout URL

You will receive a URL like:

```ruby
https://checkout.stripe.com/c/pay/cs_test_...
```

#### Step 3: Use Stripe Test Card

    4242 4242 4242 4242

- Any future expiration date
- Any CVC
- Any ZIP code

#### Step 4: Verify Result

- Webhook is received

- Payment status becomes `PAID`

- Logs confirm webhook processing

- `/api/payments` endpoint returns updated data

### Optional: Stripe CLI (Alternative to Ngrok)

**Stripe CLI** can forward events directly:

```bash
stripe login
stripe listen --forward-to localhost:8080/webhook/stripe
stripe trigger checkout.session.completed
```

This is useful for rapid local testing without exposing a public URL.

