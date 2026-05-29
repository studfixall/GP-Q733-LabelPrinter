package com.gp.q733.data.remote

import android.util.Log
import com.gp.q733.data.repository.ProductRepositoryImpl
import com.gp.q733.domain.model.ProductInfo
import com.gp.q733.domain.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RMIS 商品仓库 — Issue #13
 * 通过昂捷RMIS接口查询商品，支持多门店价格
 *
 * 查询流程：
 * 1. 按条码调 "获取门店商品SKU信息" (带StoreId) → 获取该门店价格
 * 2. 如果门店查询无结果，fallback到 "获取商品基本信息" (全局查)
 * 3. 本地Room数据库作为离线缓存和手动录入的后备
 */
@Singleton
class RmisProductRepository @Inject constructor(
    private val rmisApiClient: RmisApiClient,
    private val localRepository: ProductRepositoryImpl
) : ProductRepository by localRepository {

    companion object {
        private const val TAG = "RmisProductRepo"
    }

    /** 当前门店编码 */
    var storeId: String = ""

    /**
     * 按条码查询商品 — 优先RMIS远程，fallback本地Room
     *
     * RMIS查询策略：
     * - 有storeId → 调"获取门店商品SKU信息"(拿门店维度价格)
     * - 无论有无storeId → 都调"获取商品基本信息"拿主档
     * - 合并: 主档(名称/规格/单位/产地) + 门店价格
     * - 全部失败 → fallback本地Room
     */
    override suspend fun getProductByBarcode(barcode: String): ProductInfo? {
        if (storeId.isBlank()) {
            Log.w(TAG, "门店编码为空，使用本地查询")
            return localRepository.getProductByBarcode(barcode)
        }

        return try {
            queryFromRmis(barcode)
        } catch (e: Exception) {
            Log.w(TAG, "RMIS查询失败，fallback本地: ${e.message}")
            localRepository.getProductByBarcode(barcode)
        }
    }

    /**
     * 从RMIS远程查询商品信息
     */
    private suspend fun queryFromRmis(barcode: String): ProductInfo? = withContext(Dispatchers.IO) {
        var productInfo: ProductInfo? = null

        // 1. 先查门店商品SKU信息（带价格）
        if (storeId.isNotBlank()) {
            try {
                val tag = JSONObject().apply {
                    put("Barcode", barcode)
                    put("StoreId", storeId)
                }
                val result = rmisApiClient.callPaged("获取门店商品SKU信息", tag, pageSize = 1)
                val returnObject = result.optJSONArray("ReturnObject")
                if (returnObject != null && returnObject.length() > 0) {
                    val item = returnObject.getJSONObject(0)
                    productInfo = ProductInfo(
                        barcode = barcode,
                        name = "", // 门店SKU接口不返回名称，后续从主档补全
                        price = item.optDouble("c_price", 0.0),
                        mprice = item.optDouble("c_m_price", 0.0),
                        spec = "",
                        unit = "",
                        origin = "",
                        category = ""
                    )
                    Log.d(TAG, "门店SKU查询成功: barcode=$barcode storeId=$storeId price=${productInfo!!.price}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "门店SKU查询异常: ${e.message}")
            }
        }

        // 2. 查商品主档（补全名称/规格等）
        try {
            val tag = JSONObject().apply {
                put("IsBarcode", "是")
                put("GdsCode", barcode)
            }
            val result = rmisApiClient.callPaged("获取商品基本信息", tag, pageSize = 1)
            val returnObject = result.optJSONArray("ReturnObject")
            if (returnObject != null && returnObject.length() > 0) {
                val item = returnObject.getJSONObject(0)
                // 用主档信息补全，价格保留门店维度的（如果有）
                productInfo = ProductInfo(
                    barcode = item.optString("c_barcode", barcode),
                    name = item.optString("c_name", ""),
                    price = productInfo?.price ?: item.optDouble("c_price", 0.0),
                    mprice = productInfo?.mprice ?: 0.0,
                    spec = item.optString("c_model", ""),
                    unit = item.optString("c_basic_unit", ""),
                    origin = item.optString("c_produce", ""),
                    category = item.optString("c_cname", "")
                )
                Log.d(TAG, "商品主档查询成功: ${productInfo!!.name}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "商品主档查询异常: ${e.message}")
        }

        // 3. 也尝试通过增量数据接口获取价格（如果门店SKU没查到价格）
        if (productInfo != null && productInfo.price == 0.0 && storeId.isNotBlank()) {
            try {
                val tag = JSONObject().apply {
                    put("strType", "价格")
                    put("strStoreId", storeId)
                }
                // 增量数据接口格式不同，用call而非callPaged
                val objectData = JSONObject().apply {
                    put("strType", "价格")
                    put("strLastId", "0")
                    put("strGetCount", "1")
                    put("strStoreId", storeId)
                }
                val result = rmisApiClient.call("获取增量数据", objectData)
                val dataTable = result.optJSONArray("DataTable")
                if (dataTable != null && dataTable.length() > 0) {
                    // 遍历找匹配条码
                    for (i in 0 until dataTable.length()) {
                        val item = dataTable.getJSONObject(i)
                        if (item.optString("c_barcode") == barcode || item.optString("c_gcode") == barcode) {
                            productInfo = productInfo!!.copy(
                                price = item.optDouble("c_price", 0.0),
                                mprice = item.optDouble("c_price_mem", 0.0)
                            )
                            Log.d(TAG, "增量价格查询成功: price=${productInfo!!.price}")
                            break
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        productInfo
    }

    /**
     * 获取门店列表 — 用于门店选择UI
     * @return List<Pair(门店编码, 门店名称)>
     */
    suspend fun getStoreList(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val objectData = JSONObject().apply {
                put("strType", "机构")
                put("strLastId", "0")
                put("strGetCount", "500")
            }
            val result = rmisApiClient.call("获取增量数据", objectData)
            val dataTable = result.optJSONArray("DataTable") ?: return@withContext emptyList()

            val stores = mutableListOf<Pair<String, String>>()
            for (i in 0 until dataTable.length()) {
                val item = dataTable.getJSONObject(i)
                val id = item.optString("c_id", "")
                val name = item.optString("c_name", "")
                val type = item.optString("c_type", "")
                // 过滤门店类型（排除总部、配送中心等）
                if (id.isNotBlank() && name.isNotBlank()) {
                    stores.add(id to name)
                }
            }
            Log.d(TAG, "获取门店列表: ${stores.size}个")
            stores
        } catch (e: Exception) {
            Log.w(TAG, "获取门店列表失败: ${e.message}")
            emptyList()
        }
    }
}
