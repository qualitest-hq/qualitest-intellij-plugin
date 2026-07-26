package com.qualitest.scan;

import com.qualitest.scan.model.ScannedApi;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ApiUploadFiltersTest {

    @Test
    public void summarize_countsTotalAndExplicit() {
        List<ScannedApi> apis = List.of(
                api("com.example.C1", true),
                api("com.example.C1", true),
                api("com.example.C2", false),
                api("com.example.C3", true)
        );

        UploadScanStats stats = ApiUploadFilters.summarize(apis);

        assertEquals(4, stats.getTotalApiCount());
        assertEquals(3, stats.getTotalControllerCount());
        assertEquals(3, stats.getExplicitApiCount());
        assertEquals(2, stats.getExplicitControllerCount());
    }

    @Test
    public void filterByExplicitGroup_returnsAllWhenDisabled() {
        List<ScannedApi> apis = List.of(api("c1", true), api("c2", false));
        List<ScannedApi> filtered = ApiUploadFilters.filterByExplicitGroup(apis, false);
        assertSame(apis, filtered);
    }

    @Test
    public void filterByExplicitGroup_keepsOnlyExplicitControllers() {
        ScannedApi explicitApi = api("c1", true);
        ScannedApi fallbackApi = api("c2", false);
        List<ScannedApi> apis = List.of(explicitApi, fallbackApi);

        List<ScannedApi> filtered = ApiUploadFilters.filterByExplicitGroup(apis, true);

        assertEquals(1, filtered.size());
        assertEquals(explicitApi, filtered.get(0));
    }

    @Test
    public void summarize_fallbackControllerCountForSingleControllerWithoutName() {
        ScannedApi api = new ScannedApi();
        api.setControllerHasExplicitGroup(true);

        UploadScanStats stats = ApiUploadFilters.summarize(List.of(api));

        assertEquals(1, stats.getTotalApiCount());
        assertEquals(1, stats.getTotalControllerCount());
        assertEquals(1, stats.getExplicitApiCount());
        assertEquals(0, stats.getExplicitControllerCount());
    }

    private static ScannedApi api(String controllerName, boolean explicitGroup) {
        ScannedApi api = new ScannedApi();
        api.setControllerQualifiedName(controllerName);
        api.setControllerHasExplicitGroup(explicitGroup);
        return api;
    }
}
