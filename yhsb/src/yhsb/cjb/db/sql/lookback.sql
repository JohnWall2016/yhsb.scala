create database lookback2021;

use lookback2021;

create table cards_data(
    idcard varchar(32) primary key,
    name varchar(32),
    address varchar(128),
    bank_name varchar(32),
    card_number varchar(32),
    data_type varchar(32), /** 社保卡 */
    reserve1 varchar(32),
    reserve2 varchar(32),
    reserve3 varchar(32)
);

alter table cards_data add reserve3 varchar(32);

create table jb_data(
    idcard varchar(32) primary key,
    name varchar(32),
    address varchar(128),
    bank_name varchar(32),
    card_number varchar(32),
    data_type varchar(32), /** 居保 */
    reserve1 varchar(32),
    reserve2 varchar(32),
    reserve3 varchar(32)
);

alter table jb_data add reserve3 varchar(32);

update jb_data as a, cards_data as b set a.bank_name=b.bank_name, a.card_number=b.card_number where a.idcard=b.idcard;

create table qmcb_data(
    idcard varchar(32) primary key,
    name varchar(32),
    address varchar(128),
    bank_name varchar(32),
    card_number varchar(32),
    data_type varchar(32), /** 全民参保 */
    reserve1 varchar(32),
    reserve2 varchar(32),
    reserve3 varchar(32)
);

alter table qmcb_data add reserve3 varchar(32);

update qmcb_data as a, cards_data as b set a.bank_name=b.bank_name, a.card_number=b.card_number where a.idcard=b.idcard;

create table union_data(
    idcard varchar(32) primary key,
    name varchar(32),
    address varchar(128),
    bank_name varchar(32),
    card_number varchar(32),
    data_type varchar(32),
    reserve1 varchar(32),
    reserve2 varchar(32),
    reserve3 varchar(32)
);

create table police_data(
    idcard varchar(32) primary key,
    name varchar(32),
    address varchar(128),
    bank_name varchar(32),
    card_number varchar(32),
    data_type varchar(32), /** 公安 */
    reserve1 varchar(32),
    reserve2 varchar(32),
    reserve3 varchar(32)
);

create table retired_data(
    area_code varchar(32),
    idcard varchar(32) primary key,
    name varchar(32),
    address varchar(128),
    card_number varchar(32),
    bank_name varchar(32),
    card_type varchar(32),
    retired_type varchar(32),
    stop_time varchar(32),
    phone varchar(32),
    old_address varchar(128)
);


insert into union_data select * from jb_data;

insert into union_data select * from cards_data where idcard not in (select idcard from jb_data);

insert into union_data select * from qmcb_data on duplicate key update union_data.idcard=union_data.idcard;

select count(*) from union_data;

select count(*) from retired_data;

select * from retired_data limit 100;

select * from union_data where address like '%雨湖路街道%大埠桥社区%';

select count(*) from union_data where reserve1 is not null and reserve1<>'';

select data_type, count(*) from union_data where reserve1 is not null and reserve1<>'' group by data_type;

select reserve1, reserve2, count(*) from union_data where reserve1 is not null and reserve1<>'' group by reserve1, reserve2;

select count(*) from police_data;

select count(*) from police_data as a, jb_data as b where a.idcard=b.idcard;

select count(*) from police_data as a, cards_data as b where a.idcard=b.idcard;

select count(*) from police_data as a where a.address like '%雨湖区%';

select count(*) from police_data where address like '%响水乡%' or address like '%和平街道%' or address like '%雨湖区红旗社区%' or address like '%九华%';

select count(*) from police_data as a where a.address like '%雨湖区红旗社区%' or a.address like '%九华%';

select count(*) from police_data as a where a.address like '%九华%';

select * from police_data as a where a.address like '%响水%' and a.address not like '%响水乡%';


select * from police_data as a where a.address like '%响水乡%' or a.address like '%和平
街道%' into outfile 'outfile.csv' fields terminated by ',';