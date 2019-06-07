-- productivity_data schema

-- !Ups

CREATE TABLE `productivity_data` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `day` date NOT NULL,
  `project` varchar(50) COLLATE utf8_bin DEFAULT NULL,
  `developer` varchar(50) COLLATE utf8_bin DEFAULT NULL,
  `metric_name` varchar(50) COLLATE utf8_bin DEFAULT NULL,
  `metric_value` varchar(50) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10000 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

SELECT * FROM `productivity_data`;
-- !Downs

DROP TABLE  `productivity_data` ;