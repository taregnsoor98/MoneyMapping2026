package com.moneymapping.backend

import org.springframework.mail.javamail.JavaMailSender // the Spring mail sender
import org.springframework.mail.javamail.MimeMessageHelper // helps build the email content
import org.springframework.scheduling.annotation.Async // runs this method in a background thread so server responds instantly
import org.springframework.stereotype.Service // marks this as a Spring service

@Service // tells Spring to create and manage this class automatically
class EmailService(
    private val mailSender: JavaMailSender // injected automatically by Spring
) {

    @Async // sends email in background — server won't wait for this to finish before responding
    fun sendConfirmationEmail(to: String, token: String) { // sends a confirmation email to the user
        val message = mailSender.createMimeMessage() // create a new email message
        val helper = MimeMessageHelper(message, true) // helper to set email fields

        helper.setFrom("tareg123456789tareg@gmail.com") // who the email is from
        helper.setTo(to) // who the email is going to
        helper.setSubject("Confirm your MoneyMapping account") // email subject line

        val confirmUrl = "http://192.168.31.216:8080/account/confirm?token=$token" // the link the user clicks to confirm

        helper.setText(
            """
            <h2>Welcome to MoneyMapping!</h2>
            <p>Click the link below to confirm your email address:</p>
            <a href="$confirmUrl">Confirm my account</a>
            <p>This link expires in 24 hours.</p>
            """.trimIndent(),
            true // true means the body is HTML
        )

        mailSender.send(message) // send the email
    }
}