package com.qualitest.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.intellij.openapi.diagnostic.Logger;
import com.qualitest.QualiTestBundle;
import com.qualitest.QualiTestStrings;
import com.qualitest.scan.model.ApiConfigV2Defaults;
import com.qualitest.scan.model.ApiImportItem;
import com.qualitest.scan.model.ApiImportParams;
import com.qualitest.scan.model.ScannedApi;
import com.qualitest.scan.ImportResult;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;

/**
 * API客户端
 * 负责与QualiTest服务器通信，上传扫描到的API信息
 *
 * @author qualitest
 */
public class ApiClient {

    private static final Logger LOG = Logger.getInstance(ApiClient.class);

    private static final int TIMEOUT_SECONDS = 30;

    private static final String PROJECT_TOKEN_HEADER = "X-Project-Token";

    private final String baseUrl;
    private final String projectToken;
    private final HttpClient httpClient;
    private final Gson gson;

    public ApiClient(String baseUrl, String projectToken) {
        this.baseUrl = baseUrl != null ? baseUrl.trim() : "";
        this.projectToken = projectToken;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        this.gson = new GsonBuilder()
                // Spring/Jackson 无法反序列化 Gson 默认的本地化 Date 字符串，使用 ISO-8601
                .registerTypeAdapter(Date.class, (JsonSerializer<Date>) (src, typeOfSrc, context) ->
                        new JsonPrimitive(src.toInstant().toString()))
                .setPrettyPrinting()
                .create();
    }

    /**
     * 上传扫描到的 API 列表。项目 ID 由请求头 X-Project-Token 在服务端解析，请求体无需带 testProjectId。
     */
    public ImportResult uploadApis(List<ScannedApi> apis) throws ApiClientException {
        if (apis == null || apis.isEmpty()) {
            throw new ApiClientException(QualiTestBundle.message("scan.done.no.apis"));
        }

        validateUploadConfiguration();

        List<ApiImportItem> items = apis.stream()
                .map(this::convertToItem)
                .toList();

        ApiImportParams params = ApiImportParams.builder()
                .configVersion(ApiImportParams.CONFIG_VERSION)
                .apiList(items)
                .build();

        String json = gson.toJson(params);

        try {
            String response = sendRequest("/api/project/importApis", json);
            ImportResult result = parseResponse(response);
            // 记录插件发送条数，便于与服务端处理/新增/更新数对比
            result.setSentCount(apis.size());
            LOG.info(String.format(
                    "QualiTest API 上传: 发送=%d, 处理=%d, 新增=%d, 更新=%d, 成功=%d, 失败=%d",
                    result.getSentCount(), result.getTotalCount(),
                    result.getInsertCount(), result.getUpdateCount(),
                    result.getSuccessCount(), result.getFailCount()));
            return result;
        } catch (ApiClientException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiClientException(resolveNetworkMessage(e), e);
        } catch (IOException e) {
            throw new ApiClientException(resolveNetworkMessage(e), e);
        } catch (Exception e) {
            throw new ApiClientException(QualiTestBundle.message("error.upload.unknown"), e);
        }
    }

    private void validateUploadConfiguration() throws ApiClientException {
        if (QualiTestStrings.trimToNull(baseUrl) == null) {
            throw new ApiClientException(QualiTestBundle.message("error.upload.no.server"));
        }
        if (QualiTestStrings.trimToNull(projectToken) == null) {
            throw new ApiClientException(QualiTestBundle.message("error.upload.no.token"));
        }
    }

    private static String resolveNetworkMessage(Exception e) {
        String detail = e.getMessage();
        if (detail != null && !detail.isBlank()) {
            return QualiTestBundle.message("error.upload.network.detail", detail);
        }
        return QualiTestBundle.message("error.upload.network");
    }

    /**
     * 将 ScannedApi 转为导入项；配置对象为 null 时使用 {@link ApiConfigV2Defaults} 最小 JSON。
     */
    private ApiImportItem convertToItem(ScannedApi api) {
        ApiImportItem item = new ApiImportItem();
        item.setApiStatus(api.getApiStatus());
        item.setApiGroup(api.getApiGroup());
        item.setApiName(api.getApiName());
        item.setApiDescription(api.getApiDescription());
        item.setApiPath(api.getApiPath());
        item.setProtocolType(api.getProtocolType());

        item.setRequestConfig(toJsonOrDefault(api.getRequestConfig(), ApiConfigV2Defaults.MIN_REQUEST_CONFIG_JSON));
        item.setResponseConfig(toJsonOrDefault(api.getResponseConfig(), ApiConfigV2Defaults.MIN_RESPONSE_CONFIG_JSON));

        item.setSourceSystem(api.getSourceSystem());
        item.setExternalId(api.getExternalId());
        item.setLastSyncTime(new Date());

        return item;
    }

