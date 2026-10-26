package com.dndsaas.controller

import com.dndsaas.config.AuthProperties
import com.dndsaas.dto.ChangePasswordRequest
import com.dndsaas.dto.ForgotPasswordRequest
import com.dndsaas.dto.ResetPasswordRequest
import com.dndsaas.dto.TokenRequest
import com.dndsaas.dto.LoginRequest
import com.dndsaas.dto.UpdateProfileRequest
import com.dndsaas.dto.UserRegistrationRequest
import com.dndsaas.dto.UserResponse
import com.dndsaas.service.AuthService
import com.dndsaas.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

/**
 * Creating an account, signing in and out. Signing in sets an HttpOnly session
 * cookie that the browser sends with every later request.
 */
@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Sign-in", description = "Accounts, signing in and out")
class AuthController(
    private val authService: AuthService,
    private val userService: UserService,
    private val properties: AuthProperties,
) {

    @PostMapping("/register")
    @Operation(summary = "Create an account and sign in", description = "Free accounts start with 50 tokens.")
    fun register(@RequestBody request: UserRegistrationRequest): ResponseEntity<UserResponse> {
        val signedIn = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .header(HttpHeaders.SET_COOKIE, cookie(signedIn.token, signedIn.maxAge))
            .body(signedIn.user)
    }

    @PostMapping("/login")
    @Operation(summary = "Sign in with email and password")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<UserResponse> {
        val signedIn = authService.login(request.email, request.password)
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie(signedIn.token, signedIn.maxAge))
            .body(signedIn.user)
    }

    @PostMapping("/logout")
    @Operation(summary = "Sign out of this browser")
    fun logout(@CookieValue(name = "\${app.auth.cookie-name}", required = false) token: String?): ResponseEntity<Void> {
        authService.logout(token)
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO)).build()
    }

    @GetMapping("/me")
    @Operation(summary = "The signed-in user (401 when signed out)")
    fun me(@CurrentUser userId: Long): UserResponse = userService.get(userId)

    @PutMapping("/me")
    @Operation(summary = "Change the display name")
    fun updateProfile(@CurrentUser userId: Long, @RequestBody request: UpdateProfileRequest): UserResponse =
        userService.updateDisplayName(userId, request.displayName)

    @PutMapping("/password")
    @Operation(summary = "Change the password; signs out every other browser")
    fun changePassword(
        @CurrentUser userId: Long,
        @CookieValue(name = "\${app.auth.cookie-name}", required = false) token: String?,
        @RequestBody request: ChangePasswordRequest,
    ): ResponseEntity<Void> {
        authService.changePassword(userId, token, request.currentPassword, request.newPassword)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Confirm the email address with the token from the verification email")
    fun verifyEmail(@RequestBody request: TokenRequest): UserResponse = authService.verifyEmail(request.token)

    @PostMapping("/resend-verification")
    @Operation(summary = "Send the verification email again (at most once a minute)")
    fun resendVerification(@CurrentUser userId: Long): ResponseEntity<Void> {
        authService.resendVerification(userId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/forgot-password")
    @Operation(
        summary = "Email a password reset link",
        description = "Always answers 204, whether or not the address has an account.",
    )
    fun forgotPassword(@RequestBody request: ForgotPasswordRequest): ResponseEntity<Void> {
        authService.forgotPassword(request.email)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Choose a new password with the token from the reset email; signs out everywhere")
    fun resetPassword(@RequestBody request: ResetPasswordRequest): ResponseEntity<Void> {
        authService.resetPassword(request.token, request.newPassword)
        return ResponseEntity.noContent().build()
    }

    private fun cookie(value: String, maxAge: Duration): String =
        ResponseCookie.from(properties.cookieName, value)
            .httpOnly(true)
            .secure(properties.secureCookie)
            .sameSite("Lax")
            .path("/")
            .maxAge(maxAge)
            .build()
            .toString()
}
