package app.simplecloud.droplet.serverhost.shared.actions

interface YamlAction<TData> {
    fun exec(ctx: YamlActionContext, data: TData)
    fun getDataType(): Class<TData>
}