package com.meetup

import io.swagger.v3.oas.models.media.Schema

package object codegen {
  def mkSchema[P <: Schema[_]](f: => P): String => P =
    v => {
      val prop = f
      prop.setFormat(v)
      prop
    }

}
