package com.ainotebuddy.app.domain.template

data class Template(
    val id: Long,
    val name: String,
    val description: String?,
    val icon: String,
    val category: String
)