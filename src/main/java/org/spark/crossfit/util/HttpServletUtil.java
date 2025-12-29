package org.spark.crossfit.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;


@Slf4j
public class HttpServletUtil {
    public static String getCookieValue(HttpServletRequest request, String cookieName) {



        if (request.getCookies() == null) {
            log.info("No cookies found in the request.");
            return null;
        }


        log.info("COOKIES: {}", Arrays.toString(request.getCookies()));

        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
