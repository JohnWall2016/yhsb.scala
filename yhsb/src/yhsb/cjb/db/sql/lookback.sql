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
    reserve2 varchar(32)
);

create table jb_data(
    idcard varchar(32) primary key,
    name varchar(32),
    address varchar(128),
    bank_name varchar(32),
    card_number varchar(32),
    data_type varchar(32), /** 居保 */
    reserve1 varchar(32),
    reserve2 varchar(32)
);

create table qmcb_data(
    idcard varchar(32) primary key,
    name varchar(32),
    address varchar(128),
    bank_name varchar(32),
    card_number varchar(32),
    data_type varchar(32), /** 全民参保 */
    reserve1 varchar(32),
    reserve2 varchar(32)
);