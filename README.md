# Nebula Telegram v34 — per-chat screenshot protection and privacy audit

## New code
- `NebulaChatPolicy`: centralized account+dialog-ID scoped policy access and durable writes.
- `ChatActivity`: applies Android `FLAG_SECURE` when the current chat has `secure_screen=true`; restores the global setting when the chat is paused.
- `NebulaChatRulesActivity`: exposes the new per-chat protection option.
- `NebulaChatHubActivity`: adds a confirmation-based quick toggle for that chat.
- `NebulaPrivacyAuditActivity`: read-only audit of global screenshot protection, PIN, background locking, and per-chat rules; linked from Nebula Features.
- AndroidManifest registers the new audit screen.

## Limitations
This is a **source overlay**, not a full Telegram project or APK. Android build and device tests have NOT been performed. FLAG_SECURE blocks normal Android screenshots/screen recording, not external cameras or every manufacturer-specific capture mechanism. Chat-level flags apply to the host Activity window; full multi-window/bubble behavior needs testing. Hidden chat labels and ghost mode are not implemented. Existing settings and code from v33 are retained.

## v35: локальний планувальник чернеток
- Єдиний екран для чернеток чату з правильним account/dialog_id.
- Редагування тексту, перенесення/скасування дати, фільтр прострочених чернеток, сортування за часом.
- Обмеження 500 записів і 20000 символів, перевірка результату збереження.
- Попередження про приватність перед копіюванням.
- Жодного автоматичного надсилання або сповіщень: час — лише локальний план.
- Це overlay, не повний Telegram проєкт; Android APK не компілювався.


## v36: локальний PIN на окремі чати
- `pin_lock` — opt-in правило для конкретного Telegram-акаунта й діалогу.
- Перевірка PIN через існуючий PBKDF2 verifier з обмеженням невдалих спроб.
- До успішної перевірки вміст фрагмента приховано; вихід повертає назад.
- При виході з чату повторно потрібен PIN; FLAG_SECURE для захищених чатів.
- Не перевірено компіляцією або на Android. Застосування потребує накладання ZIP на відповідну версію Telegram.


## v37: локальний менеджер прихованих чатів
- Окремий екран із пошуком, фільтром позначок, швидким перемиканням, відкриттям центру чату та копіюванням ID із попередженням.
- Позначки обмежені ключами `telegram:<account>:<dialogId>`; не змішуються між акаунтами.
- Додано перемикач до центру конкретного чату, shortcut у налаштуваннях та manifest registration.
- Усі дії локальні, opt-in; головний список чатів Telegram **ще не приховується**.
- PIN екрану працює лише якщо користувач увімкнув PIN Nebula.
- APK не збирався.

## v38: приховування чатів у звичайному списку
- `DialogsActivity.getDialogsArray` застосовує `NebulaDialogVisibility` лише для звичайного списку діалогів; оригінальні списки Telegram не змінюються.
- Фільтрація локальна, окремо за акаунтами; працює тільки для явно позначених `hidden=true` чатів.
- Вибір одержувачів, пересилання, інші типи списків і пошук не змінені.
- Після повернення на головний екран список оновлюється через наявний `onResume` notifyDataSetChanged.
- ВАЖЛИВО: приховування не захищає від пошуку, сповіщень, інших екранів, Telegram API чи доступу до даних. Не замінює PIN або шифрування.
- Компіляція APK і перевірка на Android не проводилися.

## v39: окремий системний менеджер HTTPS-завантажень
- Додано `NebulaDownloadsActivity`: введення HTTPS URL, черга системного Android DownloadManager, статус, байти, відкриття завершеного файлу, видалення/скасування.
- ID завдань зберігаються локально, без сканування чужих завантажень; відмова від HTTP і URL із вбудованими обліковими даними.
- Файли зберігаються у власному зовнішньому каталозі застосунку. Видалення завдання може видалити файл.
- Це НЕ менеджер медіафайлів Telegram і не перехоплює Telegram-завантаження.
- Android APK не збирався і не тестувався на пристрої.

## v40: покращений системний менеджер завантажень
- Реальний opt-in режим економії мобільного трафіку для нових HTTPS-завантажень (`setAllowedOverMetered(false)`); не змінює завантаження Telegram.
- Фільтр активних, завершених та невдалих завантажень, збереження вибраного фільтра.
- Підсумкові лічильники станів, коди причин паузи/помилки Android DownloadManager.
- Надсилання завершених файлів через Android ACTION_SEND з тимчасовим дозволом на читання URI.
- Не є APK, Android-компіляція й пристроєві тести не проводилися.

