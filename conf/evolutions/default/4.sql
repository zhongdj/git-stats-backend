-- metabase graph

-- !Ups
CREATE TABLE `graph` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `task_id` int(11) unsigned NOT NULL,
  `graph_id` int(11) NOT NULL,
  `graph_name` varchar(500) NOT NULL DEFAULT '',
  `created_on` bigint(20) NOT NULL,
  `last_modified` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UNI_F` (`task_id`, `graph_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- !Downs
DROP TABLE `graph`;
