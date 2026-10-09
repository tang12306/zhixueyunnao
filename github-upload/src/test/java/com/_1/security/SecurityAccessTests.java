package com._1.security;

import com._1.core.common.Roles;
import com._1.entity.Setting;
import com._1.repository.SettingRepository;
import com._1.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 访问控制回归测试：匿名一律 401、学生访问管理接口 403、设置修改只给管理员、
 * 登录接口的成功/失败/锁定，以及敏感字段不出现在响应里。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAccessTests {

    private static final String TEACHER = "test.teacher";
    private static final String TEACHER_PASSWORD = "TeacherPass123!";
    private static final String STUDENT = "test.student";
    private static final String STUDENT_PASSWORD = "StudentPass123!";
    private static final String LOCKED = "test.locked";
    private static final String LOCKED_PASSWORD = "LockedPass123!";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserService userService;

    @Autowired
    private SettingRepository settingRepository;

    @Autowired
    private Environment environment;

    @BeforeEach
    void createUsers() {
        // saveUser 会跳过已存在的用户，可重复调用
        userService.saveUser(TEACHER, "测试教师", TEACHER_PASSWORD, Roles.TEACHER);
        userService.saveUser(STUDENT, "测试学生", STUDENT_PASSWORD, Roles.STUDENT);
        userService.saveUser(LOCKED, "锁定测试", LOCKED_PASSWORD, Roles.TEACHER);
    }

    // ---- 匿名访问 ----

    @Test
    void anonymousApiRequestsGet401Json() throws Exception {
        for (String path : new String[] {"/api/students", "/api/settings", "/api/subjects", "/api/colleges",
                "/api/ai/health-check", "/api/user/current", "/api/auth/me", "/questions/api/query"}) {
            mvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Test
    void anonymousAiGenerationIsRejected() throws Exception {
        mvc.perform(post("/api/ai/generate-exam").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/generate-question").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void removedTestEndpointIsGone() throws Exception {
        mvc.perform(get("/api/test/hello")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void removedTestEndpointReturns404ForTeacher() throws Exception {
        mvc.perform(get("/api/test/hello")).andExpect(status().isNotFound());
    }

    // ---- 角色 ----

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotUseManagementApis() throws Exception {
        mvc.perform(get("/api/students")).andExpect(status().isForbidden());
        mvc.perform(get("/api/settings")).andExpect(status().isForbidden());
        mvc.perform(get("/questions/api/query")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCanReadButNotChangeSettings() throws Exception {
        mvc.perform(get("/api/settings")).andExpect(status().isOk());
        mvc.perform(put("/api/settings/SITE_NAME").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanChangeSettings() throws Exception {
        mvc.perform(put("/api/settings/SITE_NAME").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"教务题库管理系统\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void retiredApiKeySettingIsHidden() throws Exception {
        settingRepository.save(new Setting("DEEPSEEK_API_KEY", "should-not-leak", "DeepSeek API密钥", "", "AI服务", true));

        mvc.perform(get("/api/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("DEEPSEEK_API_KEY"))));
        mvc.perform(get("/api/settings/DEEPSEEK_API_KEY")).andExpect(status().isNotFound());
        mvc.perform(put("/api/settings/DEEPSEEK_API_KEY").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"x\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void writeRequestsWithoutCsrfTokenAreRejected() throws Exception {
        mvc.perform(post("/api/colleges").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    // ---- 敏感信息 ----

    @Test
    @WithMockUser(roles = "TEACHER")
    void studentListDoesNotExposePasswords() throws Exception {
        mvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").exists())
                .andExpect(content().string(not(containsString("password"))));
    }

    @Test
    void errorResponsesDoNotIncludeDetails() {
        assertThat(environment.getProperty("server.error.include-stacktrace")).isEqualTo("never");
        assertThat(environment.getProperty("server.error.include-message")).isEqualTo("never");
        assertThat(environment.getProperty("server.error.include-exception")).isEqualTo("false");
    }

    // ---- 登录接口 ----

    @Test
    void teacherCanLoginAndSessionIsKept() throws Exception {
        MvcResult result = mvc.perform(login(TEACHER, TEACHER_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.username").value(TEACHER))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value(Roles.TEACHER));
        mvc.perform(get("/api/students").session(session)).andExpect(status().isOk());

        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordGets401() throws Exception {
        mvc.perform(login(TEACHER, "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
        mvc.perform(login("no.such.user", "whatever"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void studentLoginIsRejectedWhileStudentPortalIsClosed() throws Exception {
        MvcResult result = mvc.perform(login(STUDENT, STUDENT_PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("学生端暂未开放")))
                .andReturn();

        // 被拒绝后不能留下已登录的会话
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        if (session != null) {
            mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void repeatedFailuresLockTheAccount() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(login(LOCKED, "wrong-password")).andExpect(status().isUnauthorized());
        }
        // 锁定期间即使密码正确也拒绝
        mvc.perform(login(LOCKED, LOCKED_PASSWORD)).andExpect(status().isTooManyRequests());
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder login(
            String username, String password) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
    }
}
