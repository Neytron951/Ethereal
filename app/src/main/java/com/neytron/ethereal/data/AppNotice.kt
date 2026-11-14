package com.neytron.ethereal.data

/**
 * Разовое сообщение пользователям.
 *
 * Показывается **один раз** после онбординга и больше не появляется — независимо от обновлений.
 * Факт показа хранится флагом в настройках, поэтому текст можно менять без повторного показа.
 *
 * Как пользоваться:
 *  1. Напиши текст в [Notices.CURRENT] (заголовок + тело).
 *  2. Разметка тела — упрощённый Markdown:
 *     - `## Заголовок`, `### Подзаголовок`
 *     - `- пункт списка`
 *     - `**жирный**`, `*курсив*`, `` `код` ``, `[текст](https://...)`
 *     - `---` разделитель
 *  3. Чтобы совсем отключить сообщение — поставь `val CURRENT: AppNotice? = null`.
 *
 * ВАЖНО: это не патчноут. Для заметок к релизу есть отдельный рендер в Update Center.
 */
data class AppNotice(
    val title: String,
    /** Тело сообщения в упрощённом Markdown (см. комментарий выше). */
    val body: String,
    /** Необязательная кнопка-ссылка под текстом. */
    val linkUrl: String? = null,
    val linkLabel: String? = null
)

object Notices {

    // ============================================================
    // ↓↓↓ ТЕКСТ СООБЩЕНИЯ — МЕНЯЙ ЗДЕСЬ ↓↓↓
    // ============================================================
    val CURRENT: AppNotice? = AppNotice(
        title = "A message from the developer",
        body = """
            Hi everyone. Since the project has no Telegram channel or other social media, I'm reaching out to you right here through the launcher itself.

            I want to say a big thank you for using Ethereal. It may sound cliché, but I'm genuinely glad the project is finding an audience. With that in mind, I'd love to get your feedback.

            ## Feedback
            - If you like Ethereal, please **star it on GitHub** — it's the main place for rating and stars really matter there.
            - If something is missing in Ethereal or feels "weak", **open an issue on GitHub**. It's not hard — no strict reporting or formatting required.
            - The project uses **Firebase Crashlytics**, but the info there doesn't always capture the full picture of a problem.

            *P.S. If Firebase bothers you, you can disable it on the About page.*

            ---

            ## About distribution
            The main code will always live on **GitHub**, since it's a platform with no moderation and content appears the moment it's released.

            All APKs are signed with my personal key. If you downloaded Ethereal from somewhere other than the official GitHub repo and Android shows an incompatibility error with a previously installed Ethereal, then you're using a **fork / unofficial build**. Proceed at your own risk.

            ---

            A special thanks to everyone helping spread Ethereal — writing posts or adding the repository to app stores. For a small project, that really matters.

            — Neytron951
        """.trimIndent(),
        linkUrl = "https://github.com/Neytron951/Ethereal",
        linkLabel = "Star on GitHub"
    )
    // ============================================================
    // ↑↑↑ ТЕКСТ СООБЩЕНИЯ — МЕНЯЙ ЗДЕСЬ ↑↑↑
    // ============================================================
}