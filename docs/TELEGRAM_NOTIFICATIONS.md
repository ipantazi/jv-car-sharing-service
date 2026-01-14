# Telegram Notifications Configuration

This project uses **Telegram Bot API** to send notifications about rentals, payments, and system events.

## 1. Create a Telegram Bot (BotFather)

a. Open Telegram and search for `@BotFather`

b. Run:

```bash
/newbot
```

c. Follow the prompts:

- Bot name: `CarRentalNotifierBot`

- Username: must end with `bot`
Example: `rental_notifier_name_bot`

d. BotFather will return a token:

    1234567890:ABCdefGhIjKlmnOpQRstUVwxyz

✅ This is your ***TELEGRAM_BOT_TOKEN***

## 2. Create a Chat for Notifications
   
### Option A — Private Chat

1. Open your bot in **Telegram**
2. Click ***Start***
3. Send any message (e.g. `Hi`)

### Option B — Group Chat (Recommended)

1. Create a *Telegram group* (e.g. ***Rental Notifications***)
2. Add your bot to the group
3. Promote the bot to **Admin**
   - Enable ***Can post messages***

## 3. Obtain the Chat ID
   Easiest Method — `@userinfobot`

1. Search for @userinfobot

2. Send:

```bash
/start
```

 - Private chat example:

         Id: 123456789


 - Group chat example:

         Chat ID: -987654321

## 4. Configure Application Properties

```properties
telegram.bot-token=1234567890:ABCdefGhIjKlmnOpQRstUVwxyz
telegram.chat-id=-987654321
```

### These values should be provided via:

- `.env` (local)

- **GitHub Actions** secrets

- **AWS** environment variables

## 5. Rate Limiting with Resilience4j

To prevent **Telegram API** blocking due to mass message delivery, the project uses **Resilience4j RateLimiter**.

Dependency:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
</dependency>
```

### This ensures:

- Controlled message throughput
- System stability under load
- Graceful degradation
