update users set email = '101@example.com',
                 password = '$2a$10$HmJz/wZv5WFjJArzq90dTOcGmYMsYtd.x61Z6qsgNoXTgtSJceOqe',
                 first_name = 'FirstName',
                 last_name = 'LastName',
                 role = 'CUSTOMER',
                 is_deleted = 0
where id = 101;

update users set email = '102@example.com',
                 password = '$2a$10$HmJz/wZv5WFjJArzq90dTOcGmYMsYtd.x61Z6qsgNoXTgtSJceOqe',
                 first_name = 'FirstName',
                 last_name = 'LastName',
                 role = 'MANAGER',
                 is_deleted = 0
where id = 102;
