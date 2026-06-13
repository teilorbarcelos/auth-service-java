package com.app.modules.audit;

import com.app.modules.user.UserModel;
import com.app.modules.user.UserRepository;
import com.app.infrastructure.auth.UserSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import io.vertx.core.http.HttpServerRequest;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AuditService {

    private static final Logger LOG = Logger.getLogger(AuditService.class);

    @Inject
    AuditRepository auditRepository;

    @Inject
    ErrorLogRepository errorLogRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    UserSession userSession;

    @Inject
    HttpServerRequest request;

    @Inject
    ObjectMapper objectMapper;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void log(String actionType, String feature, String function, Object params, Object result, String error) {
        try {
            AuditModel audit = new AuditModel();

            String userId = userSession.getUserId();
            if (userId != null) {
                audit.setIdUser(userId);
                UserModel user = userRepository.findById(userId);
                if (user != null) {
                    audit.setUserName(user.getName());
                } else {
                    LOG.debugv("User not found in DB for ID: {0}", userId);
                }
            } else {
                LOG.debug("UserSession is empty or null");
            }

            audit.setActionType(actionType);
            audit.setTableName(feature);
            audit.setClassName(feature);
            audit.setFunctionName(function);

            audit.setIp(getClientIp());
            audit.setMethod(request.method().name());
            audit.setOriginalUrl(request.uri());

            if (params != null)
                audit.setParams(objectMapper.writeValueAsString(params));
            if (result != null)
                audit.setDiffValue(objectMapper.writeValueAsString(result));
            if (error != null)
                audit.setError(error);

            LOG.debugv("Persisting audit log for: {0} - {1}", feature, actionType);
            auditRepository.persist(audit);
            LOG.debug("Audit log persisted successfully");
        } catch (Exception e) {
            LOG.errorv("Failed to save audit log: {0}", e.getMessage());
        }
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void logError(Throwable exception, String source) {
        try {
            String userId = userSession.getUserId();
            if (userId == null) {
                return;
            }

            ErrorLog log = new ErrorLog();
            log.setIdUser(userId);

            log.setSource(source != null ? source : request.uri());
            log.setErrorMessage(exception.getMessage());

            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            exception.printStackTrace(pw);
            log.setErrorData(sw.toString());

            errorLogRepository.persist(log);
        } catch (Exception e) {
            LOG.errorv("Failed to save error log: {0}", e.getMessage());
        }
    }

    private String getClientIp() {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty())
            return xf.split(",")[0];
        return request.remoteAddress().host();
    }
}
