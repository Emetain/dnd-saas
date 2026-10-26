package com.dndsaas.config

import com.dndsaas.controller.CurrentUser
import com.dndsaas.controller.CurrentUserArgumentResolver
import com.dndsaas.service.AuthService
import com.dndsaas.service.NotSignedInException
import com.dndsaas.service.OwnershipService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springdoc.core.utils.SpringDocUtils
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Sign-in for the whole API: every `/v1` endpoint needs a signed-in user,
 * except the handful that must work without one (signing in, the Stripe
 * webhook, public status). Controllers receive the user with [CurrentUser].
 */
@Configuration
class AuthConfig(
    private val authInterceptor: AuthInterceptor,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/v1/**")
            .excludePathPatterns(PUBLIC_PATHS)
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(CurrentUserArgumentResolver())
    }

    companion object {
        /** Reachable without signing in. */
        val PUBLIC_PATHS = listOf(
            "/v1/auth/login",
            "/v1/auth/register",
            "/v1/auth/logout",
            "/v1/auth/verify-email",
            "/v1/auth/forgot-password",
            "/v1/auth/reset-password",
            "/v1/billing/config",
            "/v1/stripe/webhook",
            "/v1/ai/status",
        )

        init {
            // The signed-in user is not a request parameter; keep it out of Swagger.
            SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUser::class.java)
        }
    }
}

/**
 * Resolves the session cookie to a user, then checks that every id in the URL
 * (a campaign, an NPC, a session, a user…) belongs to that user.
 */
@Component
class AuthInterceptor(
    private val authService: AuthService,
    private val ownershipService: OwnershipService,
    private val properties: AuthProperties,
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (handler !is HandlerMethod || request.method == "OPTIONS") return true

        val token = request.cookies?.firstOrNull { it.name == properties.cookieName }?.value
        val userId = token?.let(authService::userIdFor) ?: throw NotSignedInException()
        request.setAttribute(CURRENT_USER_ATTRIBUTE, userId)

        @Suppress("UNCHECKED_CAST")
        val variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as? Map<String, String> ?: emptyMap()
        val pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as? String ?: ""
        ownershipService.check(userId, pattern, variables)
        return true
    }

    companion object {
        const val CURRENT_USER_ATTRIBUTE = "narrion.currentUserId"
    }
}
