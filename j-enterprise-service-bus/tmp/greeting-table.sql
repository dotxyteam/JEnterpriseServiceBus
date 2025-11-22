CREATE TABLE greeting_history(
   insertion_date DATETIME,
   message VARCHAR
);

INSERT INTO greeting_history(
   insertion_date,
   message
) 
VALUES(
	current_date_time(),
	?
)


SELECT(
   insertion_date,
   message
) FROM greeting_history
