package com.example.demo;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = PersonController.class)
public class PersonControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @SpyBean
    private PersonService personService;

    @Captor
    private ArgumentCaptor<Mono<Person>> argumentCaptor;

    @MockBean
    private PersonRepository repository;

    @ParameterizedTest
    @CsvSource({"Raul Alzate,0", "Raul Alzate,1"})
    void post(String name, Integer times) {

        if(times == 0) {
            when(repository.findByName(name)).thenReturn(Mono.just(new Person()));
        }

        if(times == 1) {
            when(repository.findByName(name)).thenReturn(Mono.empty());
        }

        var request = Mono.just(new Person(name));
        webTestClient.post()
                .uri("/person")
                .body(request, Person.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(personService).insert(argumentCaptor.capture());
        verify(repository, times(times)).save(any());

        var person = argumentCaptor.getValue().block();

        Assertions.assertEquals(name, person.getName());

    }

    @Test
    void get() {
        var pers = new Person("Kevin Vargas");
        pers.setId("1");
        when(repository.findById("1")).thenReturn(Mono.just(pers));
        webTestClient.get()
                .uri("/person/1")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Person.class)
                .consumeWith(personEntityExchangeResult -> {
                    var person = personEntityExchangeResult.getResponseBody();
                    assert person != null;
                });

        verify(personService).update(argumentCaptor.capture());
        var person2 = argumentCaptor.getValue().block();
        Assertions.assertEquals("Brayan Vargas", person2.getName());
    }

    @Test
    void update() {
        var person = new Person("Kevin Felipe");
        person.setId("1");
        when(repository.findById("1")).thenReturn(Mono.just(person));
        when(repository.save(person)).thenReturn(Mono.just(person));
        var request = Mono.fromCallable(() -> {
            person.setName("Brayan Vargas");
            return person;
        });
        webTestClient.put()
                .uri("/person")
                .body(request, Person.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

    }

    @Test
    void delete() {
        var pers = new Person("Kevin Vargas");
        pers.setId("1");
        when(repository.save(pers)).thenReturn(Mono.just(pers));
        webTestClient.delete()
                .uri("/person/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();
    }

    @Test
    void list() {
        var list = Flux.just(
                new Person("Raul Alzate"),
                new Person("Pedro" )
        );
        when(repository.findAll()).thenReturn(list);

        webTestClient.get()
                .uri("/person")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].name").isEqualTo("Raul Alzate")
                .jsonPath("$[1].name").isEqualTo("Pedro");

        verify(personService).listAll();
        verify(repository).findAll();
    }

}
