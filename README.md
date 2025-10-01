
# KYC API

This project is a Spring Boot API for KYC (Know Your Customer) records.

Features:
- Create, Update, Delete, Search KYC records
- Photo attachments stored on filesystem (configurable)
- JWT authentication (demo)
- Pagination and filtering
- DTO validation
- Swagger / OpenAPI

Run:
1. configure `src/main/resources/application.yml`
2. create Postgres database `kyc_db`
3. mvn spring-boot:run

Postman collection included at `postman_collection.json`
