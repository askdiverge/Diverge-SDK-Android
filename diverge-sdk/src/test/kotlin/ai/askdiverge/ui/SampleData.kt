package ai.askdiverge.ui

import ai.askdiverge.domain.model.Config
import ai.askdiverge.domain.model.config.display.Display
import ai.askdiverge.domain.model.config.display.subtitle.Subtitle
import ai.askdiverge.domain.model.config.theme.Brand
import ai.askdiverge.domain.model.config.theme.Theme
import ai.askdiverge.domain.model.config.theme.ThemeProductCard
import ai.askdiverge.domain.model.config.theme.ThemeSurface
import ai.askdiverge.domain.model.config.theme.font.ThemeAndroidFont
import ai.askdiverge.domain.model.config.theme.font.ThemeFont
import ai.askdiverge.domain.model.config.theme.header.HeaderAlignment
import ai.askdiverge.domain.model.config.theme.header.ThemeHeader
import ai.askdiverge.domain.model.config.theme.header.ThemeHeaderButton
import ai.askdiverge.domain.model.config.theme.input.ThemeInput
import ai.askdiverge.domain.model.config.theme.messages.AssistantMessageStyle
import ai.askdiverge.domain.model.config.theme.messages.ThemeMessages
import ai.askdiverge.domain.model.config.theme.messages.UserMessageStyle
import ai.askdiverge.domain.model.message.incoming.Message
import ai.askdiverge.domain.model.message.incoming.MessageRole
import ai.askdiverge.domain.model.message.incoming.part.MessagePart
import ai.askdiverge.domain.model.message.incoming.part.ProductPart
import ai.askdiverge.domain.model.message.incoming.part.RichTextPart
import ai.askdiverge.domain.model.message.incoming.part.TablePart
import ai.askdiverge.domain.model.message.incoming.part.product.ProductCard
import ai.askdiverge.domain.model.message.incoming.part.product.ProductPrice
import ai.askdiverge.domain.model.message.incoming.part.table.TableCell
import ai.askdiverge.domain.model.message.incoming.part.table.TableColumnAlignment
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextBlock
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextBulletListBlock
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextBulletListItem
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextParagraphBlock
import ai.askdiverge.domain.model.message.incoming.part.text.TableCellImageBlock
import ai.askdiverge.domain.model.message.incoming.part.text.span.RichTextSpan
import ai.askdiverge.domain.model.message.incoming.part.text.span.RichTextSpanType
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextBlockUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanTypeUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanUiModel

private const val SAMPLE_ASSISTANT_NAME = "Bot"
internal const val SAMPLE_PRIVACY_POLICY_URL = "https://example.com/privacy-policy"

internal fun sampleConfig(
    display: Display = sampleDisplay(),
    theme: Theme = sampleTheme()
): Config = Config(
    display = display,
    theme = theme
)

internal fun sampleDisplay(
    name: String = SAMPLE_ASSISTANT_NAME,
    avatarUrl: String? = null,
    welcomeMessage: String? = null,
    subtitle: Subtitle? = null,
    privacyPolicyUrl: String = SAMPLE_PRIVACY_POLICY_URL
): Display = Display(
    name = name,
    avatarUrl = avatarUrl,
    welcomeMessage = welcomeMessage,
    subtitle = subtitle,
    privacyPolicyUrl = privacyPolicyUrl
)

internal fun sampleTheme(): Theme = Theme(
    brand = Brand(primaryColor = "#000000"),
    surface = ThemeSurface(
        backgroundGradientColor = null,
        backgroundColor = null,
        mutedTextColor = null
    ),
    header = ThemeHeader(
        alignment = HeaderAlignment.LEFT,
        logoUrl = null,
        button = ThemeHeaderButton(
            backgroundColor = null,
            iconColor = null
        )
    ),
    messages = ThemeMessages(
        assistant = AssistantMessageStyle(
            backgroundColor = "#FFFFFF",
            textColor = "#000000",
            borderColor = null,
            thinkingBorderGradient = emptyList()
        ),
        user = UserMessageStyle(
            backgroundColor = "#FFFFFF",
            textColor = "#000000",
            borderColor = null
        )
    ),
    input = ThemeInput(
        textColor = null,
        placeholderColor = null,
        backgroundColor = null,
        borderColor = null,
        sendButtonIconColor = null
    ),
    productCard = ThemeProductCard(discountPriceColor = null),
    font = ThemeFont(android = ThemeAndroidFont(assetUrl = ""))
)

internal fun sampleMessage(
    id: String = "message-id",
    role: MessageRole = MessageRole.ASSISTANT,
    parts: List<MessagePart> = listOf(sampleRichTextPart())
): Message = Message(
    id = id,
    role = role,
    parts = parts
)

internal fun sampleRichTextPart(
    id: String = "part-id",
    blocks: List<RichTextBlock> = listOf(sampleParagraph("Hello"))
): RichTextPart = RichTextPart(
    id = id,
    blocks = blocks
)

internal fun sampleParagraph(vararg texts: String): RichTextParagraphBlock =
    RichTextParagraphBlock(spans = texts.map { sampleSpan(text = it) })

internal fun sampleSpan(
    text: String = "Hello",
    type: RichTextSpanType = RichTextSpanType.TEXT,
    url: String? = null
): RichTextSpan = RichTextSpan(
    text = text,
    type = type,
    url = url
)

internal fun sampleBulletList(vararg texts: String): RichTextBulletListBlock = RichTextBulletListBlock(
    items = texts.map { RichTextBulletListItem(spans = listOf(sampleSpan(text = it))) }
)

internal fun sampleProductPart(
    id: String = "product-part-id",
    products: List<ProductCard> = listOf(sampleProductCard())
): ProductPart = ProductPart(
    id = id,
    products = products
)

internal fun sampleProductCard(
    id: String = "product-id",
    title: String = "Sneakers",
    description: String? = "Comfortable",
    imageUrl: String = "https://example.com/product.png",
    price: ProductPrice = ProductPrice(
        amount = 100f,
        currency = "DKK"
    ),
    originalPrice: ProductPrice? = null,
    url: String = "https://example.com/product"
): ProductCard = ProductCard(
    id = id,
    title = title,
    description = description,
    imageUrl = imageUrl,
    originalPrice = originalPrice,
    price = price,
    url = url
)

internal fun sampleTablePart(
    id: String = "table-part-id",
    caption: String? = null,
    headers: List<TableCell> = listOf(sampleTextCell("Size")),
    alignments: List<TableColumnAlignment> = emptyList(),
    rows: List<List<TableCell>> = listOf(listOf(sampleTextCell("38")))
): TablePart = TablePart(
    id = id,
    caption = caption,
    headers = headers,
    alignments = alignments,
    rows = rows
)

internal fun sampleTextCell(text: String): TableCell = TableCell(blocks = listOf(sampleParagraph(text)))

internal fun sampleImageCell(
    url: String = "https://example.com/image.png",
    thumbnailUrl: String? = null,
    alt: String? = null
): TableCell = TableCell(
    blocks = listOf(
        TableCellImageBlock(
            url = url,
            thumbnailUrl = thumbnailUrl,
            alt = alt
        )
    )
)

/** The UI shape [String.toUserMessageItem] and the rich text mapper produce for a single line of text. */
internal fun sampleRichTextUiModel(text: String): MessagePartUiModel.RichText = MessagePartUiModel.RichText(
    blocks = listOf(
        RichTextBlockUiModel.Paragraph(
            spans = listOf(
                RichTextSpanUiModel(
                    text = text,
                    type = RichTextSpanTypeUiModel.TEXT
                )
            )
        )
    )
)
