package com.sbboakye.masel.core.domain

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*

type NonEmptyString = String :| Not[Empty]

type PositiveInt = Int :| Positive

type Score = Int :| (GreaterEqual[0] & LessEqual[100])

type DatabasePassword = String :| MinLength[8]
