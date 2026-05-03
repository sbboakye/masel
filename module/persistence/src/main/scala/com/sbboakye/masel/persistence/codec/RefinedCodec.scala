package com.sbboakye.masel.persistence.codec

import io.github.iltotore.iron.*
import skunk.*

type Base[T] = T match
  case a :| c => a

type ConstraintOf[T] = T match
  case a :| c => c

/*
encode is a no-op because T reduces to Base[T] :| ConstraintOf[T];
we can't express the bound without recursion limits.
 */
def refined[T](base: Codec[Base[T]])(
    using rc: RuntimeConstraint[Base[T], ConstraintOf[T]],
): Codec[T] =
  base.eimap[T](_.refineEither[ConstraintOf[T]].map(_.asInstanceOf[T]))(_.asInstanceOf[Base[T]])
