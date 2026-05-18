package com.gp.q733.domain.model

import com.gp.q733.domain.model.Label

data class LabelTemplate(
    val id: String,
    val name: String,
    val description: String,
    val label: Label
)
