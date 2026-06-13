package com.app.modules.audit;

import com.app.infrastructure.auth.ResourceFeature;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.jboss.logging.Logger;

@Audited
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class AuditInterceptor {

    private static final Logger LOG = Logger.getLogger(AuditInterceptor.class);

    @Inject
    AuditService auditService;

    @AroundInvoke
    public Object audit(InvocationContext context) throws Exception {
        String methodName = context.getMethod().getName();

        boolean shouldAudit = methodName.startsWith("create") ||
                methodName.startsWith("update") ||
                methodName.startsWith("delete") ||
                methodName.startsWith("toggle") ||
                methodName.startsWith("setStatus");

        if (!shouldAudit) {
            return context.proceed();
        }

        Object result = null;
        String error = null;

        try {
            result = context.proceed();
            return result;
        } catch (Exception e) {
            error = e.getMessage();
            throw e;
        } finally {
            String feature = "unknown";
            Class<?> targetClass = context.getTarget().getClass();
            LOG.debugv("Audit target class: {0}", targetClass.getName());

            ResourceFeature resourceFeature = null;
            while (targetClass != null) {
                resourceFeature = targetClass.getAnnotation(ResourceFeature.class);
                if (resourceFeature != null) {
                    LOG.debugv("Found @ResourceFeature on: {0}", targetClass.getName());
                    break;
                }
                targetClass = targetClass.getSuperclass();
            }

            if (resourceFeature != null) {
                feature = resourceFeature.value();
            } else {
                LOG.warnv("@ResourceFeature not found in hierarchy of {0}",
                        context.getTarget().getClass().getName());
            }

            auditService.log(
                    methodName.toUpperCase(),
                    feature,
                    methodName,
                    context.getParameters(),
                    result,
                    error);
        }
    }
}
