package com.dndsaas.controller

import com.dndsaas.config.AuthInterceptor
import com.dndsaas.service.NotSignedInException
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * The id of the signed-in user, taken from the session cookie:
 *
 *     fun list(@CurrentUser userId: Long): List<CampaignResponse>
 *
 * Only available on endpoints that require sign-in (see AuthConfig).
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class CurrentUser

class CurrentUserArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(CurrentUser::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Long =
        webRequest.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST) as? Long
            ?: throw NotSignedInException()
}
