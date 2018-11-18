create table book
(
  id integer not null,
  title varchar(255) not null,
  primary key(id)
);

insert into book values(10,'The Martian');
insert into book values(11,'Blue Mars');
insert into book values(12,'The Case for Mars');
insert into book values(13,'The War of Worlds');
insert into book values(14,'Edison''s Conquest of Mars');