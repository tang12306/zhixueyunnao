-- 常用查询的索引。新库和已有库（基线之后）都会执行。
-- 外键列 MySQL 会自动建索引，这里只补按普通列筛选的查询。

-- 题库按学科、题型筛选（/questions/api/query、组卷按学科和题型抽题、按学科统计题目数）
create index idx_questions_subject_type on questions (subject, type);
