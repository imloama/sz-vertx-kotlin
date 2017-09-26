package sz.api.doc

import sz.scaffold.Application
import sz.scaffold.annotations.PostForm
import sz.scaffold.annotations.PostJson
import sz.scaffold.controller.ApiRoute
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.jvmErasure

//
// Created by kk on 17/8/24.
//
class DefinedApis(private val host: String = "localhost:9000") {

    var groups: MutableList<ApiGroup> = mutableListOf()

    init {
        val routeFile = Application.getFile("conf/route")
        ApiRoute.parseFromFile(routeFile).filter {
            it.isJsonApi()
        }.forEach {
            addApi(it)
        }
    }

    private fun apiGrouByName(name: String): ApiGroup {
        val group = groups.find { it.groupName == name }
        return if (group != null) {
            group
        } else {
            val newGroup = ApiGroup(name)
            groups.add(newGroup)
            newGroup
        }
    }

    private fun addApi(apiRoute: ApiRoute) {
        val apiInfo = apiRoute.buildApiInfo(host)
        val group = apiGrouByName(apiInfo.groupName())
        group.apiInfoList.add(apiInfo)
    }

}

fun ApiRoute.buildApiInfo(host: String): ApiInfo {
    var httpMethod = this.method.name
    if (this.controllerFun.findAnnotation<PostForm>() != null) {
        httpMethod = ApiInfo.PostForm
    }
    if (this.controllerFun.findAnnotation<PostJson>() != null) {
        httpMethod = ApiInfo.PostJson
    }
    return ApiInfo(host = host,
            url = this.path,
            httpMethod = httpMethod,
            controllerClass = this.controllerKClass.java.name,
            methodName = this.controllerFun.name,
            replyKClass = this.returnType().jvmErasure,
            postDataKClass = this.postBodyClass()
    )
}