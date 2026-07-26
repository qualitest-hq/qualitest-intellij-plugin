package com.qualitest.scan.extractor;

import com.qualitest.scan.model.ScannedApi;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiClass;

/**
 * API信息提取器接口
 * 所有字段提取器都实现此接口
 *
 * @author qualitest
 */
public interface ApiExtractor {

    /**
     * 提取API字段值
     *
     * @param api 构建中的API对象
     * @param controllerClass Controller类
     * @param method API方法
     */
    void extract(ScannedApi api, PsiClass controllerClass, PsiMethod method);

    /**
     * 获取提取器优先级
     * 数值越小优先级越高
     *
     * @return 优先级
     */
    default int getPriority() {
        return 100;
    }
}
