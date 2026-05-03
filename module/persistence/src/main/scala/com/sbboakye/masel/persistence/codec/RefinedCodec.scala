package com.sbboakye.masel.persistence.codec

import io.github.iltotore.iron.*
import skunk.*

type Base[T] = T match
  case a :| c => a

type ConstraintOf[T] = T match
  case a :| c => c

def refined[T](base: Codec[Base[T]])(
    using rc: RuntimeConstraint[Base[T], ConstraintOf[T]],
): Codec[T] =
  base.eimap[T](_.refineEither[ConstraintOf[T]].map(_.asInstanceOf[T]))(_.asInstanceOf[Base[T]])
