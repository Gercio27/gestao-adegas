package pt.acv.adega.processos.moagem;

import org.springframework.format.annotation.DateTimeFormat;
import pt.acv.adega.fichas.Adega;
import pt.acv.adega.fichas.Trabalhador;
import pt.acv.adega.planeamento.PlaneamentoVinho;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Dados submetidos na folha da moagem: adega + vinho + vindimas selecionadas +
 * enchimentos. adega/plano/responsavel ligam por id (DomainClassConverter).
 */
public class MoagemForm {

    private Adega adega;
    private PlaneamentoVinho plano;
    private Trabalhador responsavel;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataInicio;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFim;

    /** Ids das linhas (vindimas) selecionadas para moer. */
    private List<Long> vindimaIds = new ArrayList<>();

    private List<Enchimento> enchimentos = new ArrayList<>();

    public Adega getAdega() { return adega; }
    public void setAdega(Adega adega) { this.adega = adega; }

    public PlaneamentoVinho getPlano() { return plano; }
    public void setPlano(PlaneamentoVinho plano) { this.plano = plano; }

    public Trabalhador getResponsavel() { return responsavel; }
    public void setResponsavel(Trabalhador responsavel) { this.responsavel = responsavel; }

    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }

    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }

    public List<Long> getVindimaIds() { return vindimaIds; }
    public void setVindimaIds(List<Long> vindimaIds) { this.vindimaIds = vindimaIds; }

    public List<Enchimento> getEnchimentos() { return enchimentos; }
    public void setEnchimentos(List<Enchimento> enchimentos) { this.enchimentos = enchimentos; }
}
