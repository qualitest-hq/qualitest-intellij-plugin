package com.qualitest.ui;

import com.qualitest.QualiTestBundle;
import com.qualitest.scan.model.ResponseConfig;
import com.qualitest.scan.model.ScannedApi;
import com.qualitest.ui.render.UiStyles;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * 「响应参数」卡片：按 responses[] 下的 Schema 树展示。
 */
final class ResponseCardBuilder {

    private final DetailCardChrome chrome;
    private final SchemaTreePanelBuilder schemaTree;

    ResponseCardBuilder(DetailCardChrome chrome, SchemaTreePanelBuilder schemaTree) {
        this.chrome = chrome;
        this.schemaTree = schemaTree;
    }

    JPanel createResponseCard(ScannedApi api) {
        ResponseConfig response = api.getResponseConfig();
        if (response == null) return new JPanel();

        List<ResponseConfig.ResponseItem> responses = response.getResponses();
        if (responses == null || responses.isEmpty()) return new JPanel();

        JPanel card = chrome.createCard(QualiTestBundle.message("card.response.params"));
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UiStyles.BG_CONTENT);
        content.setBorder(new EmptyBorder(0, 8, 0, 0));

        content.add(chrome.createSectionTitle(QualiTestBundle.message("section.body.response")));

        for (ResponseConfig.ResponseItem item : responses) {
            if (item.getSchema() != null) {
                content.add(schemaTree.createParamTree(item.getSchema()));
            }
        }

        card.add(content);
        return card;
    }
}
