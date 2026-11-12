package com.turkcell.libraryapp.memberservice.controller;

import com.turkcell.libraryapp.memberservice.security.IdentityGuard;
import com.turkcell.libraryapp.memberservice.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * H7: canlıda 500 dönen iki istemci hatası artık uçtan uca 400 dönmeli.
 * Kanıt: PATCH /api/members/1/status value'suz -> 500 idi, şimdi 400.
 */
@WebMvcTest(MemberController.class)
@Import(IdentityGuard.class)
class MemberControllerClientErrorTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @Test
    void statusValueYoksa400() throws Exception {
        mockMvc.perform(patch("/api/members/1/status").header("X-Role", "LIBRARIAN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required request parameter 'value' is missing"));
    }

    @Test
    void bozukGovde400() throws Exception {
        mockMvc.perform(post("/api/members")
                        .header("X-Role", "LIBRARIAN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNumber\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"))
                .andExpect(content().string(not(containsString("JsonParseException"))))
                .andExpect(content().string(not(containsString("MemberCreateRequest"))));
    }

    @Test
    void sayisalOlmayanId400() throws Exception {
        mockMvc.perform(get("/api/members/abc").header("X-Role", "LIBRARIAN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }
}
