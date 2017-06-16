package com.meetup

import io.swagger.models.properties.AbstractProperty

package object codegen {
  def mkProp[P <: AbstractProperty](f: => P): String => P =
    v => {
      val prop = f
      prop.setFormat(v)
      prop
    }

}
