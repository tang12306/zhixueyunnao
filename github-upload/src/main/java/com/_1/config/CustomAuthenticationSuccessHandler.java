package com._1.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationSuccessHandler.class);
    private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                        HttpServletResponse response, 
                                        Authentication authentication) throws IOException, ServletException {

        String targetUrl = determineTargetUrl(authentication, request);

        if (response.isCommitted()) {
            logger.debug("Response has already been committed. Unable to redirect to {}", targetUrl);
            return;
        }

        redirectStrategy.sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(Authentication authentication, HttpServletRequest request) {
        // 获取登录时表单提交的 userType 参数
        String userTypeParam = request.getParameter("userType"); 
        logger.debug("Login form userType param: {}", userTypeParam);

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        
        boolean isStudent = authorities.stream()
                                 .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_STUDENT"));
        boolean isTeacher = authorities.stream()
                                 .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_TEACHER"));

        logger.debug("User authorities: {}, isStudent: {}, isTeacher: {}", authorities, isStudent, isTeacher);

        if (isStudent && "student".equalsIgnoreCase(userTypeParam)) {
            logger.info("Redirecting student to /student/exams");
            return "/student/exams";
        } else if (isTeacher && "teacher".equalsIgnoreCase(userTypeParam)) {
            logger.info("Redirecting teacher to /");
            return "/"; 
        } else {
            // 如果角色与选择不符，或没有明确匹配，可以重定向到登录页并带错误提示，或者一个默认页
            // 为简单起见，如果角色和选择的类型不匹配，我们先让他到首页，首页Controller会根据实际角色处理
            // 或者，更严格的话，可以重定向回登录页带特定错误参数
            logger.warn("Role/userType mismatch or unhandled role. Param: {}, IsStudent: {}, IsTeacher: {}. Redirecting to /login?error=role_mismatch", 
                        userTypeParam, isStudent, isTeacher);
            // return "/login?error=role_mismatch"; // 更严格的错误处理
            // 或者，如果教师也可能不选类型，默认教师到首页，学生必须选对类型
            if(isTeacher) return "/";
            if(isStudent && userTypeParam == null) return "/login?error=select_usertype"; // 提示学生选择类型
            return "/login?error=true"; // 通用错误
        }
    }
} 