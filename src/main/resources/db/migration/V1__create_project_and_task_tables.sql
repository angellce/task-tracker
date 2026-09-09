create table project
(
    id bigint auto_increment primary key,
    name varchar(255) not null,
    description varchar(255),
    created_at timestamp not null
);

create table task
(
    id bigint auto_increment primary key ,
    title varchar(255),
    description varchar(255),
    status varchar(255),
    priority varchar(255),
    due_date date,
    assignee varchar(255),
    project_id bigint not null references project (id) on delete cascade,
    created_at timestamp not null
);