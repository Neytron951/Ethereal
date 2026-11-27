package com.neytron.ethereal.tiles

import android.content.Context
import com.neytron.ethereal.ui.navigation.ScreenStack
import org.json.JSONArray
import org.json.JSONObject
import com.neytron.ethereal.widgets.EtherealAppWidgetHost
import com.neytron.ethereal.widgets.WidgetHostManager
import java.util.UUID

/**
 * Реестр всех пространств.
 * Хранит метаданные (id, name, iconUri, iconText, createdAt) + умеет чистить раскладки.
 *
 * Раскладки (tiles) хранит TileLayoutRepository по ключу tiles_space_<id>.
 * Здесь — только метаданные, чтобы знать, какие пространства вообще существуют.
 */
object SpaceRepository {

    private const val PREFS = "ethereal_settings"
    private const val KEY_SPACES_META = "spaces_meta"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ============================================================
    // Метаданные
    // ============================================================

    data class SpaceMeta(
        val id: String,
        val name: String,
        val iconUri: String?,
        val iconText: String?,
        val createdAt: Long,
        /** Позиция карточки на канвасе обзора (dp). NaN — ещё не размещена. */
        val canvasX: Float = Float.NaN,
        val canvasY: Float = Float.NaN,
        /** true — карточку перетащили вручную, авто-раскладка её не двигает. */
        val canvasManual: Boolean = false
    )

