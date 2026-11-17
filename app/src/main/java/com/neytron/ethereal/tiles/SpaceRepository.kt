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
                        canvasManual = obj.optBoolean("canvasManual", false)
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
     * Раскладка канваса обзора.
     *
     * Дети встают кольцом вокруг своего родителя (по часовой стрелке, в порядке создания),
     * поэтому структура читается как дерево, а не как случайные точки. ROOT — в (0, 0).
     * Ручные позиции ([SpaceMeta.canvasManual]) не трогаем.
     */
    fun ensureCanvasPositions(context: Context): List<SpaceMeta> {
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

        val positions = HashMap<String, Pair<Float, Float>>()
        positions[rootId] = 0f to 0f

        val queue = ArrayDeque<String>()
        queue.add(rootId)
        val visited = HashSet<String>()

        while (queue.isNotEmpty()) {
            val parent = queue.removeFirst()
            if (!visited.add(parent)) continue

            val kids = childrenOf[parent].orEmpty()
                .sortedBy { byId[it]?.createdAt ?: 0L }
            if (kids.isEmpty()) continue

            val (px, py) = positions[parent] ?: (0f to 0f)
            val count = kids.size
            // Кольцо: при 1–4 детях радиус фиксированный, дальше чуть растёт
            val radius = if (count <= 4) 800.0 else 800.0 + (count - 4) * 140.0

            kids.forEachIndexed { index, childId ->
                if (positions.containsKey(childId)) return@forEachIndexed
                val angle = -Math.PI / 2.0 + index * (2.0 * Math.PI / count)
                positions[childId] = (px + Math.cos(angle) * radius).toFloat() to
                    (py + Math.sin(angle) * radius).toFloat()
                queue.add(childId)
            }
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

        if (changed) saveAll(context, updated)
        return updated
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
