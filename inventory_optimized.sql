/*----++++*/

SELECT     
    tb_gs.c_ccode AS 类别编码,
    tb_gs.c_ccode_name AS 分类名称,
    tb_gdsstore.c_gcode AS 商品编码,
    tb_gs.c_name AS 商品名称,
    tb_gdsprovider.c_provider AS 供应商,
    tb_partner.c_name AS 供应商名称,
    tb_gs.c_model AS 规格,
    tb_gs.c_color_str AS 商品配置,
    tb_gdsprovider.c_d_type AS 物流模式,
    tb_gs.c_pluno AS 条码秤编码,
    tb_gs.c_basic_unit AS 单位,
    tb_gdsstore.c_price AS 零售价,
    tb_gs.c_tcode AS 品牌编码,
    tb_gs.c_name AS 品牌名称,
    tb_gdsstore.c_adno AS 部门,
    tb_depart.c_name AS 部门名称,
    tb_gdsstore.c_status AS 商品状态,
    tb_gs.c_buy_userno AS 采购员,
    tb_gdsstore.c_n_min AS 存量下限,
    tb_gdsstore.c_n_max AS 存量上限,
    tb_gdsstore.c_n_min_order AS 补货点,
    tb_gdsstore.c_dnlmt_number AS 最佳货位量,
    tb_gdsstore.c_distr_content AS 配货包装量,
    tb_gdsstore.c_number AS 存量,
    jj.c_n AS 退货仓,
    kk.c_n AS 外仓,
    tb_gdsstore.c_onway AS 在途量,
    tb_gdsstore.c_sample AS 样品数量,
    tb_gdsstore.c_lastsale_dt AS 最后销售日期,
    tb_gdsstore.c_lastin_dt AS 最后进货日期,
    ISNULL(DATEDIFF(DAY, tb_gdsstore.c_lastsale_dt, GETDATE()), 999) AS 无销售天数,
    tb_gdsstore.c_a AS 售价总额,
    tb_gdsstore.c_at_cost AS 成本价总额,
    tb_gdsstore.c_to_ret AS 请退数量,
    tb_gdsstore.c_sn_perday AS 日均销量,
    CASE 
        WHEN tb_gdsstore.c_sn_perday > 0 THEN ROUND(1.0 * tb_gdsstore.c_number / tb_gdsstore.c_sn_perday, 1)
        ELSE NULL 
    END AS 库存天数,
    tb_gdsstore.c_first_order_dt AS 首次进货日期,
    tb_gdsprovider.c_pt_in AS tb_gds__c_pt_cost,
    tb_gdsstore.c_a_cost AS 不含税成本金额,
    tb_gs.c_barcode AS 主条码,
    tb_gdsstore.c_sale_status AS 销售状态,
    tb_gdsstore.c_pro_status AS 促销状态,
    n1.c_floor AS 楼层,
    n1.c_shelfno AS 货架编号,
    n1.c_layer AS 层号,
    tb_gdsstore.c_introduce_date AS 引入日期,
    tb_gdsstore.c_quarter AS 季节
FROM tb_gdsstore WITH (NOLOCK)
INNER JOIN tb_gdsprovider WITH (NOLOCK) 
    ON tb_gdsstore.c_gcode = tb_gdsprovider.c_gcode 
    AND tb_gdsstore.c_store_id = tb_gdsprovider.c_store_id
    AND tb_gdsprovider.c_status = '主供应商'
LEFT JOIN tb_depart WITH (NOLOCK) 
    ON tb_depart.c_adno = tb_gdsstore.c_adno
LEFT JOIN tb_partner WITH (NOLOCK) 
    ON tb_gdsprovider.c_provider = tb_partner.c_no
