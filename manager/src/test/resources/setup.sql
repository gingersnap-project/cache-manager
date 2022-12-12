create schema debezium;
create table debezium.customer(id int not null, fullname varchar(255), email varchar(255), constraint primary key (id));
create table debezium.car_model(id int not null, model varchar(255), brand varchar(255), constraint primary key (id));
GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'gingersnap_user';

insert into debezium.customer values (1, 'Jon Doe', 'jd@example.com');
insert into debezium.customer values (3, 'Bob', 'bob@example.com');
insert into debezium.customer values (4, 'Alice', 'alice@example.com');
insert into debezium.customer values (5, 'Mallory', 'mallory@example.com');

insert into debezium.car_model values (1, 'QQ', 'Chery');
insert into debezium.car_model values (2, 'Beetle', 'VW');