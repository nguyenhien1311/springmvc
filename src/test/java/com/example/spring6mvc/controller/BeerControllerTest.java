package com.example.spring6mvc.controller;

import com.example.spring6mvc.common.Constant;
import com.example.spring6mvc.config.SpringSecurityConfig;
import com.example.spring6mvc.model.BeerDTO;
import com.example.spring6mvc.services.BeerService;
import com.example.spring6mvc.services.BeerServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BeerController.class)
@Import(SpringSecurityConfig.class)
class BeerControllerTest {
    @Autowired
    MockMvc mock;
    @Autowired
    ObjectMapper mapper;
    @MockBean
    BeerService service;

    BeerServiceImpl impl;

    @BeforeEach
    void setUp() {
        impl = new BeerServiceImpl();
    }

    @Test
    void getBeers() throws Exception {
        given(service.getList(any(), any(), any(), any(), any())).willReturn(impl.getList(null, null, false, 1, 25));
        mock.perform(get(Constant.BEER_ROOT_PATH)
                        .with(jwtProcessor())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()", is(10)));
    }

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtProcessor() {
        return jwt().jwt(jwt -> {
            jwt.claims(claims -> {
                        claims.put("scope", "message-read");
                        claims.put("scope", "message-write");
                    }).issuer("messaging-client")
                    .notBefore(Instant.now().minusSeconds(5l));
        });
    }

    @Test
    void getBeerById() throws Exception {
        BeerDTO beer = impl.getList(null, null, false, 1, 25).getContent().get(0);
        given(service.getById(beer.getId())).willReturn(Optional.of(beer));
        mock.perform(get(Constant.BEER_PATH_DETAIL, beer.getId())
                        .with(jwtProcessor())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(beer.getId().toString())))
                .andExpect(jsonPath("$.beerName", is(beer.getBeerName())));
    }

    @Test
    void addBeer() throws Exception {
        BeerDTO beerDTO = impl.getList(null, null, false, 1, 25).getContent().get(0);
        beerDTO.setId(null);
        beerDTO.setVersion(null);
        beerDTO.setBeerName("This Beer is new");

        given(service.addBeer(any(BeerDTO.class))).willReturn(impl.getList(null, null, false, 1, 25).getContent().get(1));
        MvcResult result = mock.perform(post(Constant.BEER_ROOT_PATH)
                        .with(jwtProcessor())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(beerDTO)))
                .andExpect(status().isCreated()).andReturn();
        System.out.println(result.getResponse().getContentAsString());
    }

    @Test
    void handlePut() throws Exception {
        BeerDTO beer = impl.getList(null, null, false, 1, 25).getContent().get(0);
        given(service.updateBeer(any(), any())).willReturn(Optional.of(beer));
        mock.perform(put(Constant.BEER_PATH_DETAIL, beer.getId())
                        .with(jwtProcessor())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(beer)))
                .andExpect(status().isNoContent());
        verify(service).updateBeer(any(UUID.class), any(BeerDTO.class));
    }

    @Test
    void deleteBeer() throws Exception {
        BeerDTO beer = impl.getList(null, null, false, 1, 25).getContent().get(0);
        given(service.deleteBeer(any())).willReturn(true);
        mock.perform(delete(Constant.BEER_PATH_DETAIL, beer.getId())
                        .with(jwtProcessor()).
                        accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        ArgumentCaptor<UUID> captor = ArgumentCaptor.forClass(UUID.class);
        verify(service).deleteBeer(captor.capture());

        assertThat(beer.getId()).isEqualTo(captor.getValue());
    }
}