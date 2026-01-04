package com.timesheet.timesheet.config;

import com.timesheet.timesheet.exception.TenantNotFoundException;
import com.timesheet.timesheet.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private final TenantRepository tenantRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String tenantId = request.getHeader("X-Tenant-ID");

            if (tenantId != null) {
                Long tid = Long.parseLong(tenantId);
                if (tenantRepository.existsById(tid)) {
                    TenantContext.setTenantId(tid);
                } else {
                    throw new TenantNotFoundException("Tenant not found");
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
