drop table if exists file_object;

create table file_object(
	url varchar(100) primary key ,
	name varchar(100),
	creator varchar(30),
	create_time datetime,
	ext varchar(30),
	size_ bigint,
	size_str varchar(20),
	target_class varchar(120),
	target_id varchar(36)
);
