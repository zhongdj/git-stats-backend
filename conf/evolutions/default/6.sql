-- function metrics

-- !Ups
ALTER TABLE func_metric ADD CONSTRAINT t_d_n_p UNIQUE (`task_id`,`task_item_id`, `day`,`name`,`path`);

-- !Downs
ALTER TABLE func_metric DROP CONSTRAINT t_d_n_p;
