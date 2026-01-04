package com.timesheet.timesheet.util;

import java.util.regex.Pattern;

public class ValidationUtils {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    private static final Pattern SUBDOMAIN_PATTERN =
            Pattern.compile("^[a-z0-9]([a-z0-9-]{1,61}[a-z0-9])?$");

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidSubdomain(String subdomain) {
        return subdomain != null && SUBDOMAIN_PATTERN.matcher(subdomain).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }
}

