package com.moneymapping.backend

import com.sirolf2009.modulith.account.JWTUser // the interface we must implement
import jakarta.persistence.Column // for column constraints
import jakarta.persistence.Entity // marks this as a database entity
import jakarta.persistence.Id // marks the primary key
import jakarta.persistence.Table // specifies the table name

@Entity // tells Hibernate to create a table for this class
@Table(name = "users") // the table will be called "users"
data class User(
    @Id // this is the primary key
    override val id: String, // unique user ID
    override val username: String, // the username used to login
    override val password: String, // the hashed password
    override val salt: String, // the salt used for hashing
    @Column(unique = true, nullable = false) // email must be unique and not empty
    val email: String, // the email address used to register
    val confirmationToken: String? = null, // the token sent to the user's email to confirm their account
    val confirmed: Boolean = false // whether the user has confirmed their email
) : JWTUser // implements JWTUser so the library can use it