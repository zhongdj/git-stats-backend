-- productivity_data schema

-- !Ups

CREATE TABLE `metric` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `day` date NOT NULL,
  `project` varchar(50) COLLATE utf8_bin DEFAULT NULL,
  `developer` varchar(50) COLLATE utf8_bin DEFAULT NULL,
  `metric_name` varchar(50) COLLATE utf8_bin DEFAULT NULL,
  `metric_value` varchar(50) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UNI_INX_D_P_D_M` (`day`,`project`,`developer`,`metric_name`)
) ENGINE=InnoDB AUTO_INCREMENT=10080 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;


CREATE TABLE `task` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `start_date` varchar(20) NOT NULL DEFAULT '',
  `end_date` varchar(20) NOT NULL DEFAULT '',
  `status` varchar(20) NOT NULL DEFAULT 'Draft',
  `task_type` varchar(20) NOT NULL DEFAULT 'OneTime',
  `finger_print` varchar(128) NOT NULL,
  `created_on` bigint(20) NOT NULL,
  `last_modified` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UNI_F` (`finger_print`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `task_item` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `task_id` int(11) NOT NULL,
  `repository_url` varchar(200) NOT NULL DEFAULT '',
  `branch` varchar(200) NOT NULL DEFAULT 'master',
  `status` varchar(20) NOT NULL DEFAULT 'Created',
  `created_on` bigint(20) NOT NULL,
  `last_modified` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `taskId` (`task_id`,`repository_url`,`branch`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- !Downs

DROP TABLE  `metric` ;
DROP TABLE `task`;
DROP TABLE `task_item`;
