-- productivity_data schema

-- !Ups
CREATE TABLE `git_commit` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `project` varchar(100) NOT NULL,
    `commit_id` VARCHAR(50) NOT NULL,
    `file` VARCHAR(400) NOT NULL,
    `developer` varchar(50) NOT NULL,
    `day` date NOT NULL,
    `message` TEXT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UNI_INX_P_C_F` (`project`,`commit_id`,`file`)
  ) ENGINE=InnoDB AUTO_INCREMENT=10080 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `tagged_commit` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `day` date NOT NULL,
  `tag` VARCHAR(100) NOT NULL,
  `project` varchar(100) NOT NULL,
  `commit_id` VARCHAR(50) NOT NULL,
  `file` VARCHAR(400) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UNI_P_C_F` (`tag`, `project`,`commit_id`,`file`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- !Downs

DROP TABLE `git_commit`;
DROP TABLE `tagged_commit`;