## v41: пошук та масове керування HTTPS-завантаженнями
- Пошук завдань за назвою, вихідною адресою або ID, сумісний із фільтрами статусів.
- Горизонтальний індикатор прогресу для завдань із відомим розміром.
- Масове очищення лише локальної історії завершених завантажень без видалення файлів чи системних завдань.
- Окреме очищення записів помилок та завдань, відсутніх у системному DownloadManager; без видалення файлів.
- Підтвердження масових дій, збереження змін через commit() і повідомлення про помилки.
- Обмеження: ці можливості не стосуються внутрішніх Telegram-завантажень. Android build/device tests not performed.


## v42 changes
- Per-chat PIN protection can be enabled/disabled from the chat hub (requires a configured Nebula PIN).
- Per-chat archive stats and destructive per-chat archive clearing with confirmation.
- Epoch guard invalidates queued archive writes when a chat/account is cleared; the existing synchronized writer still serializes active writes.
- Updated hidden-chat disclosure to reflect actual dialog-list integration.
- Validator checks overlay files instead of requiring a full source tree.
- No APK compilation or Android runtime test performed.


## v43: Experimental typing privacy
- Added an opt-in typing suppression check in the normal ChatActivity composer callback.
- Added a global toggle and per-account/per-chat opt-in rule, plus a dedicated explanation screen.
- This does **not** suppress read receipts, online presence, story views, or every Telegram typing-related pathway.
- Default OFF. Full Android build and runtime verification pending.


## v44: Experimental open-chat read suppression
- New global and per-chat opt-in setting for suppressing four read-marking calls in ChatActivity; defaults OFF.
- Does not intercept notifications, other screens, other devices or all controller read paths.
- Does not alter secret-chat read behavior. UI unread counts may differ from server state.
- Android compilation and runtime verification pending.

## v45: settings profiles overhaul
- Profile snapshots now include boolean toggles from both Nebula Features and global privacy preferences; they never include PIN, per-chat rules, accounts or chat data.
- Selectively restore features, global privacy or both; old profiles without privacy snapshots remain compatible.
- Restore no longer clears unrelated settings, and checks synchronous persistence results.
- Profile names limited to 60 characters; max 20 snapshots; failed saves/deletes are reported and rolled back in memory.
- No Android APK build or runtime test performed.


## v46: scoped local bookmarks
- Bookmark manager now supports optional account/chat context from Chat Hub; legacy bookmarks remain accessible in the general manager.
- Added case-insensitive search, edit, deletion confirmation, and persistent save checks.
- New entries require valid HTTPS URLs without embedded credentials, bounded titles/URLs, and a 500-item limit.
- No interception or bookmarking of actual Telegram messages; this is a local user-entered link manager.
- Android compilation and runtime testing pending.

## v47: unified local search and safer templates
- Rebuilt Nebula local search across scratchpad, per-chat notes, response templates, drafts, bookmarks and chat-rule flags.
- Added per-category controls, match counts, manager navigation and bounded scanning/results (1000/100) to avoid huge UI lists.
- Search is local/read-only and does not access Telegram messages, encrypted archives or server search.
- Templates now respect the existing optional clipboard warning and enforce bounded title/body/count; persistence reports failure.
- Android build and device tests are still pending.

## v48: ghost controls consistency and audit
- Fixed NebulaChatPolicy.set rejecting suppress_chat_read, which made the per-chat read toggle ineffective when saved via the centralized API.
- Added direct opt-in per-chat typing and read suppression switches in Chat Hub with confirmation and persistence failure reporting.
- Privacy audit now accurately describes existing hidden-dialog filtering and experimental read/typing suppression, including per-chat counts.
- Ghost settings now link to audit, and chat rule editor warns that arbitrary labels are not Telegram dialog identifiers.
- No additional server-side or device-wide privacy guarantees. Android build/runtime tests pending.


## v49: unified local search now includes opt-in encrypted archive
- Added a separate archive search category, **off by default** to avoid displaying private message text unexpectedly.
- Search scans bounded local archive snapshots and all stored edit versions; optionally filters deleted and edited messages.
- Tapping a match opens the archive manager prefiltered to its chat and query.
- The search remains local, does not query Telegram servers or undelete unknown messages.
- Result limits: 100 shown, 1000 scanned; Android compilation/runtime tests have not been run.


