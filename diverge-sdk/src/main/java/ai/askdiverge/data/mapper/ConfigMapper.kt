package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.ConfigRemote
import ai.askdiverge.data.model.config.display.DisplayRemote
import ai.askdiverge.data.model.config.display.subtitle.SubtitleLinkValueRemote
import ai.askdiverge.data.model.config.display.subtitle.SubtitleRemote
import ai.askdiverge.domain.model.Config
import ai.askdiverge.domain.model.config.display.Display
import ai.askdiverge.domain.model.config.display.subtitle.Subtitle
import ai.askdiverge.domain.model.config.display.subtitle.SubtitleLink

internal fun ConfigRemote.mapToDomain() = Config(
    display = display.mapToDomain(),
    theme = theme.mapToDomain()
)

private fun DisplayRemote.mapToDomain() = Display(
    name = name,
    avatarUrl = avatar.url,
    welcomeMessage = welcome_message,
    subtitle = subtitle?.mapToDomain(),
    privacyPolicyUrl = privacy_policy_url
)

private fun SubtitleRemote.mapToDomain() = Subtitle(
    text = text,
    link = link?.mapToDomain()
)

private fun SubtitleLinkValueRemote.mapToDomain() = SubtitleLink(
    text = text,
    url = url
)
