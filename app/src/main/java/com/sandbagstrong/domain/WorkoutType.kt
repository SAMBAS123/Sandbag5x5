package com.sandbagstrong.domain

enum class WorkoutType {
    A, B;

    fun next(): WorkoutType = if (this == A) B else A
}