LEFT JOIN (
    SELECT a.c_ccode, a.c_gcode, a.c_name, a.c_model, a.c_pluno, a.c_basic_unit, 
           a.c_buy_userno, a.c_barcode, b.c_tcode, b.c_name AS c_pp, 
           a.c_subcode, c.c_name AS c_ccode_name, a.c_color_str
    FROM tb_gds a WITH (NOLOCK)
    LEFT JOIN tb_trademark b WITH (NOLOCK) ON a.c_trademark = b.c_name
    LEFT JOIN tb_gdsclass c WITH (NOLOCK) ON a.c_ccode = c.c_ccode
) tb_gs ON tb_gs.c_gcode = tb_gdsstore.c_gcode 
       AND tb_gs.c_subcode = tb_gdsstore.c_subcode
LEFT JOIN (
    SELECT c.c_gcode, c.c_subcode, 
           MAX(a.c_floor) AS c_floor, 
           MAX(a.c_shelfno) AS c_shelfno, 
           MAX(b.c_layer) AS c_layer
    FROM tb_shelf a WITH (NOLOCK)
    INNER JOIN tb_shelf_layer b WITH (NOLOCK) ON a.c_guid = b.c_shelf_guid
    INNER JOIN tb_gds_shelf c WITH (NOLOCK) ON b.c_guid = c.c_layer_guid
    GROUP BY c.c_gcode, c.c_subcode
) n1 ON n1.c_gcode = tb_gdsstore.c_gcode 
    AND n1.c_subcode = tb_gdsstore.c_subcode
LEFT JOIN dbo.tb_gds_stock jj WITH (NOLOCK) 
    ON jj.c_store_id = tb_gdsstore.c_store_id 
    AND jj.c_gcode = tb_gdsstore.c_gcode 
    AND jj.c_wno = '退货仓'
    AND jj.c_store_id = '10902'
LEFT JOIN dbo.tb_gds_stock kk WITH (NOLOCK) 
    ON kk.c_store_id = tb_gdsstore.c_store_id 
    AND kk.c_gcode = tb_gdsstore.c_gcode 
    AND kk.c_wno = '外仓'
    AND kk.c_store_id = '10902'
WHERE (ISNULL(@部门, '') = '' 
    OR EXISTS (
        SELECT 1 FROM uf_split_string(@部门, ',') f 
        WHERE tb_gdsstore.c_adno LIKE f.c_str + '%' 
           OR f.c_str LIKE tb_gdsstore.c_adno + '%'
    ))
AND (@品类 IS NULL OR @品类 = '' OR tb_gs.c_ccode LIKE @品类 + '%')
AND (@品牌 IS NULL OR @品牌 = '' OR tb_gs.c_pp = @品牌)
AND (ISNULL(@商品编码, '') = '' 
    OR EXISTS (
        SELECT 1 FROM uf_split_string(@商品编码, ',') f 
        WHERE tb_gdsstore.c_gcode LIKE f.c_str + '%'
    ))
AND (@商品名称 IS NULL OR @商品名称 = '' OR tb_gs.c_name LIKE '%' + @商品名称 + '%')
AND (@商品类型 IS NULL OR @商品类型 = '' OR tb_gdsstore.c_type = @商品类型)
AND (@供应商 IS NULL OR @供应商 = '' OR tb_gdsprovider.c_provider = @供应商)
AND tb_gdsstore.c_type LIKE '%单品%'
AND tb_gdsstore.c_ctrlno LIKE '%E%'
AND (@商品状态 IS NULL OR @商品状态 = '' OR tb_gdsstore.c_status = @商品状态)
AND (@促销状态 IS NULL OR @促销状态 = '' OR tb_gdsstore.c_pro_status LIKE '%' + @促销Status + '%')
AND tb_gdsstore.c_status <> '作废'
AND (@库存不等于 IS NULL OR @库存不等于 = '' OR tb_gdsstore.c_number <> @库存不等于)
AND (ISNULL(@机构, '') = '' 
    OR EXISTS (
        SELECT 1 FROM uf_split_string(@机构, ',') f 
        WHERE tb_gdsstore.c_store_id LIKE f.c_str + '%'
           OR f.c_str LIKE tb_gdsstore.c_store_id + '%'
    ))