    fun getAllSpaces(context: Context): List<SpaceMeta> {
        val raw = prefs(context).getString(KEY_SPACES_META, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<SpaceMeta>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SpaceMeta(
                        id = obj.getString("id"),
                        name = obj.optString("name", "Space").ifBlank { "Space" },
                        iconUri = obj.optString("iconUri", "").takeIf { it.isNotEmpty() },
                        iconText = obj.optString("iconText", "").takeIf { it.isNotEmpty() },
                        createdAt = obj.optLong("createdAt", 0L),
                        canvasX = obj.optDouble("canvasX", Double.NaN).toFloat(),
                        canvasY = obj.optDouble("canvasY", Double.NaN).toFloat(),
                        canvasManual = obj.optBoolean("canvasManual", false),
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getSpace(context: Context, spaceId: String): SpaceMeta? =
        getAllSpaces(context).firstOrNull { it.id == spaceId }

    /**
     * Раскладка мира пространств.
     *
     * Пространство в мире — это настоящий экран размером [screenWidthDp]×[screenHeightDp],
     * поэтому канвас держит структуру деревом: глубина — колонка, братья — вертикаль.
     * Родитель стоит по центру своих детей, между ними остаётся зазор, в котором
     * видна нитка. Поддеревья не перекрываются. ROOT всегда в (0, 0).
     *
     * Ручные позиции ([SpaceMeta.canvasManual]) не трогаем — их человек
     * расставил сам, и они переживают пересчёт.
     */
    fun ensureCanvasPositions(
        context: Context,
        screenWidthDp: Float,
        screenHeightDp: Float
    ): List<SpaceMeta> {
        migrateCanvasLayout(context, screenWidthDp)

        val spaces = getAllSpaces(context)
        if (spaces.isEmpty()) return spaces

        val byId = spaces.associateBy { it.id }
        val rootId = ScreenStack.ROOT_ID

        // Родитель = пространство, в чьей раскладке лежит портал в ребёнка
        val parentOf = HashMap<String, String>()
        val childrenOf = LinkedHashMap<String, MutableList<String>>()

        fun scanLayout(ownerId: String) {
            val tiles = TileLayoutRepository.getTiles(context, ownerId)
            tiles.asSequence()
                .filter { it.type == TileType.SPACE && !it.payload.isNullOrBlank() }
                .forEach { tile ->
                    val childId = tile.payload!!
                    if (childId == ownerId) return@forEach
                    if (!byId.containsKey(childId)) return@forEach
                    if (parentOf.containsKey(childId)) return@forEach
                    parentOf[childId] = ownerId
                    childrenOf.getOrPut(ownerId) { mutableListOf() }.add(childId)
                }
        }

        scanLayout(rootId)
        spaces.forEach { scanLayout(it.id) }

        // Пространства без портала-родителя подвешиваем к ROOT, чтобы не потерялись
        spaces.forEach { meta ->
            if (!parentOf.containsKey(meta.id)) {
                parentOf[meta.id] = rootId
                childrenOf.getOrPut(rootId) { mutableListOf() }.add(meta.id)
            }
        }

        val frameW = screenWidthDp.coerceAtLeast(320f)
        val frameH = screenHeightDp.coerceAtLeast(480f)
        // Зазор между колонками — в нём должна быть видна нитка родитель → ребёнок.
        val gapX = (frameW * 0.55f).coerceAtLeast(170f)
        val gapY = (frameH * 0.10f).coerceAtLeast(56f)
        val stepX = frameW + gapX
        val stepY = frameH + gapY

        val positions = HashMap<String, Pair<Float, Float>>()
        val placed = HashSet<String>()
        var leafCursor = 0f

        /**
         * Дерево, а не лента: глубина — это колонка (x), братья — вертикаль (y).
         * Лист занимает следующий свободный слот, родитель встаёт по центру
         * своих детей. Поэтому структура читается как древо, и поддеревья
         * никогда не наезжают друг на друга.
         */
        fun layoutSubtree(id: String, depth: Int): Float {
            placed.add(id)
            val kids = childrenOf[id].orEmpty()
                .filter { byId.containsKey(it) && !placed.contains(it) }
                .sortedBy { byId[it]?.createdAt ?: 0L }

            val centerY: Float
            if (kids.isEmpty()) {
                centerY = leafCursor + frameH / 2f
                leafCursor += stepY
            } else {
                var first = 0f
                var last = 0f
                kids.forEachIndexed { index, childId ->
                    val childCenter = layoutSubtree(childId, depth + 1)
                    if (index == 0) first = childCenter
                    last = childCenter
                }
                centerY = (first + last) / 2f
            }

            positions[id] = (depth * stepX) to (centerY - frameH / 2f)
            return centerY
        }

        layoutSubtree(rootId, 0)

        // Кто не попал в дерево (циклы, битые порталы) — отдельными слотами справа,
        // но не теряем: они остаются на канвасе.
        spaces.forEach { meta ->
            if (!positions.containsKey(meta.id)) {
                positions[meta.id] = stepX to leafCursor
                leafCursor += stepY
            }
        }

        // ROOT всегда в (0, 0): от него отсчитывается камера и все остальные.
        val rootPos = positions[rootId] ?: (0f to 0f)
        positions.replaceAll { _, pos ->
            (pos.first - rootPos.first) to (pos.second - rootPos.second)
        }

        var changed = false
        val updated = spaces.map { meta ->
            if (meta.canvasManual) return@map meta
            val pos = positions[meta.id] ?: return@map meta
            if (meta.canvasX != pos.first || meta.canvasY != pos.second) {
                changed = true
                meta.copy(canvasX = pos.first, canvasY = pos.second)
            } else {
                meta
            }
        }

        if (changed) {
            // Перечитываем перед записью: пока считали раскладку, человек мог
            // перетащить экран. Его ручную позицию авто-раскладка не затирает.
            val latest = getAllSpaces(context).associateBy { it.id }
            val merged = updated.map { meta ->
                val fresh = latest[meta.id]
                if (fresh != null && fresh.canvasManual && !meta.canvasManual) fresh else meta
            }
            saveAll(context, merged)
            return merged
        }
        return updated
    }

    /** Версия раскладки мира: 1 — старые карточки 230×480, 3 — дерево экранов. */
    private const val KEY_CANVAS_VERSION = "spaces_canvas_version"
    private const val CANVAS_VERSION = 3

    /**
     * Старые позиции считались для карточек 230×480 dp. Не выбрасываем ручную
     * расстановку, а переводим её в новый масштаб экранов: человек свою
     * раскладку не теряет (в версии 2 она, к сожалению, уже сбрасывалась).
     */
    private fun migrateCanvasLayout(context: Context, screenWidthDp: Float) {
        val prefs = prefs(context)
        val version = prefs.getInt(KEY_CANVAS_VERSION, 1)
        if (version >= CANVAS_VERSION) return

        if (version < 2) {
            val factor = (screenWidthDp / 230f).coerceIn(1f, 3f)
            val spaces = getAllSpaces(context)
            if (spaces.isNotEmpty()) {
                saveAll(
                    context,
                    spaces.map { meta ->
                        if (meta.canvasManual && !meta.canvasX.isNaN() && !meta.canvasY.isNaN()) {
                            meta.copy(canvasX = meta.canvasX * factor, canvasY = meta.canvasY * factor)
                        } else {
                            meta
                        }
                    }
                )
            }
        }
        prefs.edit().putInt(KEY_CANVAS_VERSION, CANVAS_VERSION).apply()
    }

    /** Сохраняет позицию карточки, перетащенной пользователем. */
    fun updateCanvasPosition(context: Context, spaceId: String, x: Float, y: Float): Boolean {
        val spaces = getAllSpaces(context).toMutableList()
        val index = spaces.indexOfFirst { it.id == spaceId }
        if (index < 0) return false
        spaces[index] = spaces[index].copy(canvasX = x, canvasY = y, canvasManual = true)
        saveAll(context, spaces)
        return true
    }

    fun exists(context: Context, spaceId: String): Boolean =
        getSpace(context, spaceId) != null

    private fun saveAll(context: Context, list: List<SpaceMeta>) {
        val arr = JSONArray()
        list.forEach { meta ->
            val obj = JSONObject().apply {
                put("id", meta.id)
                put("name", meta.name)
                put("iconUri", meta.iconUri ?: "")
                put("iconText", meta.iconText ?: "")
                put("createdAt", meta.createdAt)
                if (!meta.canvasX.isNaN() && !meta.canvasY.isNaN()) {
                    put("canvasX", meta.canvasX.toDouble())
                    put("canvasY", meta.canvasY.toDouble())
                }
                if (meta.canvasManual) put("canvasManual", true)
            }
            arr.put(obj)
        }
        prefs(context).edit().putString(KEY_SPACES_META, arr.toString()).apply()
    }

    // ============================================================
    // CRUD
    // ============================================================

    /**
     * Создаёт пространство и пустую раскладку для него.
     * Возвращает spaceId.
     */
    fun createSpace(
        context: Context,
        name: String,
        iconUri: String?,
        iconText: String?
    ): String {
        val id = UUID.randomUUID().toString()
        val current = getAllSpaces(context)
        // Координаты не задаём: канвас разложит пространство вокруг родителя
        val meta = SpaceMeta(
            id = id,
            name = name.ifBlank { "Space" },
            iconUri = iconUri,
            iconText = iconText,
            createdAt = System.currentTimeMillis()
        )
        val updated = current.toMutableList()
        updated.add(meta)
        saveAll(context, updated)

        // Пустая раскладка
        TileLayoutRepository.setTiles(context, id, emptyList())

        return id
    }

    /**
     * Обновляет имя/иконку пространства.
     * Раскладку НЕ трогает.
     */
    fun updateSpace(
        context: Context,
        spaceId: String,
        name: String,
        iconUri: String?,
        iconText: String?
    ): Boolean {
        val current = getAllSpaces(context).toMutableList()
        val index = current.indexOfFirst { it.id == spaceId }
        if (index < 0) return false

        current[index] = current[index].copy(
            name = name.ifBlank { "Space" },
            iconUri = iconUri,
            iconText = iconText
        )
        saveAll(context, current)
        return true
    }

    /**
     * Удаляет пространство: метаданные + раскладку.
     * Виджеты внутри нужно удалить ДО вызова этого метода (см. MainActivity).
     */
    fun deleteSpace(context: Context, spaceId: String): Boolean {
        val current = getAllSpaces(context).toMutableList()
        val removed = current.removeAll { it.id == spaceId }
        if (!removed) return false

        saveAll(context, current)

        // Чистим раскладку
        TileLayoutRepository.clearTiles(context, spaceId)

        return true
    }

    /**
     * Удаляет пространство вместе со всеми вложенными (рекурсивно).
     *
     * Раньше дочерние пространства оставались «сиротами»: их раскладки и метаданные
     * жили дальше, хотя портал в них уже удалён. Теперь дерево вычищается целиком.
     */
    fun deleteSpaceRecursive(context: Context, spaceId: String) {
        val visited = mutableSetOf<String>()
        val order = mutableListOf<String>()

        fun collect(id: String) {
            if (!visited.add(id)) return
            TileLayoutRepository.getTiles(context, id).forEach { tile ->
                if (tile.type == TileType.SPACE && !tile.payload.isNullOrBlank()) {
                    collect(tile.payload!!)
                }
            }
            order.add(id) // потомки раньше родителя
        }

        collect(spaceId)

        // Убираем порталы, ведущие в удаляемые пространства, из чужих раскладок:
        // иначе после удаления остаётся плитка, которая открывает пустоту.
        val deleting = order.toSet()
        val owners = listOf(ScreenStack.ROOT_ID) + getAllSpaces(context).map { it.id }
        owners.forEach { ownerId ->
            if (ownerId in deleting) return@forEach
            val ownerTiles = TileLayoutRepository.getTiles(context, ownerId)
            val filtered = ownerTiles.filterNot { tile ->
                tile.type == TileType.SPACE && !tile.payload.isNullOrBlank() && tile.payload in deleting
            }
            if (filtered.size != ownerTiles.size) {
                TileLayoutRepository.setTiles(context, ownerId, filtered)
            }
        }

        order.forEach { id ->
            TileLayoutRepository.getTiles(context, id).forEach { inner ->
                if (inner.type == TileType.WIDGET) {
                    EtherealAppWidgetHost.decodeWidgetId(inner.config)?.let { wid ->
                        WidgetHostManager.removeWidget(context, wid)
                    }
                }
                if (inner.type == TileType.APP_LAUNCHER || inner.type == TileType.SETTINGS) {
                    AppLauncherConfig.fromJson(inner.config).imageUri?.let { path ->
                        runCatching { java.io.File(path).delete() }
                    }
                }
                if (inner.type == TileType.SPACE) {
                    SpaceConfig.fromJson(inner.config).iconUri?.let { path ->
                        runCatching { java.io.File(path).delete() }
                    }
                }
            }
            deleteSpace(context, id)
        }
    }
}