create table person.addresses_aud
(
    id         uuid    not null,
    rev        integer not null,
    revtype    smallint,
    address    varchar(128),
    archived   timestamp(6) with time zone,
    city       varchar(32),
    created    timestamp(6) with time zone,
    state      varchar(32),
    updated    timestamp(6) with time zone,
    zip_code   varchar(32),
    country_id integer,
    primary key (rev, id)
);
alter table if exists person.individuals alter column status set data type varchar (255);

create table person.individuals_aud
(
    id              uuid    not null,
    rev             integer not null,
    revtype         smallint,
    archived_at     timestamp(6) with time zone,
    passport_number varchar(32),
    phone_number    varchar(32),
    status          varchar(255) check ((status in ('PENDING', 'ACTIVE', 'ARCHIVED'))),
    verified_at     timestamp(6) with time zone,
    user_id         uuid,
    primary key (rev, id)
);

create table person.users_aud
(
    id         uuid    not null,
    rev        integer not null,
    revtype    smallint,
    created    timestamp(6) with time zone,
    email      varchar(1024),
    filled     boolean,
    first_name varchar(32),
    last_name  varchar(32),
    secret_key varchar(32),
    updated    timestamp(6) with time zone,
    address_id uuid,
    primary key (rev, id)
);

create table person.revinfo
(
    rev      integer not null,
    revtstmp bigint,
    primary key (rev)
);

create sequence person.revinfo_seq start with 1 increment by 50;

alter table if exists person.addresses_aud add constraint FK6mk9kq602r09b8m1t10w1l9ub foreign key (rev) references person.revinfo;
alter table if exists person.individuals_aud add constraint FK5ue4g3yeqq1pd2tyhsaxcoars foreign key (rev) references person.revinfo;
alter table if exists person.users_aud add constraint FKc4vk4tui2la36415jpgm9leoq foreign key (rev) references person.revinfo;