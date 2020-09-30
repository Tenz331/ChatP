CREATE TABLE channel (
id INT PRIMARY KEY AUTO_INCREMENT,
name VARCHAR(10) UNIQUE NOT NULL
);

INSERT TABLE channel (id, name) VALUES (1, "main"), (2, "test");

ALTER TABLE log ADD COLUMN channel_id INT NOT NULL DEFAULT (1);
ALTER TABLE log ADD FOREIGN KEY channel_id REFERENCES channel(id);

UPDATE properties
SET VALUE = 1
WHERE name = "version";