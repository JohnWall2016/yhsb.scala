create database CompareDB2021;

use CompareDB2021;

DROP TABLE IF EXISTS `jbrymx`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `jbrymx` (
  `idcard` varchar(18) NOT NULL,
  `Xzqh` longtext CHARACTER SET utf8mb4,
  `Hjxz` longtext CHARACTER SET utf8mb4,
  `Name` longtext CHARACTER SET utf8mb4,
  `Sex` longtext CHARACTER SET utf8mb4,
  `BirthDay` longtext CHARACTER SET utf8mb4,
  `Cbsf` longtext CHARACTER SET utf8mb4,
  `Cbzt` longtext CHARACTER SET utf8mb4,
  `Jfzt` longtext CHARACTER SET utf8mb4,
  `Cbsj` longtext CHARACTER SET utf8mb4,
  `Jbzt` longtext CHARACTER SET utf8mb4,
  `Memo` longtext CHARACTER SET utf8mb4,
  PRIMARY KEY (`idcard`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

drop table if exists jgsbrymx;
create table jgsbrymx (
    idcard varchar(18) not null,
    name longtext CHARACTER SET utf8mb4,
    area longtext CHARACTER SET utf8mb4,
    memo longtext CHARACTER SET utf8mb4,
    primary key(idcard)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

drop view if exists jgsbdup;
create view jgsbdup as 
select a.idcard as idcard,
       a.name as name,
       a.xzqh as xzqh,
       substring(a.idcard, 7, 8) as birthday,
       a.jbzt as jbzt,
       a.cbsf as jbsf,
       b.name as jgsb_name,
       b.area as jgsb_area
from jbrymx a, jgsbrymx b where a.idcard=b.idcard
order by CONVERT( a.xzqh USING gbk ), CONVERT( a.name USING gbk );

create table table5_1 (
  f1 varchar(18) not null,
  f2 longtext CHARACTER SET utf8mb4,
  f3 longtext CHARACTER SET utf8mb4,
  f4 longtext CHARACTER SET utf8mb4,
  f5 longtext CHARACTER SET utf8mb4,
  primary key(f1)
);

create table table5_2 (
  f1 varchar(18) not null,
  f2 longtext CHARACTER SET utf8mb4,
  f3 longtext CHARACTER SET utf8mb4,
  f4 longtext CHARACTER SET utf8mb4,
  f5 longtext CHARACTER SET utf8mb4,
  primary key(f1)
);
