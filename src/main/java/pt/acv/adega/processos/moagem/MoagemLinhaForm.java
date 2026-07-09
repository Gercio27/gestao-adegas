package pt.acv.adega.processos.moagem;

import org.springframework.format.annotation.DateTimeFormat;
import pt.acv.adega.fichas.Adega;
import pt.acv.adega.fichas.Trabalhador;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Dados da moagem de uma linha vindimada, submetidos na folha da Fase 3.
 * responsavel/adega ligam por id (DomainClassConverter). Os enchimentos sao
 * acrescentados (as linhas vazias sao ignoradas ao guardar).
 */
public class MoagemLinhaForm {

    private Trabalhador responsavel;
    private Adega adega;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataInicio;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFim;

    private List<Enchimento> enchimentos = new ArrayList<>();

    public Trabalhador getResponsavel() { return responsavel; }
    public void setResponsavel(Trabalhador responsavel) { this.responsavel = responsavel; }

    public Adega getAdega() { return adega; }
    public void setAdega(Adega adega) { this.adega = adega; }

    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }

    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }

    public List<Enchimento> getEnchimentos() { return enchimentos; }
    public void setEnchimentos(List<Enchimento> enchimentos) { this.enchimentos = enchimentos; }
}
