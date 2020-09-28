use ChadChat;
SET FOREIGN_KEY_CHECKS=0;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS log;
SET FOREIGN_KEY_CHECKS=1;

CREATE TABLE users(
ID INT NOT NULL AUTO_INCREMENT,
username VARCHAR(255) NOT NULL,
password VARCHAR(256) NOT NULL,
 PRIMARY KEY (ID)
);

CREATE TABLE log(
entry_ID INT NOT NULL AUTO_INCREMENT,
user_ID INT,
timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
msg VARCHAR(255) NOT NULL,
PRIMARY KEY(entry_ID),
FOREIGN KEY (user_ID) REFERENCES users(ID)
);  