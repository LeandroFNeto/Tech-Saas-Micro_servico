package com.example.demo.model;

import com.example.demo.util.CriptografiaCampo;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contatos", uniqueConstraints = {
    @UniqueConstraint(name = "uk_contato_empresa_numero_indice", columnNames = {"empresa_id", "numero_indice"})
})
public class Contato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Convert(converter = CriptografiaCampo.class)
    @Column(name = "whatsapp_id", nullable = false, length = 1000)
    private String whatsappId;

    @Convert(converter = CriptografiaCampo.class)
    @Column(name = "nome", length = 1000)
    private String nome;

    @Column(name = "numero_indice", nullable = false, length = 128)
    private String numeroIndice;

    @Column(name = "final_telefone", length = 8)
    private String finalTelefone;

    @Column(name = "primeira_conversa_em")
    private LocalDateTime primeiraConversaEm;

    @Column(name = "ultima_conversa_em")
    private LocalDateTime ultimaConversaEm;

    @Column(name = "nao_contatar", nullable = false)
    private boolean naoContatar = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem", nullable = false, length = 32)
    private OrigemContato origem = OrigemContato.CONVERSA;

    public Contato() {
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

    public String getWhatsappId() {
        return whatsappId;
    }

    public void setWhatsappId(String whatsappId) {
        this.whatsappId = whatsappId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNumeroIndice() {
        return numeroIndice;
    }

    public void setNumeroIndice(String numeroIndice) {
        this.numeroIndice = numeroIndice;
    }

    public String getFinalTelefone() {
        return finalTelefone;
    }

    public void setFinalTelefone(String finalTelefone) {
        this.finalTelefone = finalTelefone;
    }

    public LocalDateTime getPrimeiraConversaEm() {
        return primeiraConversaEm;
    }

    public void setPrimeiraConversaEm(LocalDateTime primeiraConversaEm) {
        this.primeiraConversaEm = primeiraConversaEm;
    }

    public LocalDateTime getUltimaConversaEm() {
        return ultimaConversaEm;
    }

    public void setUltimaConversaEm(LocalDateTime ultimaConversaEm) {
        this.ultimaConversaEm = ultimaConversaEm;
    }

    public boolean isNaoContatar() {
        return naoContatar;
    }

    public void setNaoContatar(boolean naoContatar) {
        this.naoContatar = naoContatar;
    }

    public OrigemContato getOrigem() {
        return origem;
    }

    public void setOrigem(OrigemContato origem) {
        this.origem = origem;
    }
}