## v50 — експериментальні профілі приватності
- Три профілі (звичайний, тихе друкування, експериментальний привид) з атомарним збереженням.
- Підтвердження змін та відновлення UI після помилки запису.
- Підрахунок і вибіркове скидання правил привида для чатів; інші правила залишаються.
- Попередження про обмеження; це НЕ приховування онлайн чи переглядів історій.
- Android APK не зібрано та не протестовано.

## v51: безпечний перенос правил чатів
- JSON експорт лише правил реальних Telegram-діалогів, без PIN чи секретів.
- Імпорт із перевіркою формату, версії, ключів, типів значень, дублікатів і ліміту 2000 правил.
- Перевірка всього набору перед єдиним commit; інші записи не стираються.
- Підтвердження імпорту, попередження про буфер обміну; помилки збереження повідомляються.
- Android APK не збирався, компіляцію не проводили.


## v52
- Перероблено екран локальної статистики: архів, видалення, версії редагувань, чернетки, прострочені дати, нотатки, правила та вкладення.
- Додано статистику конкретного чату з account-scoped фільтрацією та переходами до архіву і чернеток.
- Жодні дані не надсилаються в мережу; статистика не охоплює всі повідомлення Telegram.
- Android APK не компілювався.


## v53: opt-in visual effects
- Added NebulaAppearancePolicy for persisted opt-in star visuals, reduced motion, animation, contrast, intensity, and atomic presets.
- Main DialogsActivity now adds the star overlay only when enabled (previously unconditional).
- Stars view honors reduced-motion, animation toggle, intensity and contrast; avoids unnecessary continuous redraw in static mode.
- Appearance UI includes off/Nebula/accessibility presets and explicit labels for not-yet-integrated features.
- Other theme toggles remain partial/not yet integrated. No Android APK build or runtime validation performed.


## v54: opt-in gesture shortcut pad
- Dedicated gesture pad in the chat hub supports double tap, left swipe, and right swipe without hijacking Telegram native swipe actions.
- Each gesture can open account/chat-scoped notes, archive, drafts, stats, or rules; actions are configurable independently.
- Gestures default OFF, have a reset option, and use synchronous preference commits with error feedback.
- Settings shortcut and non-exported Android manifest registration added.
- Not integrated into the Telegram chat list or message bubbles. No APK build/runtime test.


## v55: atomic per-chat privacy presets
- Four explicit presets: standard, screenshot protection, experimental quiet chat, experimental hidden chat.
- Single synchronous write preserves unrelated per-chat flags, including PIN and archive exclusion.
- Added account/dialog-scoped privacy dashboard in the chat hub, with confirmation and live rule summary.
- Fixed chat note saving, archive-exclusion saving and mass draft deletion to report synchronous persistence failures.
- Experimental typing/read controls do not guarantee invisibility; hidden chats may still appear in search/notifications.
- This is a source overlay. Android build and runtime testing remain outstanding.


## v56: перевірка фільтрів на реально завантажених повідомленнях
- Додано пункт у меню звичайного чату Telegram для ручного аналізу вже завантажених текстових повідомлень.
- Застосовуються активні глобальні та чат-специфічні правила (слова / regex), окремо для кожного акаунта й чату.
- Показуються кількість перевірених повідомлень, збігів, ID, короткий уривок і кількість правил.
- Захист інтерфейсу: до 500 повідомлень, 100 активних правил, 40 результатів, 110 символів на уривок.
- Не сканує секретні чати; не зберігає повідомлення; не надсилає їх на сервер; не змінює Telegram-повідомлення.
- Це ручний перегляд, а не автоматичне приховування/блокування повідомлень.
- Android compilation and device tests NOT performed.


## v57: режим перевірки реальних повідомлень
- Додано вибір перевірки всіх, лише вхідних або лише вихідних завантажених повідомлень.
- Показуються окремі підсумки збігів для вхідних і вихідних повідомлень.
- Додано копіювання результатів із окремим підтвердженням приватності буфера обміну.
- Не змінюються серверні повідомлення, історія прочитань чи Telegram API.
- Обмеження 500 перевірених повідомлень і 40 показаних збігів збережено.
- Android compilation/device tests NOT performed.


