# Postman Collection Guide

This project includes a ready-made **Postman collection** that allows you to explore and test all **REST API endpoints**
without manual setup.

## 1. Collection Location

The Postman collection is located at:

    postman/JVCarSharingService.postman_collection.json

## 2. Importing the Collection

1. Open **Postman**

2. Click ***Import***

3. Select ***Upload Files***

4. Choose:

```pgsql
postman/JVCarSharingService.postman_collection.json
```

Once imported, all requests will appear grouped into logical folders.

## 3. Included API Coverage

The collection contains:

- Authentication and authorization requests

- Users, cars, rentals, and payments endpoints

- Example request bodies for all ***POST*** and ***PUT*** operations

- Predefined requests for both:

    - ***CUSTOMER***

    - ***MANAGER***

All endpoints are grouped by domain for easy navigation.

## 4. Environment Configuration

By default, the collection uses:

    http://localhost:8080

If the application is deployed remotely (**AWS**, **Docker**, etc.), replace this base ***URL*** with your 
public endpoint.

## 5. Authentication Flow (JWT)

Most endpoints require a valid **JWT token**.

### 5.1 Register a User

```http
POST /api/auth/register
```

### 5.2 Login

```http
POST /api/auth/login
```

The response contains:

```json
{
"token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 5.3 Authorize Requests

Copy the token and add it to the request headers:

```http
Authorization: Bearer <your_token>
```

All secured endpoints in the collection assume this header is present.

## 6. Role-Based Requests

The collection includes requests that require:

- ***CUSTOMER role***

- ***MANAGER role***

Use different users to test role-based access control.