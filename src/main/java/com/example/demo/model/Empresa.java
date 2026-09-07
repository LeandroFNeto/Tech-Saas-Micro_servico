package com.example.demo.model;


import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="empresas")

public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private Boolean usaIA = false;

    @Column(unique = true)
    private String sessaoWhatsapp;

    @Column(columnDefinition = "TEXT")
    private String mensagemSaudacao;

    private String ramoDeAtuacao;

    @Column(columnDefinition = "TEXT")
    private String tabelaDePrecos;

    // A Empresa tem uma lista de dias de reserva
    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL)
    private List<DiaReserva> diasDeReserva;

    @Column(columnDefinition = "TEXT")
    private String linkGoogleMaps;

    @Column(columnDefinition = "TEXT")
    private String linkFotoPrincipal;

    @Column(columnDefinition = "TEXT")
    private String linkGaleria;

    @Column(name = "google_calendar_id")
    private String googleCalendarId;



    @Column(name = "locacao_por_hora")
    private Boolean locacaoPorHora = false;

    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordemExibicao ASC")
    private List<ModuloEmpresa> modulosAtivos = new ArrayList<>();

    public Boolean getUsaIA() {
        return usaIA;
    }

    public void setUsaIA(Boolean usaIA) {
        this.usaIA = usaIA;
    }

    public Boolean getLocacaoPorHora() {
        return locacaoPorHora;
    }

    public void setLocacaoPorHora(Boolean locacaoPorHora) {
        this.locacaoPorHora = locacaoPorHora;
    }

    public List<ModuloEmpresa> getModulosAtivos() {
        return modulosAtivos;
    }

    public void setModulosAtivos(List<ModuloEmpresa> modulosAtivos) {
        this.modulosAtivos = modulosAtivos;
    }

    // Não se esqueça de adicionar os Getters e Setters no final do ficheiro!
    public String getGoogleCalendarId() {
        return googleCalendarId;
    }

    public void setGoogleCalendarId(String googleCalendarId) {
        this.googleCalendarId = googleCalendarId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMensagemSaudacao() {
        return mensagemSaudacao;
    }

    public void setMensagemSaudacao(String mensagemSaudacao) {
        this.mensagemSaudacao = mensagemSaudacao;
    }

    public String getSessaoWhatsapp() {
        return sessaoWhatsapp;
    }

    public void setSessaoWhatsapp(String sessaoWhatsapp) {
        this.sessaoWhatsapp = sessaoWhatsapp;
    }

    public String getRamoDeAtuacao() {
        return ramoDeAtuacao;
    }

    public void setRamoDeAtuacao(String ramoDeAtuacao) {
        this.ramoDeAtuacao = ramoDeAtuacao;
    }

    public List<DiaReserva> getDiasDeReserva() {
        return diasDeReserva;
    }

    public String getTabelaDePrecos() {
        return tabelaDePrecos;
    }

    public void setTabelaDePrecos(String tabelaDePrecos) {
        this.tabelaDePrecos = tabelaDePrecos;
    }

    public void setDiasDeReserva(List<DiaReserva> diasDeReserva) {
        this.diasDeReserva = diasDeReserva;
    }

    public String getLinkGoogleMaps() {
        return linkGoogleMaps;
    }

    public void setLinkGoogleMaps(String linkGoogleMaps) {
        this.linkGoogleMaps = linkGoogleMaps;
    }

    public String getLinkFotoPrincipal() {
        return linkFotoPrincipal;
    }

    public void setLinkFotoPrincipal(String linkFotoPrincipal) {
        this.linkFotoPrincipal = linkFotoPrincipal;
    }

    public String getLinkGaleria() {
        return linkGaleria;
    }

    public void setLinkGaleria(String linkGaleria) {
        this.linkGaleria = linkGaleria;
    }
}