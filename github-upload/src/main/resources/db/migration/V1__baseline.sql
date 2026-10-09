-- 基线：引入 Flyway 时的完整表结构，由 Hibernate 6.5 按 MySQLDialect 从实体类导出。
-- 以前靠 ddl-auto=update 建好表的数据库不会执行这个脚本：首次启动时 Flyway 把它记为基线版本（baseline-on-migrate）。
-- 以后改表结构请新增 V2__xxx.sql、V3__xxx.sql，不要修改已经发布的脚本。

create table chapters (
    id bigint not null auto_increment,
    description varchar(255),
    name varchar(255) not null,
    orderNum integer,
    subject_id bigint,
    primary key (id)
) engine=InnoDB;

create table classes (
    id bigint not null auto_increment,
    grade integer,
    name varchar(255) not null,
    studentCount integer,
    major_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table colleges (
    id bigint not null auto_increment,
    description varchar(255),
    name varchar(255) not null,
    school_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table exam_class (
    exam_id bigint not null,
    class_id bigint not null
) engine=InnoDB;

create table exam_question (
    exam_id bigint not null,
    question_id bigint not null
) engine=InnoDB;

create table exams (
    id bigint not null auto_increment,
    createTime datetime(6),
    creator varchar(255),
    duration integer,
    examDate datetime(6),
    examType varchar(255),
    name varchar(255) not null,
    status varchar(255),
    totalScore integer,
    subject_id bigint,
    primary key (id)
) engine=InnoDB;

create table major_subject (
    major_id bigint not null,
    subject_id bigint not null
) engine=InnoDB;

create table majors (
    id bigint not null auto_increment,
    code varchar(255),
    description varchar(255),
    name varchar(255) not null,
    college_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table paper_questions (
    id bigint not null auto_increment,
    orderNum integer,
    score integer not null,
    paper_id bigint not null,
    question_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table papers (
    id bigint not null auto_increment,
    createdAt datetime(6),
    createdBy varchar(255),
    description varchar(255),
    duration integer,
    questionCount integer,
    subject varchar(255),
    title varchar(255) not null,
    totalScore integer,
    updatedAt datetime(6),
    primary key (id)
) engine=InnoDB;

create table question_options (
    question_id bigint not null,
    option_text varchar(255)
) engine=InnoDB;

create table question_tags (
    question_id bigint not null,
    tag varchar(255)
) engine=InnoDB;

create table questions (
    id bigint not null auto_increment,
    analysis TEXT,
    answer TEXT not null,
    content TEXT not null,
    difficulty integer not null,
    score integer,
    subject varchar(255) not null,
    title varchar(255),
    type enum ('FILL_IN_THE_BLANK','MULTIPLE_CHOICE','SHORT_ANSWER','SINGLE_CHOICE','TRUE_FALSE') not null,
    chapter_id bigint,
    primary key (id)
) engine=InnoDB;

create table reflection_reports (
    id bigint not null auto_increment,
    correctAnswerAnalysis TEXT,
    generatedAt datetime(6),
    mistakeAnalysis TEXT,
    practiceProblems TEXT,
    weaknessAnalysis TEXT,
    score_id bigint,
    user_id bigint,
    primary key (id)
) engine=InnoDB;

create table schools (
    id bigint not null auto_increment,
    address varchar(255),
    description varchar(255),
    name varchar(255) not null,
    primary key (id)
) engine=InnoDB;

create table scores (
    id bigint not null auto_increment,
    comment varchar(255),
    createTime datetime(6),
    creator varchar(255),
    student_rank integer,
    score float(53) not null,
    exam_id bigint,
    user_id bigint,
    primary key (id)
) engine=InnoDB;

create table subjects (
    id bigint not null auto_increment,
    description varchar(255),
    name varchar(255) not null,
    primary key (id)
) engine=InnoDB;

create table system_settings (
    setting_key varchar(100) not null,
    setting_category varchar(100),
    setting_description varchar(512),
    is_editable bit not null,
    setting_name varchar(255),
    setting_value TEXT,
    primary key (setting_key)
) engine=InnoDB;

create table users (
    id bigint not null auto_increment,
    birthday date,
    email varchar(255),
    enabled bit not null,
    gender varchar(255),
    name varchar(255),
    password varchar(255) not null,
    phone varchar(255),
    role varchar(255) not null,
    username varchar(255) not null,
    class_id bigint,
    primary key (id)
) engine=InnoDB;

alter table reflection_reports add constraint UKk4hb8pvcp9n24y5n2dwmedmk unique (score_id);
alter table schools add constraint UKehwqlfa7xseucba45p6wlqfgn unique (name);
alter table subjects add constraint UKaodt3utnw0lsov4k9ta88dbpr unique (name);
alter table users add constraint UKr43af9ap4edm43mmtq01oddj6 unique (username);

alter table chapters add constraint FK3rm6snrkx0k8xyqn7017b0v41 foreign key (subject_id) references subjects (id);
alter table classes add constraint FK6r9qmxcnxge92jgx4x4gltf0o foreign key (major_id) references majors (id);
alter table colleges add constraint FKb3g6j9qmjse7lxuexg6332kl4 foreign key (school_id) references schools (id);
alter table exam_class add constraint FKgw8g9la9pbsvqsheiqu8o0fs0 foreign key (class_id) references classes (id);
alter table exam_class add constraint FK5xe1h088kh1cybdgcdsqhdfg8 foreign key (exam_id) references exams (id);
alter table exam_question add constraint FKr3wyfnkav2kjirb09n48n8q2v foreign key (question_id) references questions (id);
alter table exam_question add constraint FK7via7ws322avfbl1qx7jhnpt1 foreign key (exam_id) references exams (id);
alter table exams add constraint FKopre4n7j7fpxqbtbwpv8ywn1y foreign key (subject_id) references subjects (id);
alter table major_subject add constraint FKam2cq6a3c5rt41jd3plvu635t foreign key (subject_id) references subjects (id);
alter table major_subject add constraint FKjrithj3ohfe79pj96r88lu3hs foreign key (major_id) references majors (id);
alter table majors add constraint FKj7jy4f1gjj1v0pr66q42jxm0g foreign key (college_id) references colleges (id);
alter table paper_questions add constraint FKi9r7tupkli9axmayx7s5jgtt5 foreign key (paper_id) references papers (id);
alter table paper_questions add constraint FKie1rdvgocdlp1w33sh2xdr6sr foreign key (question_id) references questions (id);
alter table question_options add constraint FKsb9v00wdrgc9qojtjkv7e1gkp foreign key (question_id) references questions (id);
alter table question_tags add constraint FKee6kn1hbh2ka2qj64bv30esbw foreign key (question_id) references questions (id);
alter table questions add constraint FKd1wulherkir0s9abbqr195fr4 foreign key (chapter_id) references chapters (id);
alter table reflection_reports add constraint FK1uo2hqa0y5fi665oxk7wf530t foreign key (score_id) references scores (id);
alter table reflection_reports add constraint FKg8s8o6oebv0og6kiqat6t2th8 foreign key (user_id) references users (id);
alter table scores add constraint FK32g8we32gx474l7jy6sl0vy3 foreign key (exam_id) references exams (id);
alter table scores add constraint FKtkgoiahryd4yntgywbqyyw8o8 foreign key (user_id) references users (id);
alter table users add constraint FKteip88j90fbo9odnc9gxpq6n1 foreign key (class_id) references classes (id);
