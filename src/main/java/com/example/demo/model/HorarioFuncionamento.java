package com.example.demo.model;

import jakarta.persistence.*;

import java.time.LocalTime;

@Entity
@Table(name = "horario_funcionamento",
        uniqueConstraints = @UniqueConstraint(columnNames = {"empresa_id", "perfil"}))
public class HorarioFuncionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PerfilFuncionamento perfil;

    private LocalTime abre;

    private LocalTime fecha;

    @Column(nullable = false)
    private boolean fechado;

    @Column(name = "duracao_minutos")
    private Integer duracaoMinutos;

    public Long getId() {
        return id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public PerfilFuncionamento getPerfil() {
        return perfil;
    }

    public void setPerfil(PerfilFuncionamento perfil) {
        this.perfil = perfil;
    }

    public LocalTime getAbre() {
        return abre;
    }

    public void setAbre(LocalTime abre) {
        this.abre = abre;
    }

    public LocalTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalTime fecha) {
        this.fecha = fecha;
    }

    public boolean isFechado() {
        return fechado;
    }

    public void setFechado(boolean fechado) {
        this.fechado = fechado;
    }

    public Integer getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public void setDuracaoMinutos(Integer duracaoMinutos) {
        this.duracaoMinutos = duracaoMinutos;
    }
}
