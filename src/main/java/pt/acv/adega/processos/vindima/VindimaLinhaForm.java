package pt.acv.adega.processos.vindima;

import org.springframework.format.annotation.DateTimeFormat;
import pt.acv.adega.fichas.Adega;
import pt.acv.adega.fichas.Trabalhador;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma colheita a acrescentar a uma linha do planeamento, com os seus proprios
 * dados de operacao. adega/responsavel ligam por id (DomainClassConverter).
 */
public class VindimaLinhaForm {

    private Adega adegaEntrega;
    private Trabalhador responsavel;
    private String vasilame;
    private String meios;
    private String metodos;
    private String transporte;
    private String observacoes;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataInicio;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFim;

    private BigDecimal quantidadeKg;

    public Adega getAdegaEntrega() { return adegaEntrega; }
    public void setAdegaEntrega(Adega adegaEntrega) { this.adegaEntrega = adegaEntrega; }

    public Trabalhador getResponsavel() { return responsavel; }
    public void setResponsavel(Trabalhador responsavel) { this.responsavel = responsavel; }

    public String getVasilame() { return vasilame; }
    public void setVasilame(String vasilame) { this.vasilame = vasilame; }

    public String getMeios() { return meios; }
    public void setMeios(String meios) { this.meios = meios; }

    public String getMetodos() { return metodos; }
    public void setMetodos(String metodos) { this.metodos = metodos; }

    public String getTransporte() { return transporte; }
    public void setTransporte(String transporte) { this.transporte = transporte; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }

    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }

    public BigDecimal getQuantidadeKg() { return quantidadeKg; }
    public void setQuantidadeKg(BigDecimal quantidadeKg) { this.quantidadeKg = quantidadeKg; }
}
