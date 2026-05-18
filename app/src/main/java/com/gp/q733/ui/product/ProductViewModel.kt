package com.gp.q733.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gp.q733.domain.model.ProductInfo
import com.gp.q733.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 商品管理ViewModel
 * 负责商品列表展示、搜索、增删改、CSV导入
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    // 搜索关键词
    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()

    // 商品列表（根据搜索关键词动态切换全量/搜索结果）
    val products: StateFlow<List<ProductInfo>> = _searchKeyword.flatMapLatest { keyword ->
        if (keyword.isBlank()) productRepository.getAllProducts()
        else productRepository.searchProducts(keyword)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 编辑对话框状态
    private val _editProduct = MutableStateFlow<ProductInfo?>(null)
    val editProduct: StateFlow<ProductInfo?> = _editProduct.asStateFlow()

    // 是否显示编辑对话框
    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()

    // 导入结果消息
    private val _importMessage = MutableStateFlow<String?>(null)
    val importMessage: StateFlow<String?> = _importMessage.asStateFlow()

    fun updateSearchKeyword(keyword: String) {
        _searchKeyword.update { keyword }
    }

    fun showAddDialog() {
        _editProduct.update { null }
        _showEditDialog.update { true }
    }

    fun showEditDialog(product: ProductInfo) {
        _editProduct.update { product }
        _showEditDialog.update { true }
    }

    fun dismissEditDialog() {
        _showEditDialog.update { false }
        _editProduct.update { null }
    }

    fun saveProduct(product: ProductInfo) {
        viewModelScope.launch {
            val existing = productRepository.getProductByBarcode(product.barcode)
            if (existing != null) {
                productRepository.updateProduct(product)
            } else {
                productRepository.addProduct(product)
            }
            dismissEditDialog()
        }
    }

    fun deleteProduct(product: ProductInfo) {
        viewModelScope.launch {
            productRepository.deleteProduct(product)
        }
    }

    fun importFromCsv(csvData: List<ProductInfo>) {
        viewModelScope.launch {
            val count = productRepository.importProducts(csvData)
            _importMessage.update { "成功导入 $count 条商品" }
        }
    }

    fun setImportMessage(message: String) {
        _importMessage.update { message }
    }

    fun clearImportMessage() {
        _importMessage.update { null }
    }
}
