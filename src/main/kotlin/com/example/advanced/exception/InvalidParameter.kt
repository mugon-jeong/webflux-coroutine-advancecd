package com.example.advanced.exception

import org.springframework.http.HttpStatus
import org.springframework.validation.BindException
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.ResponseStatus
import kotlin.reflect.KProperty

// webflux에서는 BindingResult를 컨트롤러에서 지원하지 않음
// 직접 만들어줘야함
@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidParameter(request: Any, field: KProperty<*>, code: String = "", message: String = "") : BindException(
    WebDataBinder(request, request::class.simpleName!!).bindingResult.apply {
        rejectValue(field.name, code, message)
    }
) {
}