package com.qualitest.ui;

import com.qualitest.QualiTestBundle;
import com.qualitest.QualiTestStrings;
import com.qualitest.scan.model.RequestBody;
import com.qualitest.scan.model.ApiParameter;
import com.qualitest.scan.model.RequestConfig;
import com.qualitest.scan.model.ScannedApi;
import com.qualitest.ui.render.UiStyles;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * 「请求参数」卡片：Header、Path、Query、Body。
 */
final class RequestParamsCardBuilder {

    private final DetailCardChrome chrome;
    private final ParamRowFactory paramRows;
    private final SchemaTreePanelBuilder schemaTree;

    RequestParamsCardBuilder(DetailCardChrome chrome, ParamRowFactory paramRows, SchemaTreePanelBuilder schemaTree) {
        this.chrome = chrome;
        this.paramRows = paramRows;
        this.schemaTree = schemaTree;
    }

    JPanel createParamsCard(ScannedApi api) {
        RequestConfig requestConfig = api.getRequestConfig();
        List<ApiParameter> pathParams = requestConfig != null ? requestConfig.getPathParams() : null;
        List<ApiParameter> queryParams = requestConfig != null ? requestConfig.getQueryParams() : null;
        List<ApiParameter> headers = requestConfig != null ? requestConfig.getDeclaredHeaders() : null;
        RequestBody body = requestConfig != null ? requestConfig.getBody() : null;

        boolean hasParams = (headers != null && !headers.isEmpty()) ||
                            (pathParams != null && !pathParams.isEmpty()) ||
                            (queryParams != null && !queryParams.isEmpty()) ||
                            hasBodyContent(body);
        if (!hasParams) {
            return new JPanel();
        }

        JPanel card = chrome.createCard(QualiTestBundle.message("card.request.params"));
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UiStyles.BG_CONTENT);
        content.setBorder(new EmptyBorder(0, 8, 0, 0));

        if (headers != null && !headers.isEmpty()) {
            content.add(chrome.createSectionTitle(QualiTestBundle.message("section.header.params")));
            paramRows.addParamRows(content, headers);
            content.add(Box.createVerticalStrut(4));
        }

        if (pathParams != null && !pathParams.isEmpty()) {
            content.add(chrome.createSectionTitle(QualiTestBundle.message("section.path.params")));
            paramRows.addParamRows(content, pathParams);
            content.add(Box.createVerticalStrut(4));
        }

        if (queryParams != null && !queryParams.isEmpty()) {
            content.add(chrome.createSectionTitle(QualiTestBundle.message("section.query.params")));
            paramRows.addParamRows(content, queryParams);
            content.add(Box.createVerticalStrut(4));
        }

        if (body != null && body.getJson() != null && body.getJson().getSchema() != null) {
            content.add(chrome.createSectionTitle(
                    QualiTestBundle.message("section.body.params", getContentType(body.getMode()))
            ));
            content.add(schemaTree.createParamTree(body.getJson().getSchema()));
        } else if (body != null && body.getFormData() != null && !body.getFormData().isEmpty()) {
            content.add(chrome.createSectionTitle(
                    QualiTestBundle.message("section.body.params", getContentType(body.getMode()))
            ));
            paramRows.addFormDataRows(content, body.getFormData());
        } else if (body != null && body.getUrlencoded() != null && !body.getUrlencoded().isEmpty()) {
            content.add(chrome.createSectionTitle(
                    QualiTestBundle.message("section.body.params", getContentType(body.getMode()))
            ));
            paramRows.addFormDataRows(content, body.getUrlencoded());
        } else if (body != null && "binary".equals(body.getMode())) {
            content.add(chrome.createSectionTitle(
                    QualiTestBundle.message("section.body.params", getContentType(body.getMode()))
            ));
            content.add(paramRows.createParamRow("file", "binary", true, null));
        } else if (body != null && "text".equals(body.getMode())) {
            content.add(chrome.createSectionTitle(
                    QualiTestBundle.message("section.body.params", getContentType(body.getMode()))
            ));
            content.add(paramRows.createParamRow("content", "string", false, null));
        }

        card.add(content);
        return card;
    }

    private static String getContentType(String mode) {
        if (mode == null) return "application/json";
        return switch (mode) {
            case "json" -> "application/json";
            case "form-data" -> "multipart/form-data";
            case "x-www-form-urlencoded" -> "application/x-www-form-urlencoded";
            case "binary" -> "application/octet-stream";
            case "text" -> "text/plain";
            default -> mode;
        };
    }

    private static boolean hasBodyContent(RequestBody body) {
        if (body == null || body.getMode() == null || "none".equals(body.getMode())) {
            return false;
        }
        if (body.getJson() != null && body.getJson().getSchema() != null) {
            return true;
        }
        return (body.getFormData() != null && !body.getFormData().isEmpty()) ||
               (body.getUrlencoded() != null && !body.getUrlencoded().isEmpty()) ||
               QualiTestStrings.trimToNull(body.getText()) != null ||
               body.getBinary() != null;
    }
}
