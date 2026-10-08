CREATE DATABASE IF NOT EXISTS library_db;
USE library_db;

CREATE TABLE IF NOT EXISTS users (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    email          VARCHAR(120) NOT NULL UNIQUE,
    password_hash  CHAR(64)     NOT NULL,
    role           ENUM('LIBRARIAN','MEMBER') NOT NULL DEFAULT 'MEMBER',
    notify_enabled BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS books (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    title            VARCHAR(200) NOT NULL,
    author           VARCHAR(120) NOT NULL,
    genre            VARCHAR(60)  NOT NULL,
    total_copies     INT NOT NULL CHECK (total_copies >= 0),
    available_copies INT NOT NULL CHECK (available_copies >= 0)
);

CREATE TABLE IF NOT EXISTS transactions (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    book_id     INT  NOT NULL,
    member_id   INT  NOT NULL,
    borrow_date DATE NOT NULL,
    due_date    DATE NOT NULL,
    return_date DATE NULL,
    FOREIGN KEY (book_id)   REFERENCES books(id) ON DELETE RESTRICT,
    FOREIGN KEY (member_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notifications (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    member_id  INT NOT NULL,
    message    VARCHAR(400) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_read    BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (member_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS search_history (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    member_id   INT NOT NULL,
    query_text  VARCHAR(200) NOT NULL,
    searched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Sample data (passwords are SHA-256 hashes: admin123 / member123)
INSERT INTO users(name,email,password_hash,role) VALUES
 ('Head Librarian','admin@library.com','240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9','LIBRARIAN'),
 ('Asha Verma','asha@example.com','5600376e863d2f57a053518f324ad3840b0bc2348b573af281a7b7cbe7a228c6','MEMBER');

INSERT INTO books(title,author,genre,total_copies,available_copies) VALUES
 ('Clean Code','Robert C. Martin','Programming',3,3),
 ('Effective Java','Joshua Bloch','Programming',2,2),
 ('The Alchemist','Paulo Coelho','Fiction',4,4),
 ('Sapiens','Yuval Noah Harari','History',2,2),
 ('Wings of Fire','A. P. J. Abdul Kalam','Biography',3,3);
