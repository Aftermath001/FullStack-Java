# FullStack-Java

## Database Schema

### Students Table

```sql
CREATE TABLE students (
  studentid BIGINT PRIMARY KEY,
  firstname VARCHAR(100),
  lastname VARCHAR(100),
  dob DATE,
  clazz VARCHAR(50),
  score INT
);
```

This table stores student information with the following fields:
- `studentid`: Primary key (BIGINT)
- `firstname`: Student's first name (VARCHAR(100))
- `lastname`: Student's last name (VARCHAR(100))
- `dob`: Date of birth (DATE)
- `clazz`: Class name (VARCHAR(50))
- `score`: Student's score (INT)