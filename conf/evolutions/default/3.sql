-- productivity_data schema

-- !Ups
Create View `v_commits_loc_by_day` AS
SELECT commits.day, commits.count, loc_delta.net, commits.project, loc_delta.net / commits.count AS `loc_per_commit`
FROM
(
  SELECT date(`metric`.`day`) AS `day`,
         SUM(`metric`.`metric_value`) AS `net`,
         (`metric`.`project`) AS `project`
  FROM   `metric`
  WHERE  ( ( `metric`.`metric_name` = 'net' ) )
  GROUP  BY date(`metric`.`day`) , (`metric`.`project`)
  ORDER  BY date(`metric`.`day`) ASC
) AS loc_delta

RIGHT JOIN
(
  SELECT date(`git_commit`.`day`) AS `day`, count(distinct `git_commit`.`commit_id`) AS `count` ,  (`git_commit`.`project`) AS `project`
  FROM `git_commit`
  GROUP BY date(`git_commit`.`day`), (`git_commit`.`project`)
  ORDER BY date(`git_commit`.`day`) ASC
) AS commits

ON loc_delta.day = commits.day AND loc_delta.project = commits.project;

Create View `v_commits_loc_by_week` AS
SELECT Str_to_date(Concat(Yearweek(`d`.`day`), 'Sunday'), '%X%V %W') AS `day`,
       `d`.`project`,
       SUM(`d`.count) AS `count`,
       SUM(`d`.net) AS `net`,
       SUM(`d`.net) / SUM(`d`.count) AS `loc_per_commit`
FROM `v_commits_loc_by_day` as `d`
GROUP BY Str_to_date(Concat(Yearweek(`d`.`day`), 'Sunday'), '%X%V %W'), `d`.`project`
ORDER BY Str_to_date(Concat(Yearweek(`d`.`day`), 'Sunday'), '%X%V %W') ASC;


-- !Downs
DROP VIEW `v_commits_loc_by_day`;
DROP VIEW `v_commits_loc_by_week`