    /**
     * 将对象序列化为 JSON 字符串，如果对象为 null 则返回默认值
     *
     * @param value        待序列化的对象
     * @param defaultValue 默认值（当 value 为 null 或列表为空时使用）
     * @return JSON 字符串
     */
    private String toJsonOrDefault(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof List<?> list && list.isEmpty()) {
            return defaultValue;
        }
        return gson.toJson(value);
    }

    /**
     * 发送HTTP请求
     */
    private String sendRequest(String endpoint, String json) throws IOException, InterruptedException, ApiClientException {
        String url = buildUrl(endpoint);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));

        if (projectToken != null && !projectToken.isEmpty()) {
            builder.header(PROJECT_TOKEN_HEADER, projectToken.trim());
        }

        HttpRequest request = builder.build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() >= 400) {
            String body = response.body();
            String detail = body != null && !body.isBlank()
                    ? QualiTestBundle.message("error.upload.http", response.statusCode()) + " - " + body
                    : QualiTestBundle.message("error.upload.http", response.statusCode());
            throw new ApiClientException(detail);
        }

        return response.body();
    }

    /**
     * 构建完整URL
     */
    private String buildUrl(String endpoint) {
        String base = baseUrl;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!endpoint.startsWith("/")) {
            endpoint = "/" + endpoint;
        }
        return base + endpoint;
    }

    /**
     * 解析导入接口返回的 JSON。
     * 填充成功标记、统计计数、逐条明细，以及可选的 syncImpact 影响计数。
     */
    private ImportResult parseResponse(String response) throws ApiClientException {
        try {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();

            ImportResult result = new ImportResult();

            if (json.has("code")) {
                int code = json.get("code").getAsInt();
                result.setSuccess(code == 200 || code == 0);
            } else if (json.has("success")) {
                result.setSuccess(json.get("success").getAsBoolean());
            }

            setMessageIfPresent(result, json);

            if (json.has("data")) {
                JsonObject data = json.getAsJsonObject("data");
                if (data != null) {
                    setIntIfPresent(data, "totalCount", result::setTotalCount);
                    setIntIfPresent(data, "successCount", result::setSuccessCount);
                    setIntIfPresent(data, "insertCount", result::setInsertCount);
                    setIntIfPresent(data, "updateCount", result::setUpdateCount);
                    setIntIfPresent(data, "failCount", result::setFailCount);
                    parseDetailsIfPresent(data, result);
                    parseSyncImpactIfPresent(data, result);
                    setMessageIfPresent(result, data);
                }
            }

            if (result.getMessage() == null) {
                result.setMessage(result.isSuccess() ? "上传成功" : "上传失败");
            }

            return result;
        } catch (Exception e) {
            throw new ApiClientException(QualiTestBundle.message("error.upload.parse"), e);
        }
    }

    private void setMessageIfPresent(ImportResult result, JsonObject source) {
        if (source.has("message")) {
            result.setMessage(source.get("message").getAsString());
        } else if (source.has("msg")) {
            // 根包 R 类型统一使用 msg 字段
            result.setMessage(source.get("msg").getAsString());
        }
    }

    private void setIntIfPresent(JsonObject source, String fieldName, java.util.function.IntConsumer setter) {
        if (source.has(fieldName)) {
            setter.accept(source.get(fieldName).getAsInt());
        }
    }

    /**
     * 从 data.details 解析每条 API 的处理状态。
     * 失败项可在通知里展示路径与原因示例。
     */
    private void parseDetailsIfPresent(JsonObject data, ImportResult result) {
        if (!data.has("details") || !data.get("details").isJsonArray()) {
            return;
        }
        JsonArray details = data.getAsJsonArray("details");
        java.util.List<ImportResult.ImportDetail> list = new java.util.ArrayList<>();
        for (JsonElement element : details) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            ImportResult.ImportDetail detail = ImportResult.ImportDetail.builder()
                    .apiPath(getStringOrNull(item, "apiPath"))
                    .apiName(getStringOrNull(item, "apiName"))
                    .status(getStringOrNull(item, "status"))
                    .message(getStringOrNull(item, "message"))
                    .build();
            if (item.has("testProjectApiId") && !item.get("testProjectApiId").isJsonNull()) {
                detail.setTestProjectApiId(item.get("testProjectApiId").getAsLong());
            }
            list.add(detail);
        }
        result.setDetails(list);
    }

    /**
     * 从 data.syncImpact 解析影响计数。
     * 只读取变更 API 数、受影响流数、告警数；不解析流明细列表。
     */
    private void parseSyncImpactIfPresent(JsonObject data, ImportResult result) {
        if (!data.has("syncImpact") || data.get("syncImpact").isJsonNull()
                || !data.get("syncImpact").isJsonObject()) {
            return;
        }
        JsonObject impactJson = data.getAsJsonObject("syncImpact");
        ImportResult.SyncImpact impact = ImportResult.SyncImpact.builder().build();
        setIntIfPresent(impactJson, "changedApiCount", impact::setChangedApiCount);
        setIntIfPresent(impactJson, "affectedFlowCount", impact::setAffectedFlowCount);
        setIntIfPresent(impactJson, "warningCount", impact::setWarningCount);
        result.setSyncImpact(impact);
    }

    private static String getStringOrNull(JsonObject obj, String field) {
        return obj.has(field) && !obj.get(field).isJsonNull() ? obj.get(field).getAsString() : null;
    }

    /**
     * API客户端异常
     */
    public static class ApiClientException extends Exception {
        public ApiClientException(String message) {
            super(message);
        }

        public ApiClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
