# Spring Boot E-Commerce API

## Project Overview
This project is a RESTful API built using Spring Boot, designed to handle various e-commerce functionalities such as product management, user authentication, and order processing.

## Features
- **User Management**: Register, login, and manage user profiles.
- **Product Management**: Add, update, and delete products.
- **Order Management**: Create, update, and view orders.
- **Shopping Cart**: Add products to a cart and checkout.

## Tech Stack
- **Languages**: Java
- **Frameworks**: Spring Boot, Spring Security
- **Database**: MySQL
- **Build Tool**: Maven

## Setup Instructions
1. Clone the repository: 
   ```bash
   https://github.com/jackson951/api.streetluxciry.git
   cd api.streetluxciry
   ```
2. Update application properties with your database credentials in `src/main/resources/application.properties`.
3. Run the application: 
   ```bash
   mvn spring-boot:run
   ```

## API Documentation
- **Base URL**: `http://localhost:8080/api`
- **Endpoints**: 
  - `POST /users`: Registers a new user.
  - `POST /login`: Logs in a user.
  - `GET /products`: Retrieves a list of products.
  - `POST /orders`: Creates a new order.

## Usage Examples
- To register a user, send a POST request to `/users` with the necessary data:
  ```json
  {
      "username": "user1",
      "password": "password123"
  }
  ```
- For detailed API usage, refer to the API documentation link.
