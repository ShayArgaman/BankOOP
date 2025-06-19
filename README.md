# 🏦 Banking Management System - Database Project

A comprehensive Java application with a robust PostgreSQL backend, demonstrating advanced database concepts and design.

---

## ℹ️ About The Project

This project is a fully functional banking management system developed in Java. It serves as a platform to handle core banking operations, including the management of various account types (Checking, Business, Mortgage, and Savings), detailed client profiles, and the complex relationships between them.

The core of this project is its robust **PostgreSQL database**, which was designed to be efficient, secure, and reliable. The system showcases advanced database features to ensure data integrity and high performance.

## ✨ Key Features

* **💼 Account Management:** Full support for different account types using a database-level ISA (Inheritance) hierarchy.
* **👤 Client Management:** Create, read, update, and delete client profiles.
* **🔗 Rich Relationships:** Manages the many-to-many relationship between clients and accounts.
* **🤖 Advanced Database Automation & Optimization:**
    * **VIEW:** Simplifies complex queries on the account inheritance structure.
    * **TRIGGER:** Provides automatic auditing for sensitive data changes (like client rank).
    * **INDEX:** Ensures high-performance queries for common search operations.
    * **TRANSACTION:** Guarantees data integrity during critical, multi-step operations.

## 🛠️ Tech Stack
* **Language:** Java
* **Database:** PostgreSQL
* **Connectivity:** PostgreSQL JDBC Driver

---

## 🚀 Getting Started

To get the project up and running on your local machine, please follow these steps.

### 📋 Prerequisites

Make sure you have the following software installed:
* Java Development Kit (JDK 17 or later)
* PostgreSQL Server
* An IDE of your choice (e.g., Eclipse, IntelliJ, VS Code)

### ⚙️ Installation and Execution

1.  **Clone the Repository**
    ```sh
    git clone https://github.com/ShayArgaman/BankOOP.git
    ```

2.  **🗄️ Database Setup**
    * Create a new, empty database in PostgreSQL (e.g., `bank_project_db`).
    * Run the provided SQL scripts from the `/sql` folder in the following order:
        1.  `Create_Tables.sql` - This will build the entire schema and insert initial data.
        2.  `Advanced_Features.sql` - This will create the VIEW, TRIGGER, and INDEXes.

3.  **⚠️ CRUCIAL STEP: Configure Application Connection**
    * Before running the Java application, you **must** update the database connection settings.
    * Open the file: `DatabaseManager.java`
    * Locate these lines and edit them with your personal PostgreSQL credentials:

    ```java
    // Database connection configuration - UPDATE THESE VALUES FOR YOUR ENVIRONMENT
    private static final String DB_URL = "jdbc:postgresql://localhost:5433/bank_project_db";  // <-- UPDATE THE PORT NUMBER HERE IF NEEDED
    private static final String DB_USERNAME = "postgres";
    private static final String DB_PASSWORD = "";  // <-- ENTER YOUR POSTGRESQL PASSWORD HERE
    ```

4.  **▶️ Run the Application**
    * Open the project in your IDE.
    * Ensure the PostgreSQL JDBC driver is included in your project's build path/classpath.
    * Run the `Main.java` file to start the application's command-line interface.

