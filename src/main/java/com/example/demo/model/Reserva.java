package com.example.demo.model;

import com.example.demo.util.CriptografiaCampo;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservas", uniqueConstraints = {
    @UniqueConstraint(name = "uk_reserva_empresa_google_event_id", columnNames = {"empresa_id", "google_event_id"})
})
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contato_id")
    private Contato contato;

    @Column(name = "google_event_id", length = 128)
    private String googleEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem", nullable = false, length = 32)
    private OrigemReserva origem = OrigemReserva.AGENDA;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "horario", length = 64)
    private String horario;

    @Convert(converter = CriptografiaCampo.class)
    @Column(name = "titulo", length = 1000)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 32)
    private SituacaoReserva situacao = SituacaoReserva.SEM_CONTATO;

    @Column(name = "lembrete_enviar_em")
    private LocalDateTime lembreteEnviarEm;

    @Column(name = "lembrete_enviado_em")
    private LocalDateTime lembreteEnviadoEm;

    @Column(name = "tentativas_envio", nullable = false)
    private int tentativasEnvio = 0;

    @Column(name = "atualizada_em")
    private LocalDateTime atualizadaEm;

    public Reserva() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Contato getContato() {
        return contato;
    }

    public void setContato(Contato contato) {
        this.contato = contato;
    }

    public String getGoogleEventId() {
        return googleEventId;
    }

    public void setGoogleEventId(String googleEventId) {
        this.googleEventId = googleEventId;
    }

    public OrigemReserva getOrigem() {
        return origem;
    }

    public void setOrigem(OrigemReserva origem) {
        this.origem = origem;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public SituacaoReserva getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoReserva situacao) {
        this.situacao = situacao;
    }

    public LocalDateTime getLembreteEnviarEm() {
        return lembreteEnviarEm;
    }

    public void setLembreteEnviarEm(LocalDateTime lembreteEnviarEm) {
        this.lembreteEnviarEm = lembreteEnviarEm;
    }

    public LocalDateTime getLembreteEnviadoEm() {
        return lembreteEnviadoEm;
    }

    public void setLembreteEnviadoEm(LocalDateTime lembreteEnviadoEm) {
        this.lembreteEnviadoEm = lembreteEnviadoEm;
    }

    public int getTentativasEnvio() {
        return tentativasEnvio;
    }

    public void setTentativasEnvio(int tentativasEnvio) {
        this.tentativasEnvio = tentativasEnvio;
    }

    public LocalDateTime getAtualizadaEm() {
        return atualizadaEm;
    }

    public void setAtualizadaEm(LocalDateTime atualizadaEm) {
        this.atualizadaEm = atualizadaEm;
    }
}
