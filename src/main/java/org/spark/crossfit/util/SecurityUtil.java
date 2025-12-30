package org.spark.crossfit.util;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    public static String getCurrentUserId() {
        SecurityContext context = SecurityContextHolder.getContext();
        return context.getAuthentication().getPrincipal().toString();
    }
}
