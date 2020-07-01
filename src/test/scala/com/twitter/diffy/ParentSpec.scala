package com.twitter.diffy

import org.mockito.Matchers.{eq => _eq}
import org.mockito.Mockito.RETURNS_SMART_NULLS
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, MustMatchers, OneInstancePerTest}

/**
 * Base trait for ScalaTest implementations that includes commonly used
 * mixins
 */
trait ParentSpec
  extends FunSpec
  with MustMatchers
  with MockitoSugar
  with OneInstancePerTest
{
  def smartMock[T <: AnyRef](implicit m: Manifest[T]): T = mock[T](RETURNS_SMART_NULLS)
  def argEq[T](value: T) = _eq(value)
}