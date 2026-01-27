package com.example.internshipproject.data.model

data class Company(
    val companyEmail: String,
    val contactNumber: String,
    val password: String,
    val companyName: String,
    val contactPerson: String,
    val industryType: String,
    val companyAddress: String,
    val companyDescription: String,
    val logoUri: String?
)
