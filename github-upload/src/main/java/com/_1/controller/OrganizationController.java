package com._1.controller;

import com._1.entity.ClassEntity;
import com._1.entity.College;
import com._1.entity.Major;
import com._1.entity.School;
import com._1.entity.Subject;
import com._1.entity.User;
import com._1.service.ClassService;
import com._1.service.CollegeService;
import com._1.service.MajorService;
import com._1.service.SchoolService;
import com._1.service.SubjectService;
import com._1.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/organization")
public class OrganizationController {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationController.class);

    @Autowired private SchoolService schoolService;
    @Autowired private CollegeService collegeService;
    @Autowired private MajorService majorService;
    @Autowired private ClassService classService;
    @Autowired private SubjectService subjectService;
    @Autowired private UserService userService;

    @GetMapping
    public String organizationIndex() {
        return "redirect:/organization/schools";
    }

    // ==== 学校管理 ====
    @GetMapping("/schools")
    public String listSchools(Model model) {
        try {
            model.addAttribute("schools", schoolService.findAll());
        } catch (Exception e) {
            logger.error("Error listing schools", e);
            model.addAttribute("errorMessage", "加载学校列表失败: " + e.getMessage());
        }
        return "organization/school_list"; // View for listing schools
    }

    @GetMapping("/schools/form")
    public String schoolForm(@RequestParam(required = false) Long id, Model model) {
        try {
            School school = (id != null) ? schoolService.findById(id).orElse(new School()) : new School();
            model.addAttribute("school", school);
        } catch (Exception e) {
            logger.error("Error loading school form for id: {}", id, e);
            model.addAttribute("errorMessage", "加载学校表单失败: " + e.getMessage());
            model.addAttribute("school", new School()); // Provide an empty object for the form
        }
        return "organization/school_form"; // View for school form (create/edit)
    }

    @PostMapping("/schools/save")
    public String saveSchool(@ModelAttribute School school, Model model) {
        try {
            schoolService.save(school);
            return "redirect:/organization/schools";
        } catch (Exception e) {
            logger.error("Error saving school: {}", school, e);
            model.addAttribute("errorMessage", "保存学校失败: " + e.getMessage());
            model.addAttribute("school", school); // Keep data in form
            return "organization/school_form";
        }
    }

    @DeleteMapping("/schools/delete/{id}")
    public ResponseEntity<?> deleteSchool(@PathVariable Long id) {
        try {
            schoolService.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting school with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("删除学校失败: " + e.getMessage());
        }
    }

    // ==== 学院管理 ====
    @GetMapping("/colleges")
    public String listColleges(@RequestParam(required = false) Long schoolId, Model model) {
        try {
            if (schoolId != null) {
                School school = schoolService.findById(schoolId).orElse(null);
                if (school != null) {
                    model.addAttribute("colleges", collegeService.findBySchool(school));
                    model.addAttribute("selectedSchool", school);
                }
            } else {
                model.addAttribute("colleges", collegeService.findAll());
            }
            model.addAttribute("schools", schoolService.findAll()); // For dropdown filter
        } catch (Exception e) {
            logger.error("Error listing colleges for schoolId: {}", schoolId, e);
            model.addAttribute("errorMessage", "加载学院列表失败: " + e.getMessage());
        }
        return "organization/college_list";
    }

    @GetMapping("/colleges/form")
    public String collegeForm(@RequestParam(required = false) Long id, @RequestParam(required = false) Long schoolId, Model model) {
        try {
            College college = (id != null) ? collegeService.findById(id).orElse(new College()) : new College();
            if (college.getSchool() == null && schoolId != null) { // Pre-select school for new college if schoolId is provided
                schoolService.findById(schoolId).ifPresent(college::setSchool);
            }
            model.addAttribute("college", college);
            model.addAttribute("schools", schoolService.findAll());
        } catch (Exception e) {
            logger.error("Error loading college form for id: {} or schoolId: {}", id, schoolId, e);
            model.addAttribute("errorMessage", "加载学院表单失败: " + e.getMessage());
            model.addAttribute("college", new College());
            model.addAttribute("schools", schoolService.findAll());
        }
        return "organization/college_form";
    }

    @PostMapping("/colleges/save")
    public String saveCollege(@ModelAttribute College college, @RequestParam Long schoolId, Model model) {
        try {
            School school = schoolService.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("无效的学校ID: " + schoolId));
            college.setSchool(school);
            collegeService.save(college);
            return "redirect:/organization/colleges?schoolId=" + schoolId;
        } catch (Exception e) {
            logger.error("Error saving college: {}", college, e);
            model.addAttribute("errorMessage", "保存学院失败: " + e.getMessage());
            model.addAttribute("college", college);
            model.addAttribute("schools", schoolService.findAll());
            return "organization/college_form";
        }
    }

    @DeleteMapping("/colleges/delete/{id}")
    public ResponseEntity<?> deleteCollege(@PathVariable Long id) {
        try {
            collegeService.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting college with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("删除学院失败: " + e.getMessage());
        }
    }

    // ==== 专业管理 ====
    @GetMapping("/majors")
    public String listMajors(@RequestParam(required = false) Long collegeId, Model model) {
        try {
            if (collegeId != null) {
                College college = collegeService.findById(collegeId).orElse(null);
                if (college != null) {
                    model.addAttribute("majors", majorService.findByCollege(college));
                    model.addAttribute("selectedCollege", college);
                }
            } else {
                model.addAttribute("majors", majorService.findAll());
            }
            model.addAttribute("colleges", collegeService.findAll()); // For dropdown filter
        } catch (Exception e) {
            logger.error("Error listing majors for collegeId: {}", collegeId, e);
            model.addAttribute("errorMessage", "加载专业列表失败: " + e.getMessage());
        }
        return "organization/major_list";
    }

    @GetMapping("/majors/form")
    public String majorForm(@RequestParam(required = false) Long id, @RequestParam(required = false) Long collegeId, Model model) {
        try {
            Major major = (id != null) ? majorService.findById(id).orElse(new Major()) : new Major();
            if (major.getCollege() == null && collegeId != null) { // Pre-select college
                collegeService.findById(collegeId).ifPresent(major::setCollege);
            }
            model.addAttribute("major", major);
            model.addAttribute("colleges", collegeService.findAll());
            model.addAttribute("allSubjects", subjectService.findAll());
        } catch (Exception e) {
            logger.error("Error loading major form for id: {} or collegeId: {}", id, collegeId, e);
            model.addAttribute("errorMessage", "加载专业表单失败: " + e.getMessage());
            model.addAttribute("major", new Major());
            model.addAttribute("colleges", collegeService.findAll());
            model.addAttribute("allSubjects", subjectService.findAll());
        }
        return "organization/major_form";
    }

    @PostMapping("/majors/save")
    public String saveMajor(@ModelAttribute Major major, 
                            @RequestParam Long collegeId, 
                            @RequestParam(required = false) List<Long> subjectIds,
                            Model model) {
        try {
            College college = collegeService.findById(collegeId)
                .orElseThrow(() -> new IllegalArgumentException("无效的学院ID: " + collegeId));
            major.setCollege(college);

            if (subjectIds != null) {
                List<Subject> selectedSubjects = subjectService.findAllById(subjectIds);
                major.setSubjects(selectedSubjects);
            }

            majorService.save(major);
            return "redirect:/organization/majors?collegeId=" + collegeId;
        } catch (Exception e) {
            logger.error("Error saving major: {}", major, e);
            model.addAttribute("errorMessage", "保存专业失败: " + e.getMessage());
            model.addAttribute("major", major);
            model.addAttribute("colleges", collegeService.findAll());
            model.addAttribute("allSubjects", subjectService.findAll());
            return "organization/major_form";
        }
    }

    @DeleteMapping("/majors/delete/{id}")
    public ResponseEntity<?> deleteMajor(@PathVariable Long id) {
        try {
            majorService.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting major with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("删除专业失败: " + e.getMessage());
        }
    }

    // ==== 班级管理 ====
    @GetMapping("/classes")
    public String listClasses(@RequestParam(required = false) Long majorId, Model model) {
        try {
            if (majorId != null) {
                Major major = majorService.findById(majorId).orElse(null);
                if (major != null) {
                    model.addAttribute("classes", classService.findByMajor(major));
                    model.addAttribute("selectedMajor", major);
                }
            } else {
                model.addAttribute("classes", classService.findAll());
            }
            model.addAttribute("majors", majorService.findAll()); // For dropdown filter
        } catch (Exception e) {
            logger.error("Error listing classes for majorId: {}", majorId, e);
            model.addAttribute("errorMessage", "加载班级列表失败: " + e.getMessage());
        }
        return "organization/class_list";
    }

    @GetMapping("/classes/form")
    public String classForm(@RequestParam(required = false) Long id, @RequestParam(required = false) Long majorId, Model model) {
        try {
            ClassEntity classEntity = (id != null) ? classService.findById(id).orElse(new ClassEntity()) : new ClassEntity();
            if (classEntity.getMajor() == null && majorId != null) { // Pre-select major
                majorService.findById(majorId).ifPresent(classEntity::setMajor);
            }
            model.addAttribute("classEntity", classEntity);
            model.addAttribute("majors", majorService.findAll());
        } catch (Exception e) {
            logger.error("Error loading class form for id: {} or majorId: {}", id, majorId, e);
            model.addAttribute("errorMessage", "加载班级表单失败: " + e.getMessage());
            model.addAttribute("classEntity", new ClassEntity());
            model.addAttribute("majors", majorService.findAll());
        }
        return "organization/class_form";
    }

    @PostMapping("/classes/save")
    public String saveClass(@ModelAttribute ClassEntity classEntity, @RequestParam Long majorId, Model model) {
        try {
            Major major = majorService.findById(majorId)
                .orElseThrow(() -> new IllegalArgumentException("无效的专业ID: " + majorId));
            classEntity.setMajor(major);
            classService.save(classEntity);
            return "redirect:/organization/classes?majorId=" + majorId;
        } catch (Exception e) {
            logger.error("Error saving class: {}", classEntity, e);
            model.addAttribute("errorMessage", "保存班级失败: " + e.getMessage());
            model.addAttribute("classEntity", classEntity);
            model.addAttribute("majors", majorService.findAll());
            return "organization/class_form";
        }
    }

    @DeleteMapping("/classes/delete/{id}")
    public ResponseEntity<?> deleteClass(@PathVariable Long id) {
        try {
            classService.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting class with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("删除班级失败: " + e.getMessage());
        }
    }

    // ==== 班级内学生管理 ====
    @GetMapping("/classes/{classId}/students")
    public String listStudentsInClass(@PathVariable Long classId, Model model) {
        try {
            ClassEntity classEntity = classService.findById(classId)
                    .orElseThrow(() -> new IllegalArgumentException("无效的班级ID: " + classId));
            List<User> students = userService.findStudentsByClazzId(classEntity.getId());
            model.addAttribute("classEntity", classEntity);
            model.addAttribute("students", students);
            if (classEntity.getMajor() != null) {
                 model.addAttribute("majorId", classEntity.getMajor().getId()); 
            }
            model.addAttribute("allUnassignedStudents", userService.findUsersByRoleAndClassEntityIsNull("STUDENT"));
        } catch (Exception e) {
            logger.error("Error listing students for classId: {}", classId, e);
            model.addAttribute("errorMessage", "加载班级内学生列表失败: " + e.getMessage());
            model.addAttribute("classEntity", classService.findById(classId).orElse(new ClassEntity()));
            model.addAttribute("students", List.of());
            model.addAttribute("allUnassignedStudents", List.of()); 
        }
        return "organization/class_students_manage";
    }

    // 显示添加学生到班级的表单
    @GetMapping("/classes/{classId}/students/add")
    public String showAddStudentForm(@PathVariable Long classId, Model model) {
        try {
            ClassEntity clazz = classService.findById(classId)
                    .orElseThrow(() -> new IllegalArgumentException("无效的班级ID: " + classId));
            User student = new User(); // 用于表单绑定
            student.setStudentClass(clazz); // 预设班级
            model.addAttribute("student", student);
            model.addAttribute("classEntity", clazz);
            model.addAttribute("formAction", "/organization/classes/" + classId + "/students/save");
            model.addAttribute("formTitle", "向班级 [" + clazz.getName() + "] 添加新学生");
        } catch (Exception e) {
            logger.error("Error preparing add student form for classId: {}", classId, e);
            model.addAttribute("errorMessage", "准备添加学生表单失败: " + e.getMessage());
            return "redirect:/organization/classes/" + classId + "/students";
        }
        return "organization/student_form"; // 新的或复用的学生表单视图
    }

    // 显示编辑学生信息的表单
    @GetMapping("/classes/{classId}/students/{studentId}/edit")
    public String showEditStudentForm(@PathVariable Long classId, @PathVariable Long studentId, Model model) {
        try {
            ClassEntity clazz = classService.findById(classId)
                    .orElseThrow(() -> new IllegalArgumentException("无效的班级ID: " + classId));
            User student = userService.findById(studentId)
                    .filter(s -> "STUDENT".equals(s.getRole()) && classId.equals(s.getStudentClass().getId()))
                    .orElseThrow(() -> new IllegalArgumentException("无效的学生ID或学生不属于该班级"));
            
            model.addAttribute("student", student);
            model.addAttribute("classEntity", clazz);
            model.addAttribute("formAction", "/organization/classes/" + classId + "/students/save");
            model.addAttribute("formTitle", "修改班级 [" + clazz.getName() + "] 内学生 [" + student.getName() + "] 信息");
        } catch (Exception e) {
            logger.error("Error preparing edit student form for studentId: {} in classId: {}", studentId, classId, e);
            model.addAttribute("errorMessage", "准备编辑学生表单失败: " + e.getMessage());
            return "redirect:/organization/classes/" + classId + "/students";
        }
        return "organization/student_form";
    }

    // 处理添加或修改学生的表单提交
    @PostMapping("/classes/{classId}/students/save")
    public String saveOrUpdateStudentInClass(@PathVariable Long classId, @ModelAttribute("student") User student, Model model) {
        try {
            if (student.getId() == null) { // 新增
                userService.saveStudentInClass(student, classId);
            } else { // 修改
                userService.updateStudentInClass(student, classId);
            }
            return "redirect:/organization/classes/" + classId + "/students";
        } catch (IllegalArgumentException iae) {
            logger.warn("Validation error while saving student in classId {}: {}", classId, iae.getMessage());
            model.addAttribute("errorMessage", "保存学生失败: " + iae.getMessage());
            try {
                 ClassEntity clazz = classService.findById(classId).orElse(null);
                 model.addAttribute("classEntity", clazz);
                 model.addAttribute("formAction", "/organization/classes/" + classId + "/students/save");
                 if (student.getId() == null) {
                    model.addAttribute("formTitle", "向班级 [" + (clazz != null ? clazz.getName() : classId) + "] 添加新学生");
                 } else {
                    model.addAttribute("formTitle", "修改班级 [" + (clazz != null ? clazz.getName() : classId) + "] 内学生 [" + student.getName() + "] 信息");
                 }
            } catch (Exception classLoadEx) {
                 logger.error("Error reloading class info for student form after save failure", classLoadEx);
            }
            model.addAttribute("student", student); // 保留用户输入的数据
            return "organization/student_form";
        } catch (Exception e) {
            logger.error("Error saving student in classId {}: {}", classId, student, e);
            model.addAttribute("errorMessage", "保存学生时发生意外错误: " + e.getMessage());
            model.addAttribute("student", student);
             try {
                 ClassEntity clazz = classService.findById(classId).orElse(null);
                 model.addAttribute("classEntity", clazz);
                 model.addAttribute("formAction", "/organization/classes/" + classId + "/students/save");
                 if (student.getId() == null) {
                    model.addAttribute("formTitle", "向班级 [" + (clazz != null ? clazz.getName() : classId) + "] 添加新学生");
                 } else {
                    model.addAttribute("formTitle", "修改班级 [" + (clazz != null ? clazz.getName() : classId) + "] 内学生 [" + student.getName() + "] 信息");
                 }
            } catch (Exception classLoadEx) {
                 logger.error("Error reloading class info for student form after save failure", classLoadEx);
            }
            return "organization/student_form";
        }
    }

    // 处理将现有未分配学生分配到当前班级
    @PostMapping("/classes/{classId}/students/assign/{studentId}")
    public String assignStudentToClass(@PathVariable Long classId, 
                                     @PathVariable Long studentId, 
                                     RedirectAttributes redirectAttributes) {
        try {
            userService.assignStudentToClass(studentId, classId);
            redirectAttributes.addFlashAttribute("successMessage", "学生成功分配到班级！");
        } catch (IllegalArgumentException e) {
            logger.warn("Assigning student {} to class {} failed: {}", studentId, classId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "分配学生失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error assigning student {} to class {}", studentId, classId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "分配学生时发生意外错误，请查看日志");
        }
        return "redirect:/organization/classes/" + classId + "/students";
    }

    // ==== API Endpoints for dynamic loading ====
    @GetMapping("/api/schools/{schoolId}/colleges")
    @ResponseBody
    public ResponseEntity<?> getCollegesBySchool(@PathVariable Long schoolId) {
        try {
            School school = schoolService.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("无效学校ID: " + schoolId));
            return ResponseEntity.ok(collegeService.findBySchool(school));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching colleges for schoolId: {}", schoolId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("获取学院列表失败");
        }
    }

    // Utility to get majors for a college
    @GetMapping("/api/colleges/{collegeId}/majors")
    @ResponseBody
    public ResponseEntity<?> getMajorsByCollege(@PathVariable Long collegeId) {
        try {
            College college = collegeService.findById(collegeId)
                .orElseThrow(() -> new IllegalArgumentException("无效学院ID: " + collegeId));
            return ResponseEntity.ok(majorService.findByCollege(college));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching majors for collegeId: {}", collegeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("获取专业列表失败");
        }
    }

    // Utility to get classes for a major
    @GetMapping("/api/majors/{majorId}/classes")
    @ResponseBody
    public ResponseEntity<?> getClassesByMajor(@PathVariable Long majorId) {
        try {
            Major major = majorService.findById(majorId)
                .orElseThrow(() -> new IllegalArgumentException("无效专业ID: " + majorId));
            return ResponseEntity.ok(classService.findByMajor(major));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching classes for majorId: {}", majorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("获取班级列表失败");
        }
    }
} 