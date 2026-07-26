package com.qualitest.ui;

import com.qualitest.scan.model.ScannedApi;

import javax.swing.*;

/**
 * 右侧详情区：基本信息、请求参数、响应结构卡片。
 */
public final class ApiSelectionDetailBuilder {

    private final BasicInfoCardBuilder basicInfo;
    private final RequestParamsCardBuilder requestParams;
    private final ResponseCardBuilder responseCard;

    public ApiSelectionDetailBuilder() {
        DetailCardChrome chrome = new DetailCardChrome();
        ParamRowFactory paramRows = new ParamRowFactory();
        SchemaTreePanelBuilder schemaTree = new SchemaTreePanelBuilder(paramRows);
        this.basicInfo = new BasicInfoCardBuilder(chrome);
        this.requestParams = new RequestParamsCardBuilder(chrome, paramRows, schemaTree);
        this.responseCard = new ResponseCardBuilder(chrome, schemaTree);
    }

    public JPanel createInfoCard(ScannedApi api) {
        return basicInfo.createInfoCard(api);
    }

    public JPanel createParamsCard(ScannedApi api) {
        return requestParams.createParamsCard(api);
    }

    public JPanel createResponseCard(ScannedApi api) {
        return responseCard.createResponseCard(api);
    }
}
