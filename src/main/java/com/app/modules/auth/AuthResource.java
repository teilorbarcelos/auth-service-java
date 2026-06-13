package com.app.modules.auth;

import com.app.infrastructure.auth.Authenticated;
import com.app.infrastructure.auth.UserSession;
import com.app.modules.auth.dto.AuthResponseDTO;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import java.util.Map;

@Path("/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Auth")
public class AuthResource {
    private static final String EMAIL_KEY = "email";


    @Inject
    AuthService authService;

    @Inject
    UserSession userSession;

    @POST
    @Path("/login")
    @Operation(summary = "Authenticate user", description = "Returns a JWT token and user info if credentials are valid.")
    @APIResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = AuthSchemas.AuthResponse.class)))
    @APIResponse(responseCode = "401", description = "Invalid credentials")
    public Response login(@RequestBody(content = @Content(schema = @Schema(implementation = AuthSchemas.LoginRequest.class))) Map<String, String> body) {
        String email = body.getOrDefault(EMAIL_KEY, "");
        String password = body.getOrDefault("password", "");
        AuthResponseDTO result = authService.login(email, password);
        return Response.ok(result).build();
    }

    @GET
    @Path("/me")
    @Authenticated
    @Operation(summary = "Get current user info", description = "Returns the profile of the currently authenticated user.")
    @APIResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = AuthSchemas.UserInfo.class)))
    public Response me() {
        String userId = userSession.getUserId();
        AuthResponseDTO result = authService.getMe(userId);
        return Response.ok(result).build();
    }

    @POST
    @Path("/refresh")
    @Operation(summary = "Refresh session", description = "Generates a new JWT token using a valid refresh token.")
    @APIResponse(responseCode = "200", description = "Token refreshed", content = @Content(schema = @Schema(implementation = AuthSchemas.AuthResponse.class)))
    public Response refresh(@RequestBody(content = @Content(schema = @Schema(implementation = AuthSchemas.RefreshTokenRequest.class))) Map<String, String> body) {
        String refreshToken = body.getOrDefault("refreshToken", "");
        AuthResponseDTO result = authService.refreshToken(refreshToken);
        return Response.ok(result).build();
    }

    @POST
    @Path("/password/request")
    @Operation(summary = "Request password reset", description = "Sends an email with a reset token.")
    @APIResponse(responseCode = "200", description = "Email sent")
    public Response requestPasswordReset(@RequestBody(content = @Content(schema = @Schema(implementation = AuthSchemas.PasswordRequest.class))) Map<String, String> body) {
        String email = body.getOrDefault(EMAIL_KEY, "");
        authService.requestPasswordReset(email);
        return Response.ok(Map.of("message", "E-mail de recuperação enviado com sucesso!")).build();
    }

    @POST
    @Path("/password/validate")
    @Operation(summary = "Validate reset token")
    public Response validateResetToken(Map<String, String> body) {
        String email = body.getOrDefault(EMAIL_KEY, "");
        String token = body.getOrDefault("token", "");
        authService.validateResetToken(email, token);
        return Response.ok(Map.of("valid", true)).build();
    }

    @POST
    @Path("/password/reset")
    @Operation(summary = "Reset password")
    public Response resetPassword(Map<String, String> body) {
        String email = body.getOrDefault(EMAIL_KEY, "");
        String token = body.getOrDefault("token", "");
        String newPassword = body.getOrDefault("password", "");
        authService.resetPassword(email, token, newPassword);
        return Response.ok(Map.of("message", "Senha alterada com sucesso!")).build();
    }
}
