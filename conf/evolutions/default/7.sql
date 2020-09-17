-- function metrics delta

-- !Ups
CREATE TABLE `func_metric_delta` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `task_id` int(11) NOT NULL,
  `task_item_id` int(11) NOT NULL,
  `day` date NOT NULL,
  `name` varchar(255) NOT NULL DEFAULT '',
  `file` varchar(400) NOT NULL DEFAULT '',
  `project_root` varchar(400) NOT NULL DEFAULT '',
  `path` varchar(400) NOT NULL DEFAULT '',
  `lines` int(11) NOT NULL DEFAULT '0',
  `params` int(11) NOT NULL DEFAULT '0',
  `complexity` int(11) NOT NULL DEFAULT '0',
  `complexity_per_line` float NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `t_d_n_p` (`task_id`,`task_item_id`,`day`,`name`,`path`),
  KEY `TASK_DAY_NAME_PATH` (`task_id`,`day`,`name`,`path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- !Downs
DROP TABLE `func_metric_delta`;
