DROP DATABASE ChadChat;
CREATE DATABASE ChadChat;
use ChadChat;
SET GLOBAL max_allowed_packet = 1024 * 1024 * 1024;
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

INSERT INTO users (username, password) VALUES ("Hvid", SHA2(" ", 256));
INSERT INTO log (user_id, msg, channel_id) VALUES (2, "ggg", 2);

SELECT * FROM (SELECT log.entry_id, users.username, log.timestamp, log.msg, log.channel_id FROM log INNER JOIN users ON log.user_ID = users.ID WHERE channel_id = 2 ORDER BY entry_ID DESC LIMIT 10)sub1 ORDER BY entry_ID ASC;
SELECT * FROM (SELECT * FROM log WHERE log.channel_id = 1 ORDER BY entry_ID DESC LIMIT 10)var1 ORDER BY entry_ID ASC ;
SELECT entry_ID, users.username, timestamp, msg, channel_id FROM (SELECT entry_ID, users.username, timestamp, msg, channel_id FROM log INNER JOIN users ON log.channel_id = users.ID WHERE log.channel_id = 1 ORDER BY entry_ID DESC LIMIT 10)var ORDER BY entry_ID ASC ;
SELECT * FROM (SELECT * FROM log WHERE channel_id = 1 ORDER BY entry_ID DESC LIMIT 10)sub ORDER BY entry_ID ASC;
SELECT * FROM (SELECT * FROM log INNER JOIN users ON log.user_ID = users.ID WHERE channel_id = 1 ORDER BY entry_ID DESC LIMIT 10)sub ORDER BY entry_ID ASC;

SELECT * FROM channel WHERE name = "main";

DELETE FROM log WHERE entry_ID = 2;
