package app.simplecloud.droplet.serverhost.shared.template

import app.simplecloud.droplet.serverhost.shared.actions.YamlActionData
import app.simplecloud.droplet.serverhost.shared.actions.YamlActionTriggerTypes

interface YamlTemplateActionMap : Map<YamlActionTriggerTypes, List<YamlActionData>>