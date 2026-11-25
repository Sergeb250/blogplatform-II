package com.amali.blogPlatform.controller;

import com.amali.blogPlatform.dto.response.SystemInfoResponse;
import com.amali.blogPlatform.service.SystemInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SystemController.class)
@ActiveProfiles("test")
class SystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SystemInfoService systemInfoService;

    @Test
    void infoReturnsStandardApiResponse() throws Exception {
        when(systemInfoService.getInfo()).thenReturn(
                new SystemInfoResponse("Blogging Platform API", "1.0.0", "dev", List.of("dev"), "21"));

        mockMvc.perform(get("/api/system/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Application is running"))
                .andExpect(jsonPath("$.data.environment").value("dev"))
                .andExpect(jsonPath("$.data.activeProfiles[0]").value("dev"));
    }
}
