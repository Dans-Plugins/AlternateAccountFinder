create table aaf_login_record(
    address varchar(64) not null,
    minecraft_uuid varchar(36) not null,
    logins int not null,
    first_login datetime not null,
    last_login datetime not null,
    primary key(address, minecraft_uuid)
);