## v58: gesture overlay in Telegram chat menu
- Added a separate, opt-in gesture surface directly accessible from the Telegram chat options menu.
- Double tap and left/right swipes execute configured local actions with the real Telegram account and dialog ID.
- Shows the currently assigned actions and provides a shortcut to gesture settings.
- Does not override Telegram native swipe gestures, does not change messages or network traffic.
- Menu entry is only visible when gestures are enabled and the chat is not secret.
- No Android build or device test has been performed.

## v59: деталізована перевірка фільтрів повідомлень
- Ручна перевірка повідомлень тепер показує кількість збігів для кожного активного правила.
- Додано лічильники пропусків за напрямком і повідомлень без тексту.
- Збережено розділення вхідних/вихідних, обмеження 500 сканувань та 40 прикладів, приватне підтвердження копіювання.
- Жодного автоматичного приховування, надсилання даних чи змін серверного стану.
- Не перевірено компіляцією Android.


## v60: напрямок для кожного правила фільтрації
- Кожне правило може застосовуватись до всіх повідомлень, лише вхідних або лише вихідних.
- Ручний аналіз завантажених повідомлень тепер враховує напрямок кожного правила, а не тільки загальний фільтр перевірки.
- Редактор і тестер дозволяють вибрати напрямок; дублювання, експорт та імпорт його зберігають.
- Старі правила залишаються сумісними: відсутнє поле direction означає «усі».
- Валідація відхиляє невідомі значення напрямку й надто довгі ключі чатів.
- Автоматичне приховування повідомлень не реалізовано. Android-компіляції та тестів не було.


## v61: пошук та резервне копіювання чернеток
- Пошук за текстом і назвою чату, окремий фільтр чернеток із датою; працює разом із фільтром прострочених.
- Експорт видимих чернеток у версійований JSON через підтвердження приватного буфера обміну.
- Імпорт із перевіркою формату, довжини, дат, кількості; попередній перегляд кількості та підтвердження.
- У режимі конкретного чату імпорт не дозволяє додати записи іншого чату/акаунта.
- Автоматичне надсилання, системні нагадування та Android-збірка не реалізовані.


## v62: пакетні операції з чернетками
- Додано ручну передачу тексту чернетки через Android Sharesheet (користувач сам обирає застосунок; жодного автоматичного надсилання).
- Додано копіювання всіх чернеток, час яких настав, із перевіркою приватності буфера обміну та обмеженням 100000 символів.
- Додано масове видалення прострочених чернеток із підтвердженням, обмеженням поточним чатом у режимі чату та перевіркою збереження.
- Автоматичне надсилання, фонові сповіщення, Android compilation/device tests НЕ реалізовано.


## v63 — draft completion workflow
- Local done/undone flag with completion timestamp; no automatic Telegram send.
- Due counts, due copy, and due deletion exclude completed drafts.
- Duplicate draft action resets completion state.
- Separate completed/unfinished filter; export respects it.
- Import validates optional completed flag; older backups remain supported.
- Android compilation and runtime tests NOT performed.


## v64 — пакетні операції з чернетками
- Прапорці вибору чернеток у відфільтрованому списку; вибрати всі показані або зняти вибір.
- Масове позначення виконаними та повернення до невиконаних із підтвердженням.
- Масове видалення вибраних із підтвердженням, без впливу на повідомлення Telegram.
- Вибір обмежено поточним чатом/акаунтом і видимими фільтрами; після операції вибір очищується.
- Android compilation/runtime tests not performed.


## v65 — локальні нагадування
- Опційні системні нагадування про майбутні чернетки через AlarmManager (inexact).
- Android 13+ запит дозволу сповіщень, прихований текст у повідомленні, відновлення після перезавантаження та зміни часу.
- Після зміни чернеток переплановується найближче нагадування.
- Немає автоматичного надсилання, гарантованої точності часу або перевірки APK.


## v66 — draft postponement and schedule overview
- Per-draft postponement by 10 or 60 minutes for unfinished drafts, from the current time.
- Batch postpone selected unfinished drafts by 60 minutes, with explicit confirmation.
- Planner summary shows upcoming count and nearest upcoming scheduled time within current chat scope.
- Persisting a postponed draft reuses existing reminder rescheduling, when reminders are opted in.
- Does not send Telegram messages. Android build/device tests have not been performed.


