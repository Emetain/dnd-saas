package com.dndsaas.service

import com.dndsaas.config.AuthProperties
import com.dndsaas.domain.User
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.springframework.web.util.HtmlUtils

/**
 * The emails Narrion sends. A failed send is logged, never thrown: a mail
 * server hiccup must not break registration — the user can ask for the email again.
 */
@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val properties: AuthProperties,
) {
    private val log = LoggerFactory.getLogger(EmailService::class.java)

    fun sendVerification(user: User, token: String) = send(
        to = user.email,
        subject = "Confirm your email for Narrion",
        greeting = "Hi ${user.displayName},",
        text = "Welcome to Narrion! Confirm your email address to start generating — your 50 free tokens are waiting.",
        buttonLabel = "Confirm my email",
        link = "${properties.baseUrl}/verify-email?token=$token",
        footer = "The link works for 48 hours. Didn't create an account? Then you can ignore this email.",
    )

    fun sendPasswordReset(user: User, token: String) = send(
        to = user.email,
        subject = "Reset your Narrion password",
        greeting = "Hi ${user.displayName},",
        text = "Someone (hopefully you) asked to reset the password of your Narrion account.",
        buttonLabel = "Choose a new password",
        link = "${properties.baseUrl}/reset-password?token=$token",
        footer = "The link works for 1 hour. Didn't ask for this? Then ignore this email — your password stays the same.",
    )

    private fun send(to: String, subject: String, greeting: String, text: String, buttonLabel: String, link: String, footer: String) {
        try {
            val message = mailSender.createMimeMessage()
            MimeMessageHelper(message, true, "UTF-8").apply {
                setFrom(properties.mailFrom)
                setTo(to)
                setSubject(subject)
                setText(
                    "$greeting\n\n$text\n\n$buttonLabel: $link\n\n$footer\n\n— Narrion",
                    html(greeting, text, buttonLabel, link, footer),
                )
            }
            mailSender.send(message)
            log.info("Sent \"{}\" to {}", subject, to)
        } catch (e: Exception) {
            log.error("Could not send \"{}\" to {}: {}", subject, to, e.message)
        }
    }

    private fun html(greeting: String, text: String, buttonLabel: String, link: String, footer: String): String {
        fun esc(value: String) = HtmlUtils.htmlEscape(value)
        return """
            <div style="font-family:Segoe UI,Helvetica,Arial,sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#1b2540">
              <h2 style="margin:0 0 16px;color:#0d1a3d">Narrion</h2>
              <p>${esc(greeting)}</p>
              <p>${esc(text)}</p>
              <p style="margin:28px 0">
                <a href="${esc(link)}" style="background:#2549a8;color:#fff;padding:12px 20px;border-radius:8px;text-decoration:none;font-weight:600">${esc(buttonLabel)}</a>
              </p>
              <p style="font-size:13px;color:#5b6583">Or paste this link into your browser:<br><span style="word-break:break-all">${esc(link)}</span></p>
              <p style="font-size:13px;color:#5b6583">${esc(footer)}</p>
            </div>
        """.trimIndent()
    }
}
