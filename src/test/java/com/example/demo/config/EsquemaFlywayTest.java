package com.example.demo.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.url=jdbc:h2:mem:flyway;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
class EsquemaFlywayTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Migração V1 cria o esquema que o Hibernate valida sem alterar")
    void migracaoLinhaDeBaseValidaContraAsEntidades() {
        Integer aplicadas = jdbc.queryForObject(
                "select count(*) from \"flyway_schema_history\" where \"version\" = '1' and \"success\" = true", Integer.class);
        assertEquals(1, aplicadas);
    }

    @Test
    @DisplayName("Migração V2 deixa whatsapp_id anulável e anula dados pessoais de contatos com opt-out")
    void migracaoV2AplicadaEWhatsappIdAnulavel() {
        Integer aplicadas = jdbc.queryForObject(
                "select count(*) from \"flyway_schema_history\" where \"version\" = '2' and \"success\" = true", Integer.class);
        assertEquals(1, aplicadas);
        String anulavel = jdbc.queryForObject(
                "select is_nullable from information_schema.columns where lower(table_name) = 'contatos' and lower(column_name) = 'whatsapp_id'",
                String.class);
        assertEquals("YES", anulavel);
    }
}