## v67: opt-in local text masking (experimental)
- Added NebulaAutoMask and an OFF-by-default switch in the filter editor.
- In ordinary ChatActivity rows, matching plain-text messages render as a distinct local placeholder. No server-side deletion or message mutation.
- Explicitly excludes secret chats, media/service rows and non-default/search chat modes.
- Rules honor per-chat scope, incoming/outgoing direction, regex and active state.
- Existing search, notifications, forwarding and other Telegram surfaces can still reveal original text.
- Reopen chat to apply changes. Android compilation and device tests have NOT been performed.

## v68: per-chat auto-mask exclusion
- Added opt-in per-account/per-dialog exclusion for automatic message masking, independent of global rules.
- Added a checkbox in the chat-scoped filter editor to disable/re-enable local masking for one chat.
- Existing rule matching, Telegram messages, and server state are unchanged.
- Changes become visible after reopening the chat; this is a source overlay, not a tested APK.


## v69: керування винятками і правилами окремого чату
- Перегляд локальних винятків маскування за акаунтом і chat ID.
- Прибирання одного винятку або всіх із подвійним підтвердженням для масового очищення.
- Пакетне вмикання/вимикання лише правил, явно прив'язаних до поточного чату; глобальні правила не зачіпаються.
- Не змінює Telegram-повідомлення, не вмикає фільтрацію автоматично.
- Android compilation/runtime tests NOT performed.


## v70: global pause for experimental ghost rules
- Added OFF-by-default `ghost_paused` override in NebulaGhostPolicy for both typing and selected chat read acknowledgement suppression.
- The override also disables per-chat rules without deleting them, and is reversible.
- Added confirmation and visible pause control in NebulaGhostActivity, plus audit status.
- Existing two preset switches remain unchanged when toggling pause.
- No promise of Telegram-wide stealth; previously sent status events cannot be undone.
- Static checks only; Android compilation/device testing not performed.

## v71: Opt-in message double tap
- A configured Nebula double-tap action now works on ordinary ChatMessageCell messages in ChatActivity.
- Defaults remain OFF / NONE. When enabled, it overrides Telegram's double-tap reaction for eligible messages.
- Secret chats, scheduled/preview modes, sponsored, sending and service messages are excluded.
- Swipe gestures still work only in the Nebula gesture panel; no Telegram message swipe interception.
- Source overlay only; Android compilation and device testing have not been performed.

## v72: Per-chat gesture actions
- Optional account+dialog scoped overrides for double tap, swipe left and swipe right.
- Per-chat chooser in the chat hub can inherit global action, explicitly choose NONE, or choose a Nebula shortcut.
- Message double tap and the dedicated gesture pads now use effective chat action.
- Global master enable remains OFF by default and gates all shortcuts.
- Telegram message swipes are NOT intercepted; swipes still work only on Nebula panels.
- Source overlay only; Android build and runtime tests NOT performed.

## v73: Fix per-chat double-tap eligibility
- Fixed mismatch: ChatActivity.hasDoubleTap now checks the effective per-chat action, not the global action.
- A chat-specific action can activate a double tap even when the global action is NONE (global master switch still required).
- A chat-specific NONE suppresses the Nebula override and leaves Telegram reaction handling intact.
- Added a source-level regression test for the eligibility/dispatch pairing.
- Android build/device behavior not tested.

## v74 — opt-in left swipe directly on messages
- Telegram's existing left-swipe-to-reply completion path now checks a Nebula per-chat/global left-swipe shortcut.
- When explicitly enabled and assigned, it launches the selected Nebula chat tool; otherwise the native Telegram reply gesture remains unchanged.
- Excludes secret, scheduled, preview and special chat modes and unsupported messages.
- Right-swipe-on-message remains unimplemented. Source checks only; no Android build or device test.

## v75 — scoped gesture reset and audit
- Added account/dialog-scoped bulk reset for all three chat gesture overrides, with confirmation.
- Added a count of explicit overrides (including explicit NONE) in the gesture settings when opened with a chat context.
- Does not change global gesture configuration or other chats; no Android compile/device test.

## v76: timed ghost pause
- Added opt-in 15-minute and 60-minute pauses for experimental typing/read interception.
- Pause is persisted as an expiry timestamp and automatically stops applying when the deadline passes, including across app restarts.
- A separate cancel action clears only the timed pause; permanent pause is unaffected.
- No guarantee of hiding online status, story views, or all Telegram read/typing events.
- Source overlay only; Android compilation and device tests not performed.
